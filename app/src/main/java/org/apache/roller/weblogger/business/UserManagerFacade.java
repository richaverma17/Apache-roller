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

import com.google.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.OAuthManager;

/**
 * Facade grouping user & authentication related managers:
 *   - UserManager
 *   - OAuthManager
 */
@com.google.inject.Singleton
public class UserManagerFacade {

    private static final Log log = LogFactory.getLog(UserManagerFacade.class);

    private final UserManager  userManager;
    private final OAuthManager oauthManager;

    @Inject
    public UserManagerFacade(
            UserManager  userManager,
            OAuthManager oauthManager) {

        this.userManager  = userManager;
        this.oauthManager = oauthManager;
    }

    // Getters

    public UserManager  getUserManager()  { return userManager; }
    public OAuthManager getOAuthManager() { return oauthManager; }

    // Lifecycle methods

    public void initialize() throws InitializationException {
        // No explicit initialization was present in original code
    }

    public void release() {
        try {
            userManager.release();
            // OAuthManager did not have release() in original
        } catch (Exception e) {
            log.error("Error releasing user-related managers", e);
        }
    }

    public void shutdown() {
        // No specific shutdown logic in original code
    }
}