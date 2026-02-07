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
import org.apache.roller.weblogger.config.PingConfig;
import org.apache.xmlrpc.util.SAXParsers;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import org.apache.roller.weblogger.business.pings.AutoPingManager;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Handles security configuration and ping setup during Weblogger initialization.
 */
public class WebloggerInitializer {

    private static final Log log = LogFactory.getLog(WebloggerInitializer.class);

    /**
     * Configures XML parser security to prevent XXE attacks.
     */
    public void configureXmlSecurity() {
        SAXParserFactory spf = SAXParsers.getSAXParserFactory();
        try {
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            String msg = "Unable to disable external DTD support in SAXParser → XML vulnerable to XXE";
            log.error(msg + (log.isDebugEnabled() ? "" : " (stacktrace in debug)"), e);
        }
    }

    /**
     * Configures ping settings based on PingConfig.
     */
    public void configurePings(AutoPingManager autoPingManager) throws InitializationException {
        try {
            if (PingConfig.getDisablePingUsage()) {
                log.info("Ping usage disabled → removing all auto-ping configurations");
                autoPingManager.removeAllAutoPings();
            }
        } catch (Exception e) {
            throw new InitializationException("Error finalizing ping configuration", e);
        }
    }
}