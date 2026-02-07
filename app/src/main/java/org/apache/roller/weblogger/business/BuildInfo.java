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

import java.io.IOException;
import java.util.Properties;

/**
 * Encapsulates build and version information for Roller Weblogger.
 */
public class BuildInfo {

    private static final Log log = LogFactory.getLog(BuildInfo.class);
    private static final String UNKNOWN_VALUE = "UNKNOWN";

    private final String version;
    private final String revision;
    private final String buildTime;
    private final String buildUser;

    public BuildInfo() {
        Properties props = new Properties();
        try {
            props.load(getClass().getResourceAsStream("/roller-version.properties"));
        } catch (IOException e) {
            log.error("roller-version.properties not found", e);
        }

        this.version   = props.getProperty("ro.version",   UNKNOWN_VALUE);
        this.revision  = props.getProperty("ro.revision",  UNKNOWN_VALUE);
        this.buildTime = props.getProperty("ro.buildTime", UNKNOWN_VALUE);
        this.buildUser = props.getProperty("ro.buildUser", UNKNOWN_VALUE);
    }

    public String getVersion() {
        return version;
    }

    public String getRevision() {
        return revision;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public String getBuildUser() {
        return buildUser;
    }
}