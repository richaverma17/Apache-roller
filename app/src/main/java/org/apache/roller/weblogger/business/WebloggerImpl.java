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
import org.apache.roller.planet.business.PlanetManager;
import org.apache.roller.planet.business.fetcher.FeedFetcher;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.pings.AutoPingManager;
import org.apache.roller.weblogger.business.pings.PingQueueManager;
import org.apache.roller.weblogger.business.pings.PingTargetManager;
import org.apache.roller.weblogger.business.plugins.PluginManager;
import org.apache.roller.weblogger.business.runnable.ThreadManager;
import org.apache.roller.weblogger.business.search.IndexManager;
import org.apache.roller.weblogger.business.themes.ThemeManager;
import org.apache.roller.weblogger.config.PingConfig;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.apache.xmlrpc.util.SAXParsers;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import java.io.IOException;
import java.util.Properties;


/**
 * Abstract base implementation of the Weblogger interface.
 * 
 * <p>This class provides the common functionality for all Weblogger implementations,
 * regardless of their persistence strategy. It manages the lifecycle of all business
 * managers and handles common initialization and shutdown logic.</p>
 * 
 * <p>Concrete implementations should extend this class and implement the abstract
 * methods for persistence-specific operations.</p>
 * 
 * @see Weblogger
 */
@com.google.inject.Singleton
public abstract class WebloggerImpl implements Weblogger {
    
    private static final Log log = LogFactory.getLog(WebloggerImpl.class);
    
    // ========================================================================
    // Manager Dependencies
    // ========================================================================
    
    // Core business managers
    private final UserManager          userManager;
    private final WeblogManager        weblogManager;
    private final WeblogEntryManager   weblogEntryManager;
    private final MediaFileManager     mediaFileManager;
    private final FileContentManager   fileContentManager;
    private final BookmarkManager      bookmarkManager;
    
    // Configuration & properties managers
    private final PropertiesManager    propertiesManager;
    private final ThemeManager         themeManager;
    private final PluginManager        pluginManager;
    
    // Ping & OAuth managers
    private final AutoPingManager      autoPingManager;
    private final PingTargetManager    pingTargetManager;
    private final PingQueueManager     pingQueueManager;
    private final OAuthManager         oauthManager;
    
    // Infrastructure managers
    private final ThreadManager        threadManager;
    private final IndexManager         indexManager;
    
    // Planet (aggregation) components
    private final FeedFetcher          feedFetcher;
    private final PlanetManager        planetManager;
    private final org.apache.roller.planet.business.PlanetURLStrategy planetUrlStrategy;
    
    // URL strategy
    private final URLStrategy          urlStrategy;
    
    // Build information
    private final BuildInfo            buildInfo;
    
    
    /**
     * Construct a WebloggerImpl instance with all required dependencies.
     * 
     * @param userManager User management operations
     * @param weblogManager Weblog management operations
     * @param weblogEntryManager Entry management operations
     * @param mediaFileManager Media file operations
     * @param fileContentManager File content operations
     * @param bookmarkManager Bookmark operations
     * @param propertiesManager Global properties management
     * @param themeManager Theme operations
     * @param pluginManager Plugin operations
     * @param autoPingManager Auto-ping operations
     * @param pingTargetManager Ping target operations
     * @param pingQueueManager Ping queue operations
     * @param oauthManager OAuth operations
     * @param threadManager Background thread operations
     * @param indexManager Search indexing operations
     * @param feedFetcher Feed fetching operations
     * @param planetManager Planet operations
     * @param planetUrlStrategy Planet URL building strategy
     * @param urlStrategy Main URL building strategy
     * @throws WebloggerException if construction fails
     */
    protected WebloggerImpl(
            UserManager          userManager,
            WeblogManager        weblogManager,
            WeblogEntryManager   weblogEntryManager,
            MediaFileManager     mediaFileManager,
            FileContentManager   fileContentManager,
            BookmarkManager      bookmarkManager,
            PropertiesManager    propertiesManager,
            ThemeManager         themeManager,
            PluginManager        pluginManager,
            AutoPingManager      autoPingManager,
            PingTargetManager    pingTargetManager,
            PingQueueManager     pingQueueManager,
            OAuthManager         oauthManager,
            ThreadManager        threadManager,
            IndexManager         indexManager,
            FeedFetcher          feedFetcher,
            PlanetManager        planetManager,
            org.apache.roller.planet.business.PlanetURLStrategy planetUrlStrategy,
            URLStrategy          urlStrategy) throws WebloggerException {
        
        // Assign all dependencies
        this.userManager         = userManager;
        this.weblogManager       = weblogManager;
        this.weblogEntryManager  = weblogEntryManager;
        this.mediaFileManager    = mediaFileManager;
        this.fileContentManager  = fileContentManager;
        this.bookmarkManager     = bookmarkManager;
        this.propertiesManager   = propertiesManager;
        this.themeManager        = themeManager;
        this.pluginManager       = pluginManager;
        this.autoPingManager     = autoPingManager;
        this.pingTargetManager   = pingTargetManager;
        this.pingQueueManager    = pingQueueManager;
        this.oauthManager        = oauthManager;
        this.threadManager       = threadManager;
        this.indexManager        = indexManager;
        this.feedFetcher         = feedFetcher;
        this.planetManager       = planetManager;
        this.planetUrlStrategy   = planetUrlStrategy;
        this.urlStrategy         = urlStrategy;
        
        // Load build information
        this.buildInfo = new BuildInfo();
    }
    
    
    // ========================================================================
    // Core Business Manager Accessors
    // ========================================================================
    
    @Override
    public UserManager getUserManager() {
        return userManager;
    }
    
    @Override
    public WeblogManager getWeblogManager() {
        return weblogManager;
    }
    
    @Override
    public WeblogEntryManager getWeblogEntryManager() {
        return weblogEntryManager;
    }
    
    @Override
    public MediaFileManager getMediaFileManager() {
        return mediaFileManager;
    }
    
    @Override
    public FileContentManager getFileContentManager() {
        return fileContentManager;
    }
    
    @Override
    public BookmarkManager getBookmarkManager() {
        return bookmarkManager;
    }
    
    
    // ========================================================================
    // Configuration & Properties Manager Accessors
    // ========================================================================
    
    @Override
    public PropertiesManager getPropertiesManager() {
        return propertiesManager;
    }
    
    @Override
    public ThemeManager getThemeManager() {
        return themeManager;
    }
    
    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }
    
    
    // ========================================================================
    // Ping & OAuth Manager Accessors
    // ========================================================================
    
    @Override
    public AutoPingManager getAutopingManager() {
        return autoPingManager;
    }
    
    @Override
    public PingTargetManager getPingTargetManager() {
        return pingTargetManager;
    }
    
    @Override
    public PingQueueManager getPingQueueManager() {
        return pingQueueManager;
    }
    
    @Override
    public OAuthManager getOAuthManager() {
        return oauthManager;
    }
    
    
    // ========================================================================
    // Infrastructure Manager Accessors
    // ========================================================================
    
    @Override
    public ThreadManager getThreadManager() {
        return threadManager;
    }
    
    @Override
    public IndexManager getIndexManager() {
        return indexManager;
    }
    
    
    // ========================================================================
    // Planet (Aggregation) Component Accessors
    // ========================================================================
    
    @Override
    public FeedFetcher getFeedFetcher() {
        return feedFetcher;
    }

    @Override
    public PlanetManager getPlanetManager() {
        return planetManager;
    }

    @Override
    public org.apache.roller.planet.business.PlanetURLStrategy getPlanetURLStrategy() {
        return planetUrlStrategy;
    }
    
    
    // ========================================================================
    // URL Strategy Accessor
    // ========================================================================

    @Override
    public URLStrategy getUrlStrategy() {
        return urlStrategy;
    }
    
    
    // ========================================================================
    // Lifecycle Management
    // ========================================================================
    
    /**
     * Initialize the Weblogger business tier.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Initializes all managers in the correct order</li>
     *   <li>Configures XML parsers for security</li>
     *   <li>Initializes ping systems</li>
     *   <li>Performs an initial flush</li>
     * </ul>
     * 
     * @throws InitializationException if initialization fails
     */
    @Override
    public void initialize() throws InitializationException {
        log.info("Initializing Roller Weblogger business tier");
        
        try {
            // Initialize managers in dependency order
            initializeManagers();
            
            // Configure XML security
            configureXmlSecurity();
            
            // Initialize ping systems
            initializePingSystems();
            
            // Flush initial state
            performInitialFlush();
            
            log.info("Roller Weblogger business tier successfully initialized");
            
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize Weblogger", e);
        }
    }
    
    /**
     * Initialize all managers in the correct dependency order.
     * 
     * @throws InitializationException if manager initialization fails
     */
    private void initializeManagers() throws InitializationException {
        try {
            // Initialize in dependency order
            propertiesManager.initialize();
            themeManager.initialize();
            threadManager.initialize();
            indexManager.initialize();
            mediaFileManager.initialize();
        } catch (InitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new InitializationException("Manager initialization failed", e);
        }
    }
    
    /**
     * Configure XML parser security to prevent XXE attacks.
     */
    private void configureXmlSecurity() {
        SAXParserFactory spf = SAXParsers.getSAXParserFactory();
        try {
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException | SAXNotRecognizedException | 
                 SAXNotSupportedException e) {
            String message = "Unable to configure XML security features. XML-RPC may be vulnerable to XXE attacks";
            if (log.isDebugEnabled()) {
                log.error(message, e);
            } else {
                log.error(message);
            }
        }
    }
    
    /**
     * Initialize ping-related systems.
     * 
     * @throws InitializationException if ping initialization fails
     */
    private void initializePingSystems() throws InitializationException {
        try {
            // Initialize common ping targets from configuration
            PingConfig.initializeCommonTargets();
            
            // Initialize ping variants
            PingConfig.initializePingVariants();
            
            // Remove all auto-ping configurations if ping usage is disabled
            if (PingConfig.getDisablePingUsage()) {
                log.info("Ping usage disabled - removing all auto-ping configurations");
                WebloggerFactory.getWeblogger().getAutopingManager().removeAllAutoPings();
            }
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize ping systems", e);
        }
    }
    
    /**
     * Perform initial flush to persist any initialization changes.
     * 
     * @throws InitializationException if flush fails
     */
    private void performInitialFlush() throws InitializationException {
        try {
            flush();
        } catch (WebloggerException e) {
            throw new InitializationException("Initial flush failed", e);
        }
    }
    
    /**
     * Shutdown the Weblogger business tier.
     * 
     * <p>This method performs cleanup of all managed resources.</p>
     */
    @Override
    public void shutdown() {
        log.info("Shutting down Roller Weblogger business tier");
        
        try {
            // Shutdown components in reverse dependency order
            shutdownHitCountQueue();
            shutdownIndexManager();
            shutdownThreadManager();
            
            log.info("Roller Weblogger business tier shutdown complete");
        } catch (Exception e) {
            log.error("Error during Weblogger shutdown", e);
        }
    }
    
    /**
     * Shutdown the hit count queue.
     */
    private void shutdownHitCountQueue() {
        try {
            HitCountQueue.getInstance().shutdown();
        } catch (Exception e) {
            log.error("Error shutting down hit count queue", e);
        }
    }
    
    /**
     * Shutdown the index manager.
     */
    private void shutdownIndexManager() {
        try {
            if (indexManager != null) {
                indexManager.shutdown();
            }
        } catch (Exception e) {
            log.error("Error shutting down index manager", e);
        }
    }
    
    /**
     * Shutdown the thread manager.
     */
    private void shutdownThreadManager() {
        try {
            if (threadManager != null) {
                threadManager.shutdown();
            }
        } catch (Exception e) {
            log.error("Error shutting down thread manager", e);
        }
    }
    
    /**
     * Release all resources associated with the current session.
     * 
     * <p>This method releases resources from all managers.</p>
     */
    @Override
    public void release() {
        try {
            releaseManagers();
        } catch (Exception e) {
            log.error("Error releasing Weblogger resources", e);
        }
    }
    
    /**
     * Release resources from all managers.
     */
    private void releaseManagers() {
        autoPingManager.release();
        bookmarkManager.release();
        mediaFileManager.release();
        fileContentManager.release();
        pingTargetManager.release();
        pingQueueManager.release();
        pluginManager.release();
        threadManager.release();
        userManager.release();
        weblogManager.release();
    }
    
    
    // ========================================================================
    // Version Information
    // ========================================================================
    
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
    
    
    // ========================================================================
    // Build Information Inner Class
    // ========================================================================
    
    /**
     * Encapsulates build information loaded from properties file.
     */
    private static class BuildInfo {
        private final String version;
        private final String revision;
        private final String buildTime;
        private final String buildUser;
        
        /**
         * Load build information from roller-version.properties.
         */
        BuildInfo() {
            Properties props = new Properties();
            try {
                props.load(getClass().getResourceAsStream("/roller-version.properties"));
            } catch (IOException e) {
                log.error("Failed to load roller-version.properties", e);
            }
            
            this.version = props.getProperty("ro.version", "UNKNOWN");
            this.revision = props.getProperty("ro.revision", "UNKNOWN");
            this.buildTime = props.getProperty("ro.buildTime", "UNKNOWN");
            this.buildUser = props.getProperty("ro.buildUser", "UNKNOWN");
        }
        
        String getVersion() { return version; }
        String getRevision() { return revision; }
        String getBuildTime() { return buildTime; }
        String getBuildUser() { return buildUser; }
    }
}
