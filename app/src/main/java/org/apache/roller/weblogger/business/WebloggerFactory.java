/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import org.apache.roller.weblogger.business.startup.WebloggerStartup;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.util.Reflection;

/**
 * Provides access to the Weblogger instance and bootstraps the business tier.
 */
public final class WebloggerFactory {

    private static final Log LOG = LogFactory.getLog(WebloggerFactory.class);

    private static WebloggerProvider webloggerProvider;

    // non-instantiable
    private WebloggerFactory() {
    }

    /**
     * True if bootstrap process has been completed.
     */
    public static boolean isBootstrapped() {
        return webloggerProvider != null;
    }

    /**
     * Accessor to the Weblogger business tier.
     */
    public static Weblogger getWeblogger() {
        ensureBootstrapped();
        return webloggerProvider.getWeblogger();
    }

    /**
     * Bootstrap using default provider.
     */
    public static void bootstrap() throws BootstrapException {
        ensurePrepared();
        WebloggerProvider provider = createDefaultProvider();
        bootstrap(provider);
    }

    /**
     * Bootstrap using supplied provider.
     */
    public static void bootstrap(WebloggerProvider provider)
            throws BootstrapException {

        ensurePrepared();
        ensureProvider(provider);

        LOG.info("Bootstrapping Roller Weblogger business tier");
        LOG.info("Weblogger Provider = " + provider.getClass().getName());

        webloggerProvider = provider;
        webloggerProvider.bootstrap();

        ensureWebloggerCreated();
        logBootstrapSuccess();
    }

    /* =========================
       Validation helpers
       ========================= */

    private static void ensurePrepared() {
        if (!WebloggerStartup.isPrepared()) {
            throw new IllegalStateException(
                    "Cannot bootstrap until application has been properly prepared");
        }
    }

    private static void ensureBootstrapped() {
        if (webloggerProvider == null) {
            throw new IllegalStateException(
                    "Roller Weblogger has not been bootstrapped yet");
        }
    }

    private static void ensureProvider(WebloggerProvider provider) {
        if (provider == null) {
            throw new NullPointerException("WebloggerProvider is null");
        }
    }

    private static void ensureWebloggerCreated() throws BootstrapException {
        if (webloggerProvider.getWeblogger() == null) {
            throw new BootstrapException(
                    "Bootstrapping failed, Weblogger instance is null");
        }
    }

    /* =========================
       Provider creation helpers
       ========================= */

    private static WebloggerProvider createDefaultProvider()
            throws BootstrapException {

        String classname = WebloggerConfig.getProperty(
                "weblogger.provider.class");

        if (classname == null) {
            throw new NullPointerException(
                    "No provider specified in config property 'weblogger.provider.class'");
        }

        return instantiateProvider(classname);
    }

    private static WebloggerProvider instantiateProvider(String classname)
            throws BootstrapException {

        try {
            return (WebloggerProvider) Reflection.newInstance(classname);
        } catch (ReflectiveOperationException ex) {
            throw new BootstrapException(
                    "Error instantiating default provider: " + classname,
                    ex);
        }
    }

    /* =========================
       Logging helpers
       ========================= */

    private static void logBootstrapSuccess() {
        Weblogger weblogger = webloggerProvider.getWeblogger();
        LOG.info("Roller Weblogger business tier successfully bootstrapped");
        LOG.info("   Version: " + weblogger.getVersion());
        LOG.info("   Revision: " + weblogger.getRevision());
    }
}
