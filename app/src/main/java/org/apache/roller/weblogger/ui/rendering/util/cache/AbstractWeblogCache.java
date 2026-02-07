    /*
    * Licensed to the Apache Software Foundation (ASF) under one or more
    *  contributor license agreements.  The ASF licenses this file to You
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

    package org.apache.roller.weblogger.ui.rendering.util.cache;

    import java.util.Enumeration;
    import java.util.HashMap;
    import java.util.Map;
    import org.apache.commons.logging.Log;
    import org.apache.commons.logging.LogFactory;
    import org.apache.roller.weblogger.config.WebloggerConfig;
    import org.apache.roller.weblogger.util.cache.Cache;
    import org.apache.roller.weblogger.util.cache.CacheHandler;
    import org.apache.roller.weblogger.util.cache.CacheManager;
    import org.apache.roller.weblogger.util.cache.LazyExpiringCacheEntry;


    /**
     * Abstract base class for weblog cache implementations.
     * Provides common caching functionality including initialization, get, put, remove, and clear operations.
     */
    public abstract class AbstractWeblogCache {
        
        private static final Log log = LogFactory.getLog(AbstractWeblogCache.class);
        
        // keep cached content
        protected boolean cacheEnabled = true;
        protected Cache contentCache = null;
        
        
        /**
         * Initialize the cache with the given cache ID and no cache handler.
         * @param cacheId the unique identifier for this cache
         */
        protected void initializeCache(String cacheId) {
            initializeCache(cacheId, null);
        }
        
        
        /**
         * Initialize the cache with the given cache ID and optional cache handler.
         * @param cacheId the unique identifier for this cache
         * @param handler the cache handler for invalidation events (can be null)
         */
        protected void initializeCache(String cacheId, CacheHandler handler) {
            
            cacheEnabled = WebloggerConfig.getBooleanProperty(cacheId + ".enabled");
            
            Map<String, String> cacheProps = new HashMap<>();
            cacheProps.put("id", cacheId);
            
            Enumeration<Object> allProps = WebloggerConfig.keys();
            String prop;
            while(allProps.hasMoreElements()) {
                prop = (String) allProps.nextElement();
                
                // we are only interested in props for this cache
                if(prop.startsWith(cacheId + ".")) {
                    cacheProps.put(prop.substring(cacheId.length() + 1), 
                            WebloggerConfig.getProperty(prop));
                }
            }
            
            log.info(cacheProps);
            
            if(cacheEnabled) {
                contentCache = CacheManager.constructCache(handler, cacheProps);
            } else {
                log.warn("Caching has been DISABLED");
            }
        }
        
        
        /**
         * Retrieve an object from the cache.
         * @param key the cache key
         * @param lastModified timestamp to check if cached entry is still valid
         * @return the cached object or null if not found or expired
         */
        public Object get(String key, long lastModified) {
            
            if (!cacheEnabled) {
                return null;
            }
            
            Object entry = null;
            
            LazyExpiringCacheEntry lazyEntry = 
                    (LazyExpiringCacheEntry) this.contentCache.get(key);
            if(lazyEntry != null) {
                entry = lazyEntry.getValue(lastModified);
                
                if(entry != null) {
                    log.debug("HIT " + key);
                } else {
                    log.debug("HIT-EXPIRED " + key);
                }
                
            } else {
                log.debug("MISS " + key);
            }
            
            return entry;
        }
        
        
        /**
         * Retrieve an object from the cache (simple version without expiration check).
         * @param key the cache key
         * @return the cached object or null if not found
         */
        public Object get(String key) {
            
            if (!cacheEnabled) {
                return null;
            }
            
            Object entry = contentCache.get(key);
            
            if(entry == null) {
                log.debug("MISS " + key);
            } else {
                log.debug("HIT " + key);
            }
            
            return entry;
        }
        
        
        /**
         * Store an object in the cache.
         * @param key the cache key
         * @param value the object to cache
         */
        public void put(String key, Object value) {
            
            if (!cacheEnabled) {
                return;
            }
            
            contentCache.put(key, new LazyExpiringCacheEntry(value));
            log.debug("PUT " + key);
        }
        
        
        /**
         * Remove an object from the cache.
         * @param key the cache key
         */
        public void remove(String key) {
            
            if (!cacheEnabled) {
                return;
            }
            
            contentCache.remove(key);
            log.debug("REMOVE " + key);
        }
        
        
        /**
         * Clear all entries from the cache.
         */
        public void clear() {
            
            if (!cacheEnabled) {
                return;
            }
            
            contentCache.clear();
            log.debug("CLEAR");
        }
    }