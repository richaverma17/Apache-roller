package org.apache.roller.weblogger.business.jpa;

import java.util.List;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.Weblogger;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.pojos.WeblogEntry;
@com.google.inject.Singleton
public class CategoryService {

    private final Weblogger roller;
    private final JPAPersistenceStrategy strategy;

    @com.google.inject.Inject
    public CategoryService(Weblogger roller,
                           JPAPersistenceStrategy strategy) {
        this.roller = roller;
        this.strategy = strategy;
    }

    public void saveWeblogCategory(WeblogCategory cat) throws WebloggerException {
        boolean exists = getWeblogCategory(cat.getId()) != null;
        if (!exists && isDuplicateWeblogCategoryName(cat)) {
            throw new WebloggerException("Duplicate category name, cannot save category");
        }

        roller.getWeblogManager().saveWeblog(cat.getWeblog());
        strategy.store(cat);
    }

    private WeblogCategory getWeblogCategory(String id)
            throws WebloggerException {

        return (WeblogCategory) strategy.load(WeblogCategory.class, id);
    }

    private boolean isDuplicateWeblogCategoryName(WeblogCategory cat)
            throws WebloggerException {

        return getWeblogCategoryByName(cat.getWeblog(), cat.getName()) != null;
    }



    private WeblogCategory getWeblogCategoryByName(Weblog weblog, String name)
            throws WebloggerException {

        TypedQuery<WeblogCategory> q = strategy.getNamedQuery(
                "WeblogCategory.getByWeblog&Name", WeblogCategory.class);
        q.setParameter(1, weblog);
        q.setParameter(2, name);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }


    public void removeWeblogCategory(WeblogCategory cat)
            throws WebloggerException {
        if(!cat.retrieveWeblogEntries(false).isEmpty()) {
            throw new WebloggerException("Cannot remove category with entries");
        }

        cat.getWeblog().getWeblogCategories().remove(cat);

        // remove cat
        this.strategy.remove(cat);

        if(cat.equals(cat.getWeblog().getBloggerCategory())) {
            cat.getWeblog().setBloggerCategory(null);
            this.strategy.store(cat.getWeblog());
        }

        // update weblog last modified date.  date updated by saveWebsite()
        roller.getWeblogManager().saveWeblog(cat.getWeblog());

    }



    public void moveWeblogCategoryContents(WeblogCategory srcCat,
                                           WeblogCategory destCat)
            throws WebloggerException {

        // get all entries in category and subcats
        List<WeblogEntry> results = srcCat.retrieveWeblogEntries(false);

        // Loop through entries in src cat, assign them to dest cat
        Weblog website = destCat.getWeblog();
        for (WeblogEntry entry : results) {
            entry.setCategory(destCat);
            entry.setWebsite(website);
            this.strategy.store(entry);
        }

        // Update Blogger API category if applicable
        WeblogCategory bloggerCategory = srcCat.getWeblog().getBloggerCategory();
        if (bloggerCategory != null && bloggerCategory.getId().equals(srcCat.getId())) {
            srcCat.getWeblog().setBloggerCategory(destCat);
            this.strategy.store(srcCat.getWeblog());

        }
    }

    public boolean isWeblogCategoryInUse(WeblogCategory cat)
            throws WebloggerException {
        if (cat.getWeblog().getBloggerCategory().equals(cat)) {
            return true;
        }
        TypedQuery<WeblogEntry> q = strategy.getNamedQuery("WeblogEntry.getByCategory", WeblogEntry.class);
        q.setParameter(1, cat);
        int entryCount = q.getResultList().size();
        return entryCount > 0;
    }

}
