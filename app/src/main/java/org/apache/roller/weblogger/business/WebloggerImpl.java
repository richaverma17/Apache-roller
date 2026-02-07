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
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.roller.weblogger.business;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.pings.AutoPingManager;
import org.apache.roller.weblogger.business.pings.PingQueueManager;
import org.apache.roller.weblogger.business.pings.PingTargetManager;

import org.apache.roller.planet.business.PlanetManager;
import org.apache.roller.planet.business.fetcher.FeedFetcher;

import org.apache.roller.weblogger.business.plugins.PluginManager;
import org.apache.roller.weblogger.business.runnable.ThreadManager;
import org.apache.roller.weblogger.business.search.IndexManager;
import org.apache.roller.weblogger.business.themes.ThemeManager;

/**
 * The abstract version of the Weblogger implementation.
 * Refactored to use facade pattern for grouping related managers,
 * reducing constructor parameters and improving modularity.
 */
@com.google.inject.Singleton
public abstract class WebloggerImpl implements Weblogger {

    private static final Log log = LogFactory.getLog(WebloggerImpl.class);

    // Facades grouping related managers
    private final PingManagerFacade   pingFacade;
    private final ContentManagerFacade contentFacade;
    private final SystemManagerFacade  systemFacade;
    private final UserManagerFacade    userFacade;
    private final PlanetFacade         planetFacade;

    // Common URL strategy
    private final URLStrategy urlStrategy;

    // Build/version information
    private final BuildInfo buildInfo;
    
    // Initializer for security and configuration
    private final WebloggerInitializer initializer;

    protected WebloggerImpl(
            PingManagerFacade   pingFacade,
            ContentManagerFacade contentFacade,
            SystemManagerFacade  systemFacade,
            UserManagerFacade    userFacade,
            PlanetFacade         planetFacade,
            URLStrategy          urlStrategy) {

        this.pingFacade     = pingFacade;
        this.contentFacade  = contentFacade;
        this.systemFacade   = systemFacade;
        this.userFacade     = userFacade;
        this.planetFacade   = planetFacade;
        this.urlStrategy    = urlStrategy;
        this.buildInfo      = new BuildInfo();
        this.initializer    = new WebloggerInitializer();
    }

    // ────────────────────────────────────────────────
    // Getter delegations to facades
    // ────────────────────────────────────────────────

    @Override 
    public ThreadManager getThreadManager() { 
        return systemFacade.getThreadManager(); 
    }

    @Override 
    public IndexManager getIndexManager() { 
        return systemFacade.getIndexManager(); 
    }

    @Override
    public ThemeManager getThemeManager() { 
        return systemFacade.getThemeManager(); 
    }

    @Override
    public PropertiesManager getPropertiesManager() { 
        return systemFacade.getPropertiesManager(); 
    }

    @Override
    public PluginManager getPluginManager() { 
        return systemFacade.getPluginManager(); 
    }

    @Override
    public UserManager getUserManager() { 
        return userFacade.getUserManager(); 
    }

    @Override
    public OAuthManager getOAuthManager() { 
        return userFacade.getOAuthManager();
    }

    @Override
    public BookmarkManager getBookmarkManager() { 
        return contentFacade.getBookmarkManager(); 
    }

    @Override
    public MediaFileManager getMediaFileManager() { 
        return contentFacade.getMediaFileManager(); 
    }

    @Override
    public FileContentManager getFileContentManager() { 
        return contentFacade.getFileContentManager(); 
    }   

    @Override
    public WeblogManager getWeblogManager() { 
        return contentFacade.getWeblogManager(); 
    }

    @Override
    public WeblogEntryManager getWeblogEntryManager() { 
        return contentFacade.getWeblogEntryManager(); 
    }

    @Override
    public AutoPingManager getAutopingManager() {   
        return pingFacade.getAutoPingManager(); 
    }

    @Override
    public PingQueueManager getPingQueueManager() { 
        return pingFacade.getPingQueueManager(); 
    }
    
    @Override
    public PingTargetManager getPingTargetManager() { 
        return pingFacade.getPingTargetManager(); 
    }

    @Override
    public FeedFetcher getFeedFetcher() { 
        return planetFacade.getFeedFetcher(); 
    }
    
    @Override
    public PlanetManager getPlanetManager() { 
        return planetFacade.getPlanetManager(); 
    }
    
    @Override
    public org.apache.roller.planet.business.PlanetURLStrategy getPlanetURLStrategy() {
        return planetFacade.getPlanetUrlStrategy();
    }

    @Override
    public URLStrategy getUrlStrategy() { 
        return urlStrategy; 
    }

    // ────────────────────────────────────────────────
    // Lifecycle methods – delegate to facades
    // ────────────────────────────────────────────────

    @Override
    public void release() {
        try {
            pingFacade.release();
            contentFacade.release();
            systemFacade.release();
            userFacade.release();
            planetFacade.release();
        } catch (Exception e) {
            log.error("Error calling Roller.release()", e);
        }
    }

    @Override
    public void initialize() throws InitializationException {
        log.info("Initializing Roller Weblogger business tier");

        // Order matters – system components first (properties, themes, threads, index)
        systemFacade.initialize();

        // Then others
        pingFacade.initialize();
        contentFacade.initialize();
        userFacade.initialize();
        planetFacade.initialize();

        // Harden XML parser against XXE
        initializer.configureXmlSecurity();

        // Ping configuration
        initializer.configurePings(getAutopingManager());

        try {
            flush();
        } catch (WebloggerException ex) {
            throw new InitializationException("Error flushing after initialization", ex);
        }

        log.info("Roller Weblogger business tier successfully initialized");
    }

    @Override
    public void shutdown() {
        try {
            HitCountQueue.getInstance().shutdown();
            systemFacade.shutdown();
            pingFacade.shutdown();
            contentFacade.shutdown();
            userFacade.shutdown();
            planetFacade.shutdown();
        } catch (Exception e) {
            log.error("Error calling Roller.shutdown()", e);
        }
    }

    // ────────────────────────────────────────────────
    // Version / build info
    // ────────────────────────────────────────────────

    @Override 
    public String getVersion() { 
        return buildInfo.getVersion(); 
    }
    
    @Override 
    public String getRevision() { 
        return buildInfo.getRevision(); 
    }
    
    @Override 
    public String getBuildTime() { 
        return buildInfo.getBuildTime(); 
    }
    
    @Override 
    public String getBuildUser() { 
        return buildInfo.getBuildUser(); 
    }
}