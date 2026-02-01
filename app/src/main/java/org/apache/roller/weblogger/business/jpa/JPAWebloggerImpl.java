/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.roller.weblogger.business.jpa;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.*;

import org.apache.roller.weblogger.business.jpa.JPAPersistenceStrategy;

/**
 * A JPA specific implementation of the Weblogger business layer.
 */
@Singleton
public class JPAWebloggerImpl extends WebloggerImpl {

    // This field is still needed for JPA-specific flush/release/shutdown
    private final JPAPersistenceStrategy strategy;

    /**
     * Guice-injected constructor
     */
    @Inject
    public JPAWebloggerImpl(
            PingManagerFacade       pingFacade,
            ContentManagerFacade    contentFacade,
            SystemManagerFacade     systemFacade,
            UserManagerFacade       userFacade,
            PlanetFacade            planetFacade,
            URLStrategy             urlStrategy,
            JPAPersistenceStrategy  strategy) throws WebloggerException {

        super(pingFacade, contentFacade, systemFacade, userFacade, planetFacade, urlStrategy);

        // Very important: initialize the final field
        this.strategy = strategy;
    }

    @Override
    public void flush() throws WebloggerException {
        this.strategy.flush();
    }

    @Override
    public void release() {
        super.release();
        // tell JPA to close down
        this.strategy.release();
    }

    @Override
    public void shutdown() {
        // do our own shutdown first
        this.release();

        // then let parent do its thing
        super.shutdown();

        this.strategy.shutdown();
    }
}