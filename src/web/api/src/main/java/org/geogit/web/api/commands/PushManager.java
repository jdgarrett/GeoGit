/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.commands;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.geogit.api.GeoGIT;
import org.geogit.api.GeogitTransaction;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.TransactionBegin;
import org.geogit.api.plumbing.UpdateRef;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

/**
 * Provides a safety net for remote pushes. This class keeps track of the IP addresses of remotes
 * that have pushed contents to this repository. If every object is successfully transfered, a
 * message will be sent to the PushManager to update the local references as indicated by the
 * remote.
 */
public class PushManager {

    private Set<String> incomingIPs;

    private static PushManager instance = new PushManager();

    private PushManager() {
        incomingIPs = Collections.synchronizedSet(new HashSet<String>());
    }

    /**
     * @return the singleton instance of the {@code PushManager}
     */
    public static PushManager get() {
        return instance;
    }

    /**
     * Begins tracking incoming objects from the specified ip address.
     * 
     * @param ipAddress the remote machine that is pushing objects
     */
    public void connectionBegin(String ipAddress) {
        if (incomingIPs.contains(ipAddress)) {
            incomingIPs.remove(ipAddress);
        }
        if (incomingIPs.size() > 0) {
            // Fail?
        }
        incomingIPs.add(ipAddress);
    }

    /**
     * This is called when the machine at the specified ip address is finished pushing objects to
     * the server. This causes the ref given by {@code refSpec} to be updated to point to the given
     * {@code newCommit} object id, as well as the {@link Ref#WORK_HEAD WORK_HEAD} and
     * {@link Ref#STAGE_HEAD STAGE_HEAD} refs if {@code refSpec} is the current branch.
     * 
     * @param geogit the geogit of the local repository
     * @param ipAddress the remote machine that is pushing objects
     */
    public void connectionSucceeded(final GeoGIT geogit, final String ipAddress,
            final String refspec, final ObjectId newCommit) {

        if (!incomingIPs.remove(ipAddress)) {// remove and check for existence in one shot
            throw new RuntimeException("Tried to end a connection that didn't exist.");
        }

        // Do not use the geogit instance after this, but the tx one!
        GeogitTransaction tx = geogit.command(TransactionBegin.class).call();
        try {
            Optional<Ref> oldRef = tx.command(RefParse.class).setName(refspec).call();
            Optional<Ref> headRef = tx.command(RefParse.class).setName(Ref.HEAD).call();
            String refName = refspec;
            if (oldRef.isPresent()) {
                if (oldRef.get().getObjectId().equals(newCommit)) {
                    return;
                }
                refName = oldRef.get().getName();
            }
            if (headRef.isPresent() && headRef.get() instanceof SymRef) {
                if (((SymRef) headRef.get()).getTarget().equals(refName)) {
                    Optional<ObjectId> commitTreeId = tx.command(ResolveTreeish.class)
                            .setTreeish(newCommit).call();
                    checkState(commitTreeId.isPresent(), "Commit %s not found", newCommit);

                    tx.command(UpdateRef.class).setName(Ref.WORK_HEAD)
                            .setNewValue(commitTreeId.get()).call();
                    tx.command(UpdateRef.class).setName(Ref.STAGE_HEAD)
                            .setNewValue(commitTreeId.get()).call();
                }
            }
            tx.command(UpdateRef.class).setName(refName).setNewValue(newCommit).call();

            tx.commit();
        } catch (Exception e) {
            tx.abort();
            throw Throwables.propagate(e);
        }
    }
}
