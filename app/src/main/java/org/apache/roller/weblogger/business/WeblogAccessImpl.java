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

import java.util.List;
import java.util.Map;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.plugins.PluginManager;
import org.apache.roller.weblogger.business.plugins.entry.WeblogEntryPlugin;
import org.apache.roller.weblogger.business.themes.ThemeManager;
import org.apache.roller.weblogger.pojos.TagStat;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogAccess;
import org.apache.roller.weblogger.pojos.WeblogBookmarkFolder;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.pojos.WeblogPermission;
import org.apache.roller.weblogger.pojos.WeblogTheme;

/**
 * Business-layer implementation of WeblogAccess.
 */
public final class WeblogAccessImpl implements WeblogAccess {

    private final WeblogService weblogService;
    private final ThemeManager themeManager;
    private final PluginManager pluginManager;
    private final UserManager userManager;
    private final URLStrategy urlStrategy;

    public WeblogAccessImpl(
            WeblogService weblogService,
            ThemeManager themeManager,
            PluginManager pluginManager,
            UserManager userManager,
            URLStrategy urlStrategy) {
        this.weblogService = weblogService;
        this.themeManager = themeManager;
        this.pluginManager = pluginManager;
        this.userManager = userManager;
        this.urlStrategy = urlStrategy;
    }

    @Override
    public WeblogTheme getTheme(Weblog weblog) throws WebloggerException {
        return themeManager.getTheme(weblog);
    }

    @Override
    public Map<String, WeblogEntryPlugin> getInitializedPlugins(Weblog weblog) throws WebloggerException {
        return pluginManager.getWeblogEntryPlugins(weblog);
    }

    @Override
    public WeblogEntry getWeblogEntry(Weblog weblog, String anchor) throws WebloggerException {
        return weblogService.getWeblogEntry(weblog, anchor);
    }

    @Override
    public List<WeblogEntry> getRecentEntries(Weblog weblog, int maxEntries) throws WebloggerException {
        return weblogService.getRecentEntries(weblog, maxEntries);
    }

    @Override
    public List<WeblogEntry> getRecentEntriesWithTags(Weblog weblog, List<String> tags, int maxEntries)
            throws WebloggerException {
        return weblogService.getRecentEntriesWithTags(weblog, tags, maxEntries);
    }

    @Override
    public User getCreator(Weblog weblog) throws WebloggerException {
        return weblogService.getCreator(weblog);
    }

    @Override
    public List<WeblogEntryComment> getRecentComments(Weblog weblog, int maxComments) throws WebloggerException {
        return weblogService.getRecentComments(weblog, maxComments);
    }

    @Override
    public WeblogBookmarkFolder getBookmarkFolder(Weblog weblog, String folderName) throws WebloggerException {
        return weblogService.getBookmarkFolder(weblog, folderName);
    }

    @Override
    public int getTodaysHits(Weblog weblog) throws WebloggerException {
        return weblogService.getTodaysHits(weblog);
    }

    @Override
    public List<TagStat> getPopularTags(Weblog weblog, int sinceDays, int maxTags) throws WebloggerException {
        return weblogService.getPopularTags(weblog, sinceDays, maxTags);
    }

    @Override
    public long getCommentCount(Weblog weblog) throws WebloggerException {
        return weblogService.getCommentCount(weblog);
    }

    @Override
    public long getEntryCount(Weblog weblog) throws WebloggerException {
        return weblogService.getEntryCount(weblog);
    }

    @Override
    public boolean hasUserPermissions(Weblog weblog, User user, List<String> actions) throws WebloggerException {
        WeblogPermission userPerms = new WeblogPermission(weblog, user, actions);
        return userManager.checkPermission(userPerms, user);
    }

    @Override
    public String getWeblogURL(Weblog weblog, boolean absolute) throws WebloggerException {
        return urlStrategy.getWeblogURL(weblog, null, absolute);
    }
}
