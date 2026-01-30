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

package org.apache.roller.weblogger.business;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.startup.WebloggerStartup;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.util.Reflection;


/**
 * Provides access to the Weblogger instance and bootstraps the business tier.
 */
public final class WebloggerFactory {
    
    private static final Log LOG = LogFactory.getLog(WebloggerFactory.class);
    
    // our configured weblogger provider
    private static WebloggerProvider webloggerProvider = null;

    // non-instantiable
    private WebloggerFactory() {
        // hello all you beautiful people
    }

    /**
     * True if bootstrap process has been completed, False otherwise.
     */
    public static boolean isBootstrapped() {
        return (webloggerProvider != null);
    }
    
    
    /**
     * Accessor to the Weblogger Weblogger business tier.
     * 
     * @return Weblogger An instance of Weblogger.
     * @throws IllegalStateException If the app has not been properly bootstrapped yet.
     */
    public static Weblogger getWeblogger() {
        validateBootstrapped();
        return webloggerProvider.getWeblogger();
    }
    
    
    /**
     * Bootstrap the Roller Weblogger business tier, uses default WebloggerProvider.
     *
     * Bootstrapping the application effectively instantiates all the necessary
     * pieces of the business tier and wires them together so that the app is 
     * ready to run.
     *
     * @throws IllegalStateException If the app has not been properly prepared yet.
     * @throws BootstrapException If an error happens during the bootstrap process.
     */
    public static void bootstrap() throws BootstrapException {
        validatePrepared();
        WebloggerProvider defaultProvider = createDefaultProvider();
        bootstrap(defaultProvider);
    }
    
    
    /**
     * Bootstrap the Roller Weblogger business tier, uses specified WebloggerProvider.
     *
     * Bootstrapping the application effectively instantiates all the necessary
     * pieces of the business tier and wires them together so that the app is 
     * ready to run.
     *
     * @param provider A WebloggerProvider to use for bootstrapping.
     * @throws IllegalStateException If the app has not been properly prepared yet.
     * @throws BootstrapException If an error happens during the bootstrap process.
     */
    public static void bootstrap(WebloggerProvider provider) throws BootstrapException {
        validatePrepared();
        validateProvider(provider);
        
        LOG.info("Bootstrapping Roller Weblogger business tier");
        LOG.info("Weblogger Provider = " + provider.getClass().getName());
        
        webloggerProvider = provider;
        webloggerProvider.bootstrap();
        
        validateWebloggerInstance();
        logSuccessfulBootstrap();
    }
    
    /**
     * Validates that the application has been properly prepared.
     * 
     * @throws IllegalStateException If the app has not been properly prepared yet.
     */
    private static void validatePrepared() {
        if (!WebloggerStartup.isPrepared()) {
            throw new IllegalStateException("Cannot bootstrap until application has been properly prepared");
        }
    }
    
    /**
     * Validates that the application has been bootstrapped.
     * 
     * @throws IllegalStateException If the app has not been properly bootstrapped yet.
     */
    private static void validateBootstrapped() {
        if (webloggerProvider == null) {
            throw new IllegalStateException("Roller Weblogger has not been bootstrapped yet");
        }
    }
    
    /**
     * Validates that the provider is not null.
     * 
     * @param provider The provider to validate
     * @throws NullPointerException If the provider is null
     */
    private static void validateProvider(WebloggerProvider provider) {
        if (provider == null) {
            throw new NullPointerException("WebloggerProvider is null");
        }
    }
    
    /**
     * Creates the default WebloggerProvider based on configuration.
     * 
     * @return WebloggerProvider instance
     * @throws BootstrapException If provider cannot be created
     */
    private static WebloggerProvider createDefaultProvider() throws BootstrapException {
        String providerClassname = getProviderClassname();
        return instantiateProvider(providerClassname);
    }
    
    /**
     * Retrieves the provider classname from configuration.
     * 
     * @return Provider classname
     * @throws NullPointerException If no provider is specified in config
     */
    private static String getProviderClassname() {
        String providerClassname = WebloggerConfig.getProperty("weblogger.provider.class");
        if (providerClassname == null) {
            throw new NullPointerException("No provider specified in config property 'weblogger.provider.class'");
        }
        return providerClassname;
    }
    
    /**
     * Instantiates a WebloggerProvider from the given classname.
     * 
     * @param classname The classname to instantiate
     * @return WebloggerProvider instance
     * @throws BootstrapException If instantiation fails
     */
    private static WebloggerProvider instantiateProvider(String classname) throws BootstrapException {
        try {
            return (WebloggerProvider) Reflection.newInstance(classname);
        } catch (ReflectiveOperationException ex) {
            throw new BootstrapException(
                "Error instantiating default provider: " + classname + 
                "; exception message: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Validates that the Weblogger instance was properly created.
     * 
     * @throws BootstrapException If the Weblogger instance is null
     */
    private static void validateWebloggerInstance() throws BootstrapException {
        if (webloggerProvider.getWeblogger() == null) {
            throw new BootstrapException("Bootstrapping failed, Weblogger instance is null");
        }
    }
    
    /**
     * Logs successful bootstrap information.
     */
    private static void logSuccessfulBootstrap() {
        Weblogger weblogger = webloggerProvider.getWeblogger();
        LOG.info("Roller Weblogger business tier successfully bootstrapped");
        LOG.info("   Version: " + weblogger.getVersion());
        LOG.info("   Revision: " + weblogger.getRevision());
    }
}
