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

package org.apache.roller.weblogger.business;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject; 

import org.apache.roller.planet.business.PlanetManager;
import org.apache.roller.planet.business.fetcher.FeedFetcher;
import org.apache.roller.weblogger.WebloggerException;
/**
 * Facade grouping Planet (aggregation / planet) related components:
 *   - PlanetManager
 *   - FeedFetcher
 *   - PlanetURLStrategy
 */

@com.google.inject.Singleton
public class PlanetFacade {

    private static final Log log = LogFactory.getLog(PlanetFacade.class);

    private final FeedFetcher feedFetcher;
    private final PlanetManager planetManager;
    private final org.apache.roller.planet.business.PlanetURLStrategy planetUrlStrategy;

    @Inject
    public PlanetFacade(
            FeedFetcher feedFetcher,
            PlanetManager planetManager,
            org.apache.roller.planet.business.PlanetURLStrategy planetUrlStrategy) {

        this.feedFetcher       = feedFetcher;
        this.planetManager     = planetManager;
        this.planetUrlStrategy = planetUrlStrategy;
    }

    // Getters

    public FeedFetcher getFeedFetcher() { return feedFetcher; }
    public PlanetManager getPlanetManager() { return planetManager; }
    public org.apache.roller.planet.business.PlanetURLStrategy getPlanetUrlStrategy() {
        return planetUrlStrategy;
    }

    // Lifecycle methods

    public void initialize() throws InitializationException {
        // No explicit initialization was present in original code
    }

    public void release() {
        // No release() calls were present for these components in original code
    }

    public void shutdown() {
        // No shutdown logic was present in original code
    }
}