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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.roller.weblogger.ui.rendering.util.WeblogFeedRequest;
import org.apache.roller.weblogger.util.Utilities;


/**
 * Cache for weblog feed content.
 */
public final class WeblogFeedCache extends AbstractWeblogCache {

    // a unique identifier for this cache, this is used as the prefix for
    // roller config properties that apply to this cache
    public static final String CACHE_ID = "cache.weblogfeed";

    // reference to our singleton instance
    private static final WeblogFeedCache singletonInstance = new WeblogFeedCache();


    private WeblogFeedCache() {
        initializeCache(CACHE_ID);
    }


    public static WeblogFeedCache getInstance() {
        return singletonInstance;
    }


    /**
     * Generate a cache key from a parsed weblog feed request.
     * This generates a key of the form ...
     *
     * <handle>/<type>/<format>/<term>[/category][/language][/excerpts]
     *
     * examples ...
     *
     * foo/entries/rss/en
     * foo/comments/rss/MyCategory/en
     * foo/entries/atom/en/excerpts
     *
     */
    public String generateKey(WeblogFeedRequest feedRequest) {

        StringBuilder key = new StringBuilder(128);

        key.append(CACHE_ID).append(':');
        key.append(feedRequest.getWeblogHandle());

        key.append('/').append(feedRequest.getType());
        key.append('/').append(feedRequest.getFormat());

        if (feedRequest.getTerm() != null) {
            key.append("/search/").append(feedRequest.getTerm());
        }

        if(feedRequest.getWeblogCategoryName() != null) {
            String cat = URLEncoder.encode(feedRequest.getWeblogCategoryName(), StandardCharsets.UTF_8);
            key.append('/').append(cat);
        }

        if(feedRequest.getTags() != null && !feedRequest.getTags().isEmpty()) {
            String[] tags = feedRequest.getTags().toArray(new String[0]);
            Arrays.sort(tags);
            key.append("/tags/").append(Utilities.stringArrayToString(tags,"+"));
        }

        if(feedRequest.getLocale() != null) {
            key.append('/').append(feedRequest.getLocale());
        }

        if(feedRequest.isExcerpts()) {
            key.append("/excerpts");
        }

        return key.toString();
    }

}