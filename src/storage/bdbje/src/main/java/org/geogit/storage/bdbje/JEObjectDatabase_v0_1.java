/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.storage.bdbje;

import org.geogit.repository.Hints;
import org.geogit.repository.RepositoryConnectionException;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.datastream.DataStreamSerializationFactoryV1;

import com.google.inject.Inject;

public final class JEObjectDatabase_v0_1 extends JEObjectDatabase {
    @Inject
    public JEObjectDatabase_v0_1(final ConfigDatabase configDB,
            final EnvironmentBuilder envProvider, final Hints hints) {
        this(configDB, envProvider, hints.getBoolean(Hints.OBJECTS_READ_ONLY),
                JEObjectDatabase.ENVIRONMENT_NAME);
    }

    public JEObjectDatabase_v0_1(final ConfigDatabase configDB,
            final EnvironmentBuilder envProvider, final boolean readOnly, final String envName) {
        super(DataStreamSerializationFactoryV1.INSTANCE, configDB, envProvider, readOnly, envName);
    }

    @Override
    public void configure() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.configure(configDB, "bdbje", "0.1");
    }

    @Override
    public void checkConfig() throws RepositoryConnectionException {
        RepositoryConnectionException.StorageType.OBJECT.verify(configDB, "bdbje", "0.1");
    }
}
