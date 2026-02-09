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

package org.apache.roller.weblogger.pojos;

import java.util.List;
import java.util.Map;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.plugins.entry.WeblogEntryPlugin;

/**
 * Access interface for Weblog convenience methods.
 *
 * This interface exists in the pojos package to avoid direct dependencies
 * from domain objects to the business layer.
 */
public interface WeblogAccess {

    WeblogTheme getTheme(Weblog weblog) throws WebloggerException;

    Map<String, WeblogEntryPlugin> getInitializedPlugins(Weblog weblog) throws WebloggerException;

    WeblogEntry getWeblogEntry(Weblog weblog, String anchor) throws WebloggerException;

    List<WeblogEntry> getRecentEntries(Weblog weblog, int maxEntries) throws WebloggerException;

    List<WeblogEntry> getRecentEntriesWithTags(Weblog weblog, List<String> tags, int maxEntries)
            throws WebloggerException;

    User getCreator(Weblog weblog) throws WebloggerException;

    List<WeblogEntryComment> getRecentComments(Weblog weblog, int maxComments) throws WebloggerException;

    WeblogBookmarkFolder getBookmarkFolder(Weblog weblog, String folderName) throws WebloggerException;

    int getTodaysHits(Weblog weblog) throws WebloggerException;

    List<TagStat> getPopularTags(Weblog weblog, int sinceDays, int maxTags) throws WebloggerException;

    long getCommentCount(Weblog weblog) throws WebloggerException;

    long getEntryCount(Weblog weblog) throws WebloggerException;

    boolean hasUserPermissions(Weblog weblog, User user, List<String> actions) throws WebloggerException;

    String getWeblogURL(Weblog weblog, boolean absolute) throws WebloggerException;
}
