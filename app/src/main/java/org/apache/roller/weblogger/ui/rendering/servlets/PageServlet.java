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

package org.apache.roller.weblogger.ui.rendering.servlets;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.roller.util.RollerConstants;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.HitCountQueue;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.themes.ThemeManager;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.StaticThemeTemplate;
import org.apache.roller.weblogger.pojos.TemplateRendition.TemplateLanguage;
import org.apache.roller.weblogger.pojos.ThemeTemplate;
import org.apache.roller.weblogger.pojos.ThemeTemplate.ComponentType;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogTheme;
import org.apache.roller.weblogger.ui.core.RollerContext;
import org.apache.roller.weblogger.ui.rendering.Renderer;
import org.apache.roller.weblogger.ui.rendering.RendererManager;
import org.apache.roller.weblogger.ui.rendering.model.ModelLoader;
import org.apache.roller.weblogger.ui.rendering.util.InvalidRequestException;
import org.apache.roller.weblogger.ui.rendering.util.ModDateHeaderUtil;
import org.apache.roller.weblogger.ui.rendering.util.WeblogEntryCommentForm;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;
import org.apache.roller.weblogger.ui.rendering.util.cache.SiteWideCache;
import org.apache.roller.weblogger.ui.rendering.util.cache.WeblogPageCache;
import org.apache.roller.weblogger.util.BannedwordslistChecker;
import org.apache.roller.weblogger.util.I18nMessages;
import org.apache.roller.weblogger.util.cache.CachedContent;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Provides access to weblog pages.
 */
public class PageServlet extends HttpServlet {

    private static Log log = LogFactory.getLog(PageServlet.class);
    // for referrer processing
    private boolean processReferrers = true;
    private static Pattern robotPattern = null;
    // for caching
    private boolean excludeOwnerPages = false;
    private WeblogPageCache weblogPageCache = null;
    private SiteWideCache siteWideCache = null;

    // Development theme reloading
    Boolean themeReload = false;

    /**
     * Init method for this servlet
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

        super.init(servletConfig);

        log.info("Initializing PageServlet");

        this.excludeOwnerPages = WebloggerConfig
                .getBooleanProperty("cache.excludeOwnerEditPages");

        // get a reference to the weblog page cache
        this.weblogPageCache = WeblogPageCache.getInstance();

        // get a reference to the site wide cache
        this.siteWideCache = SiteWideCache.getInstance();

        // see if built-in referrer spam check is enabled
        this.processReferrers = WebloggerConfig
                .getBooleanProperty("site.bannedwordslist.enable.referrers");

        log.info("Referrer spam check enabled = " + this.processReferrers);

        // check for possible robot pattern
        String robotPatternStr = WebloggerConfig
                .getProperty("referrer.robotCheck.userAgentPattern");
        if (robotPatternStr != null && robotPatternStr.length() > 0) {
            // Parse the pattern, and store the compiled form.
            try {
                robotPattern = Pattern.compile(robotPatternStr);
            } catch (Exception e) {
                // Most likely a PatternSyntaxException; log and continue as if
                // it is not set.
                log.error(
                        "Error parsing referrer.robotCheck.userAgentPattern value '"
                                + robotPatternStr
                                + "'.  Robots will not be filtered. ", e);
            }
        }

        // Development theme reloading
        themeReload = WebloggerConfig.getBooleanProperty("themes.reload.mode");
    }

    /**
     * Handle GET requests for weblog pages.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.debug("Entering");

        // Check for spam referrers early
        if (this.processReferrers && this.processReferrer(request)) {
            log.debug("spammer, giving 'em a 403");
            if (!response.isCommitted()) {
                response.reset();
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // Parse request and get weblog
        WeblogPageRequest pageRequest;
        try {
            pageRequest = new WeblogPageRequest(request);
            Weblog weblog = pageRequest.getWeblog();
            if (weblog == null) {
                throw new WebloggerException("unable to lookup weblog: "
                        + pageRequest.getWeblogHandle());
            }
        } catch (Exception e) {
            log.debug("error creating page request", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean isSiteWide = WebloggerRuntimeConfig.isSiteWideWeblog(
                pageRequest.getWeblogHandle());

        // Handle 304 Not Modified
        if (!handleNotModified(request, response, pageRequest, isSiteWide)) {
            return; // 304 was sent
        }

        // Generate cache key
        String cacheKey = generateCacheKey(pageRequest, isSiteWide);

        // Handle theme reloading in development mode
        handleThemeReload(pageRequest, isSiteWide);

        // Try to serve from cache
        if (tryServeFromCache(request, response, pageRequest, cacheKey, isSiteWide)) {
            return; // Cache hit, response sent
        }

        log.debug("Looking for template to use for rendering");

        // Find the template to render
        ThemeTemplate page = findTemplate(request, pageRequest);
        if (page == null) {
            if (!response.isCommitted()) {
                response.reset();
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        log.debug("page found, dealing with it");

        // Validate the request
        if (isInvalidRequest(pageRequest, page, isSiteWide)) {
            log.debug("page failed validation, bailing out");
            if (!response.isCommitted()) {
                response.reset();
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Force locale if needed
        if (pageRequest.getLocale() == null && !pageRequest.getWeblog().isShowAllLangs()) {
            pageRequest.setLocale(pageRequest.getWeblog().getLocale());
        }

        // Process hit counting
        if (!isSiteWide && (pageRequest.isWebsitePageHit() || pageRequest.isOtherPageHit())) {
            this.processHit(pageRequest.getWeblog());
        }

        // Determine content type
        String contentType = determineContentType(page);

        // Build the rendering model
        HashMap<String, Object> model = buildRenderingModel(request, response, pageRequest);
        if (model == null) {
            // Error already sent in buildRenderingModel
            return;
        }

        // Render the content
        CachedContent rendererOutput = renderContent(page, pageRequest, model, contentType);
        if (rendererOutput == null) {
            // Error already sent in renderContent
            if (!response.isCommitted()) {
                response.reset();
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Send response
        log.debug("Flushing response output");
        response.setContentType(contentType);
        response.setContentLength(rendererOutput.getContent().length);
        response.getOutputStream().write(rendererOutput.getContent());

        // Cache the rendered content
        cacheRenderedContent(request, pageRequest, cacheKey, rendererOutput, isSiteWide);

        log.debug("Exiting");
    }

    /**
     * Handle 304 Not Modified logic.
     * @return true if processing should continue, false if 304 was sent
     */
    private boolean handleNotModified(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      WeblogPageRequest pageRequest, 
                                      boolean isSiteWide) throws IOException {
        
        Weblog weblog = pageRequest.getWeblog();
        long lastModified = System.currentTimeMillis();
        
        if (isSiteWide) {
            lastModified = siteWideCache.getLastModified().getTime();
        } else if (weblog.getLastModified() != null) {
            lastModified = weblog.getLastModified().getTime();
        }

        // Skip 304 for logged in users to ensure they see edit links
        if (!pageRequest.isLoggedIn()) {
            if (ModDateHeaderUtil.respondIfNotModified(request, response,
                    lastModified, pageRequest.getDeviceType())) {
                return false; // 304 sent
            }
            ModDateHeaderUtil.setLastModifiedHeader(response, lastModified,
                    pageRequest.getDeviceType());
        }
        
        return true; // Continue processing
    }

    /**
     * Generate cache key for the request.
     */
    private String generateCacheKey(WeblogPageRequest pageRequest, boolean isSiteWide) {
        if (isSiteWide) {
            return siteWideCache.generateKey(pageRequest);
        }
        return weblogPageCache.generateKey(pageRequest);
    }

    /**
     * Handle theme reloading in development mode.
     */
    private void handleThemeReload(WeblogPageRequest pageRequest, boolean isSiteWide) {
        Weblog weblog = pageRequest.getWeblog();
        
        if (!themeReload || weblog.getEditorTheme().equals(WeblogTheme.CUSTOM)) {
            return;
        }
        
        if (pageRequest.getPathInfo() != null && pageRequest.getPathInfo().endsWith(".css")) {
            return;
        }

        try {
            ThemeManager manager = WebloggerFactory.getWeblogger().getThemeManager();
            boolean reloaded = manager.reLoadThemeFromDisk(weblog.getEditorTheme());
            
            if (reloaded) {
                if (isSiteWide) {
                    siteWideCache.clear();
                } else {
                    weblogPageCache.clear();
                }
                I18nMessages.reloadBundle(weblog.getLocaleInstance());
            }
        } catch (Exception ex) {
            log.error("ERROR - reloading theme " + ex);
        }
    }

    /**
     * Try to serve content from cache.
     * @return true if cache hit and response sent, false otherwise
     */
    private boolean tryServeFromCache(HttpServletRequest request,
                                      HttpServletResponse response,
                                      WeblogPageRequest pageRequest,
                                      String cacheKey,
                                      boolean isSiteWide) throws IOException {
        
        // Check if caching is disabled
        if ((this.excludeOwnerPages && pageRequest.isLoggedIn())
                || request.getAttribute("skipCache") != null
                || request.getParameter("skipCache") != null) {
            return false;
        }

        CachedContent cachedContent;
        if (isSiteWide) {
            cachedContent = (CachedContent) siteWideCache.get(cacheKey);
        } else {
            Weblog weblog = pageRequest.getWeblog();
            long lastModified = (weblog.getLastModified() != null) 
                    ? weblog.getLastModified().getTime() 
                    : System.currentTimeMillis();
            cachedContent = (CachedContent) weblogPageCache.get(cacheKey, lastModified);
        }

        if (cachedContent != null) {
            log.debug("HIT " + cacheKey);

            // Process hit counting even for cached content
            if (!isSiteWide && (pageRequest.isWebsitePageHit() || pageRequest.isOtherPageHit())) {
                this.processHit(pageRequest.getWeblog());
            }

            response.setContentLength(cachedContent.getContent().length);
            response.setContentType(cachedContent.getContentType());
            response.getOutputStream().write(cachedContent.getContent());
            return true;
        }
        
        log.debug("MISS " + cacheKey);
        return false;
    }

    /**
     * Find the appropriate template for the request.
     */
    private ThemeTemplate findTemplate(HttpServletRequest request, 
                                       WeblogPageRequest pageRequest) {
        Weblog weblog = pageRequest.getWeblog();
        ThemeTemplate page = null;

        // Handle popup requests
        if (request.getParameter("popup") != null) {
            page = findPopupTemplate(weblog);
        }
        // Handle explicit page requests
        else if ("page".equals(pageRequest.getContext())) {
            page = pageRequest.getWeblogPage();
        }
        // Handle tags index
        else if ("tags".equals(pageRequest.getContext()) && pageRequest.getTags() != null) {
            page = findTagsTemplate(weblog);
        }
        // Handle permalink
        else if (pageRequest.getWeblogAnchor() != null) {
            page = findPermalinkTemplate(weblog);
        }

        // Fall back to default template
        if (page == null) {
            page = findDefaultTemplate(weblog);
        }

        return page;
    }

    private ThemeTemplate findPopupTemplate(Weblog weblog) {
        try {
            ThemeTemplate page = weblog.getTheme().getTemplateByName("_popupcomments");
            if (page != null) {
                return page;
            }
        } catch (Exception e) {
            // ignored ... considered page not found
        }
        return new StaticThemeTemplate("templates/weblog/popupcomments.vm", 
                                       TemplateLanguage.VELOCITY);
    }

    private ThemeTemplate findTagsTemplate(Weblog weblog) {
        try {
            return weblog.getTheme().getTemplateByAction(ComponentType.TAGSINDEX);
        } catch (Exception e) {
            log.error("Error getting weblog page for action 'tagsIndex'", e);
        }
        return null;
    }

    private ThemeTemplate findPermalinkTemplate(Weblog weblog) {
        try {
            return weblog.getTheme().getTemplateByAction(ComponentType.PERMALINK);
        } catch (Exception e) {
            log.error("Error getting weblog page for action 'permalink'", e);
        }
        return null;
    }

    private ThemeTemplate findDefaultTemplate(Weblog weblog) {
        try {
            return weblog.getTheme().getDefaultTemplate();
        } catch (Exception e) {
            log.error("Error getting default page for weblog = " + weblog.getHandle(), e);
        }
        return null;
    }

    /**
     * Validate the request against the selected template.
     */
    private boolean isInvalidRequest(WeblogPageRequest pageRequest, 
                                     ThemeTemplate page,
                                     boolean isSiteWide) {
        
        // Hidden pages can't be accessed directly
        if (pageRequest.getWeblogPageName() != null && page.isHidden()) {
            return true;
        }
        
        // Locale view only if enabled
        if (pageRequest.getLocale() != null 
                && !pageRequest.getWeblog().isEnableMultiLang()) {
            return true;
        }
        
        // Validate permalink requests
        if (pageRequest.getWeblogAnchor() != null) {
            return isInvalidPermalinkRequest(pageRequest);
        }
        
        // Validate category requests
        if (pageRequest.getWeblogCategoryName() != null 
                && pageRequest.getWeblogCategory() == null) {
            return true;
        }
        
        // Validate tag requests
        if (pageRequest.getTags() != null && !pageRequest.getTags().isEmpty()) {
            return isInvalidTagRequest(pageRequest, isSiteWide);
        }
        
        return false;
    }

    private boolean isInvalidPermalinkRequest(WeblogPageRequest pageRequest) {
        WeblogEntry entry = pageRequest.getWeblogEntry();
        
        if (entry == null) {
            return true;
        }
        
        if (pageRequest.getLocale() != null 
                && !entry.getLocale().startsWith(pageRequest.getLocale())) {
            return true;
        }
        
        if (!entry.isPublished()) {
            return true;
        }
        
        if (new Date().before(entry.getPubTime())) {
            return true;
        }
        
        return false;
    }

    private boolean isInvalidTagRequest(WeblogPageRequest pageRequest, boolean isSiteWide) {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger()
                    .getWeblogEntryManager();
            return !wmgr.getTagComboExists(pageRequest.getTags(),
                    isSiteWide ? null : pageRequest.getWeblog());
        } catch (WebloggerException ex) {
            return true;
        }
    }

    /**
     * Determine the content type for the response.
     */
    private String determineContentType(ThemeTemplate page) {
        if (StringUtils.isNotEmpty(page.getOutputContentType())) {
            return page.getOutputContentType() + "; charset=utf-8";
        }

        final String defaultContentType = "text/html; charset=utf-8";
        
        if (page.getLink() == null) {
            return defaultContentType;
        }

        String mimeType = RollerContext.getServletContext().getMimeType(page.getLink());
        if (mimeType != null) {
            return mimeType + "; charset=utf-8";
        }
        
        return defaultContentType;
    }

    /**
     * Build the rendering model with all necessary data.
     */
    private HashMap<String, Object> buildRenderingModel(HttpServletRequest request,
                                                         HttpServletResponse response,
                                                         WeblogPageRequest pageRequest) {
        HashMap<String, Object> model = new HashMap<>();
        
        try {
            PageContext pageContext = JspFactory.getDefaultFactory()
                    .getPageContext(this, request, response, "", false,
                            RollerConstants.EIGHT_KB_IN_BYTES, true);

            // special hack for menu tag
            request.setAttribute("pageRequest", pageRequest);

            // populate the rendering model
            Map<String, Object> initData = new HashMap<>();
            initData.put("requestParameters", request.getParameterMap());
            initData.put("parsedRequest", pageRequest);
            initData.put("pageContext", pageContext);
            initData.put("urlStrategy", WebloggerFactory.getWeblogger().getUrlStrategy());

            // if this was a comment posting, check for comment form
            WeblogEntryCommentForm commentForm = (WeblogEntryCommentForm) request
                    .getAttribute("commentForm");
            if (commentForm != null) {
                initData.put("commentForm", commentForm);
            }

            // Load models for pages
            String pageModels = WebloggerConfig.getProperty("rendering.pageModels");
            ModelLoader.loadModels(pageModels, model, initData, true);
            
            // Load special models for site-wide blog
            if (WebloggerRuntimeConfig.isSiteWideWeblog(pageRequest.getWeblog().getHandle())) {
                String siteModels = WebloggerConfig.getProperty("rendering.siteModels");
                ModelLoader.loadModels(siteModels, model, initData, true);
            }

        } catch (WebloggerException ex) {
            log.error("Error loading model objects for page", ex);
            return null;
        }
        
        return model;
    }

    /**
     * Render the content using the appropriate renderer.
     */
    private CachedContent renderContent(ThemeTemplate page,
                                        WeblogPageRequest pageRequest,
                                        HashMap<String, Object> model,
                                        String contentType) {
        
        // Lookup renderer
        Renderer renderer;
        try {
            log.debug("Looking up renderer");
            renderer = RendererManager.getRenderer(page, pageRequest.getDeviceType());
        } catch (Exception e) {
            log.error("Couldn't find renderer for page " + page.getId(), e);
            return null;
        }

        // Render content
        CachedContent rendererOutput = new CachedContent(
                RollerConstants.TWENTYFOUR_KB_IN_BYTES, contentType);
        try {
            log.debug("Doing rendering");
            renderer.render(model, rendererOutput.getCachedWriter());
            rendererOutput.flush();
            rendererOutput.close();
        } catch (Exception e) {
            log.error("Error during rendering for page " + page.getId(), e);
            return null;
        }
        
        return rendererOutput;
    }

    /**
     * Cache the rendered content if appropriate.
     */
    private void cacheRenderedContent(HttpServletRequest request,
                                      WeblogPageRequest pageRequest,
                                      String cacheKey,
                                      CachedContent rendererOutput,
                                      boolean isSiteWide) {
        
        if ((this.excludeOwnerPages && pageRequest.isLoggedIn())
                || request.getAttribute("skipCache") != null) {
            log.debug("SKIPPED " + cacheKey);
            return;
        }

        log.debug("PUT " + cacheKey);
        
        if (isSiteWide) {
            siteWideCache.put(cacheKey, rendererOutput);
        } else {
            weblogPageCache.put(cacheKey, rendererOutput);
        }
    }

    /**
     * Handle POST requests.
     * 
     * We have this here because the comment servlet actually forwards some of
     * its requests on to us to render some pages with custom messaging. We may
     * want to revisit this approach in the future and see if we can do this in
     * a different way, but for now this is the easy way.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // make sure caching is disabled
        request.setAttribute("skipCache", "true");

        // handle just like a GET request
        this.doGet(request, response);
    }

    /**
     * Notify the hit tracker that it has an incoming page hit.
     */
    private void processHit(Weblog weblog) {
        HitCountQueue counter = HitCountQueue.getInstance();
        counter.processHit(weblog);
    }

    /**
     * Process the incoming request to extract referrer info and pass it on to
     * the referrer processing queue for tracking.
     * 
     * @return true if referrer was spam, false otherwise
     */
    private boolean processReferrer(HttpServletRequest request) {

        log.debug("processing referrer for " + request.getRequestURI());

        WeblogPageRequest pageRequest;
        try {
            pageRequest = new WeblogPageRequest(request);
        } catch (InvalidRequestException ex) {
            return false;
        }

        // Skip site-wide frontpage
        if (WebloggerRuntimeConfig.isSiteWideWeblog(pageRequest.getWeblogHandle())) {
            return false;
        }

        // Skip robots
        if (isRobotRequest(request)) {
            log.debug("skipping referrer from robot");
            return false;
        }

        String referrerUrl = extractReferrerUrl(request);
        log.debug("referrer = " + referrerUrl);

        String requestUrl = buildRequestUrl(request);

        // Skip self-referrals
        if (isSelfReferral(pageRequest, referrerUrl)) {
            log.debug("skipping referrer from own blog");
            return false;
        }

        // Validate referrer
        return validateReferrer(pageRequest, referrerUrl, requestUrl);
    }

    private boolean isRobotRequest(HttpServletRequest request) {
        if (robotPattern == null) {
            return false;
        }
        
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && userAgent.length() > 0
                && robotPattern.matcher(userAgent).matches();
    }

    private String extractReferrerUrl(HttpServletRequest request) {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        String referer = request.getHeader("Referer");
        
        if (urlValidator.isValid(referer)) {
            return referer;
        }
        return null;
    }

    private String buildRequestUrl(HttpServletRequest request) {
        StringBuffer reqsb = request.getRequestURL();
        if (request.getQueryString() != null) {
            reqsb.append("?").append(request.getQueryString());
        }
        return reqsb.toString();
    }

    private boolean isSelfReferral(WeblogPageRequest pageRequest, String referrerUrl) {
        if (referrerUrl == null) {
            return false;
        }
        
        String selfSiteFragment = "/" + pageRequest.getWeblogHandle();
        return referrerUrl.contains(selfSiteFragment);
    }

    private boolean validateReferrer(WeblogPageRequest pageRequest, 
                                     String referrerUrl, 
                                     String requestUrl) {
        
        if (pageRequest.getWeblogHandle() == null) {
            return false;
        }

        String basePageUrlWWW = WebloggerRuntimeConfig.getAbsoluteContextURL()
                + "/" + pageRequest.getWeblogHandle();
        String basePageUrl = basePageUrlWWW;
        
        if (basePageUrlWWW.startsWith("http://www.")) {
            basePageUrl = "http://" + basePageUrlWWW.substring(11);
        }

        // Ignore referrers from user's own blog
        if (referrerUrl != null
                && (referrerUrl.startsWith(basePageUrl) 
                    || referrerUrl.startsWith(basePageUrlWWW))) {
            log.debug("Ignoring referer = " + referrerUrl);
            return false;
        }

        // Validate against banned words
        if (referrerUrl != null) {
            int lastSlash = requestUrl.indexOf('/', 8);
            if (lastSlash == -1) {
                lastSlash = requestUrl.length();
            }
            String requestSite = requestUrl.substring(0, lastSlash);

            boolean isEditorReferral = referrerUrl.startsWith(requestSite)
                    && referrerUrl.indexOf(".rol") >= requestSite.length();
            
            if (isEditorReferral) {
                return false;
            }
            
            return BannedwordslistChecker.checkReferrer(
                    pageRequest.getWeblog(), referrerUrl);
        }

        return false;
    }
}