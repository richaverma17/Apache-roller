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

import org.apache.roller.weblogger.business.plugins.PluginManager;
import org.apache.roller.weblogger.business.runnable.ThreadManager;
import org.apache.roller.weblogger.business.search.IndexManager;
import org.apache.roller.weblogger.business.themes.ThemeManager;

/**
 * Facade grouping system / infrastructure related managers:
 *   - PropertiesManager
 *   - ThemeManager
 *   - ThreadManager
 *   - IndexManager
 *   - PluginManager
 */
@com.google.inject.Singleton
public class SystemManagerFacade {

    private static final Log log = LogFactory.getLog(SystemManagerFacade.class);

    private final PropertiesManager propertiesManager;
    private final ThemeManager      themeManager;
    private final ThreadManager     threadManager;
    private final IndexManager      indexManager;
    private final PluginManager     pluginManager;

    @Inject
    public SystemManagerFacade(
            PropertiesManager propertiesManager,
            ThemeManager      themeManager,
            ThreadManager     threadManager,
            IndexManager      indexManager,
            PluginManager     pluginManager) {

        this.propertiesManager = propertiesManager;
        this.themeManager      = themeManager;
        this.threadManager     = threadManager;
        this.indexManager      = indexManager;
        this.pluginManager     = pluginManager;
    }

    // Getters

    public PropertiesManager getPropertiesManager() { return propertiesManager; }
    public ThemeManager      getThemeManager()      { return themeManager; }
    public ThreadManager     getThreadManager()     { return threadManager; }
    public IndexManager      getIndexManager()      { return indexManager; }
    public PluginManager     getPluginManager()     { return pluginManager; }

    // Lifecycle methods

    public void initialize() throws InitializationException {
        // Original initialization order preserved
        try {
            propertiesManager.initialize();
            themeManager.initialize();
            threadManager.initialize();
            indexManager.initialize();
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize one of the system components", e);
        }
    }

    public void release() {
        try {
            // Only some had release() in original code
            pluginManager.release();
            threadManager.release();
            // indexManager, themeManager, propertiesManager did not have release()
        } catch (Exception e) {
            log.error("Error releasing system-related managers", e);
        }
    }

    public void shutdown() {
        try {
            if (indexManager != null) {
                indexManager.shutdown();
            }
            if (threadManager != null) {
                threadManager.shutdown();
            }
        } catch (Exception e) {
            log.error("Error shutting down system components", e);
        }
    }
}