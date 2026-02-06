package org.apache.roller.weblogger.ui.rendering;

import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.business.plugins.entry.WeblogEntryPlugin;
import org.apache.roller.weblogger.util.HTMLSanitizer;
import org.apache.roller.weblogger.util.I18nMessages;

public class WeblogEntryRendering {
    private WeblogEntryRendering() {
    }
    private static final Log LOG = LogFactory.getLog(WeblogEntryRendering.class);

    public static String getTransformedText(WeblogEntry entry) {
        return render(entry, entry.getText());
    }

    public static String getTransformedSummary(WeblogEntry entry) {
        return render(entry, entry.getSummary());
    }

    public static String displayContent(WeblogEntry entry, String readMoreLink) {
        String displayContent;

        if (readMoreLink == null || readMoreLink.isBlank() || "nil".equals(readMoreLink)) {
            if (StringUtils.isNotEmpty(entry.getText())) {
                displayContent = getTransformedText(entry);
            } else {
                displayContent = getTransformedSummary(entry);
            }
        } else {
            if (StringUtils.isNotEmpty(entry.getSummary())) {
                displayContent = getTransformedSummary(entry);
                if (StringUtils.isNotEmpty(entry.getText())) {
                    List<String> args = List.of(readMoreLink);
                    String readMore = I18nMessages
                            .getMessages(entry.getWebsite().getLocaleInstance())
                            .getString("macro.weblog.readMoreLink", args);
                    displayContent += readMore;
                }
            } else {
                displayContent = getTransformedText(entry);
            }
        }

        return HTMLSanitizer.conditionallySanitize(displayContent);
    }

    public static String getDisplayContent(WeblogEntry entry) {
        return displayContent(entry, null);
    }

    private static String render(WeblogEntry entry, String str) {
        String ret = str;
        LOG.debug("Applying page plugins to string");
        Map<String, WeblogEntryPlugin> inPlugins = entry.getWebsite().getInitializedPlugins();
        if (str != null && inPlugins != null) {
            List<String> entryPlugins = entry.getPluginsList();

            if (entryPlugins != null && !entryPlugins.isEmpty()) {
                for (Map.Entry<String, WeblogEntryPlugin> pluginEntry : inPlugins.entrySet()) {
                    if (entryPlugins.contains(pluginEntry.getKey())) {
                        WeblogEntryPlugin pagePlugin = pluginEntry.getValue();
                        try {
                            ret = pagePlugin.render(entry, ret);
                        } catch (Exception e) {
                            LOG.error("ERROR from plugin: " + pagePlugin.getName(), e);
                        }
                    }
                }
            }
        }
        return HTMLSanitizer.conditionallySanitize(ret);
    }
}
