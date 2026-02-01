package org.apache.roller.weblogger.business.jpa;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.TypedQuery;

import com.google.inject.Inject;
import java.util.Collections;


import org.apache.commons.lang3.StringUtils;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntry.PubStatus;
import org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria;


public class WeblogEntryRepository{
    private final JPAPersistenceStrategy strategy;

    @Inject
    public WeblogEntryRepository(JPAPersistenceStrategy strategy) {
        this.strategy = strategy;
    }
    
        private WeblogCategory getWeblogCategoryByName (
            org.apache.roller.weblogger.pojos.Weblog weblog,
            String categoryName) throws WebloggerException{

        TypedQuery<WeblogCategory> q = strategy.getNamedQuery(
                "WeblogCategory.getByWeblog&Name", WeblogCategory.class);
        q.setParameter(1, weblog);
        q.setParameter(2, categoryName);

        try {
            return q.getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }


    public List<WeblogEntry> getWeblogEntries(WeblogEntrySearchCriteria wesc)
            throws WebloggerException {

        WeblogCategory cat = null;
        if (StringUtils.isNotEmpty(wesc.getCatName()) && wesc.getWeblog() != null) {
            cat = getWeblogCategoryByName(
    wesc.getWeblog(), wesc.getCatName());


        }

        List<Object> params = new ArrayList<>();
        int size = 0;
        StringBuilder queryString = new StringBuilder();

        if (wesc.getTags() == null || wesc.getTags().isEmpty()) {
            queryString.append("SELECT e FROM WeblogEntry e WHERE ");
        } else {
            queryString.append("SELECT e FROM WeblogEntry e JOIN e.tags t WHERE (");
            for (int i = 0; i < wesc.getTags().size(); i++) {
                if (i != 0) queryString.append(" OR ");
                params.add(size++, wesc.getTags().get(i));
                queryString.append(" t.name = ?").append(size);
            }
            queryString.append(") AND ");
        }

        if (wesc.getWeblog() != null) {
            params.add(size++, wesc.getWeblog().getId());
            queryString.append("e.website.id = ?").append(size);
        } else {
            params.add(size++, Boolean.TRUE);
            queryString.append("e.website.visible = ?").append(size);
        }

        if (wesc.getUser() != null) {
            params.add(size++, wesc.getUser().getUserName());
            queryString.append(" AND e.creatorUserName = ?").append(size);
        }

        if (wesc.getStartDate() != null) {
            Timestamp start = new Timestamp(wesc.getStartDate().getTime());
            params.add(size++, start);
            queryString.append(" AND e.pubTime >= ?").append(size);
        }

        if (wesc.getEndDate() != null) {
            Timestamp end = new Timestamp(wesc.getEndDate().getTime());
            params.add(size++, end);
            queryString.append(" AND e.pubTime <= ?").append(size);
        }

        if (cat != null) {
            params.add(size++, cat.getId());
            queryString.append(" AND e.category.id = ?").append(size);
        }

        if (wesc.getStatus() != null) {
            params.add(size++, wesc.getStatus());
            queryString.append(" AND e.status = ?").append(size);
        }

        if (wesc.getLocale() != null) {
            params.add(size++, wesc.getLocale() + '%');
            queryString.append(" AND e.locale like ?").append(size);
        }

        if (StringUtils.isNotEmpty(wesc.getText())) {
            params.add(size++, '%' + wesc.getText() + '%');
            queryString.append(
                " AND ( e.text LIKE ?" + size +
                " OR e.summary LIKE ?" + size +
                " OR e.title LIKE ?" + size + " ) ");
        }

        if (WeblogEntrySearchCriteria.SortBy.UPDATE_TIME.equals(wesc.getSortBy())) {
            queryString.append(" ORDER BY e.updateTime ");
        } else {
            queryString.append(" ORDER BY e.pubTime ");
        }

        if (WeblogEntrySearchCriteria.SortOrder.ASCENDING.equals(wesc.getSortOrder())) {
            queryString.append("ASC ");
        } else {
            queryString.append("DESC ");
        }

        TypedQuery<WeblogEntry> query =
                strategy.getDynamicQuery(queryString.toString(), WeblogEntry.class);

        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        if (wesc.getOffset() != 0) query.setFirstResult(wesc.getOffset());
        if (wesc.getMaxResults() != -1) query.setMaxResults(wesc.getMaxResults());

        return query.getResultList();
    }
    public List<WeblogEntry> getNextPrevEntries(WeblogEntry current, String catName,
            String locale, int maxEntries, boolean next)
            throws WebloggerException {

		if (current == null) {
			return Collections.emptyList();
		}

        TypedQuery<WeblogEntry> query;
        WeblogCategory category;
        
        List<Object> params = new ArrayList<>();
        int size = 0;
        String queryString = "SELECT e FROM WeblogEntry e WHERE ";
        StringBuilder whereClause = new StringBuilder();

        params.add(size++, current.getWebsite());
        whereClause.append("e.website = ?").append(size);
        
        params.add(size++, PubStatus.PUBLISHED);
        whereClause.append(" AND e.status = ?").append(size);
                
        if (next) {
            params.add(size++, current.getPubTime());
            whereClause.append(" AND e.pubTime > ?").append(size);
        } else {
            // pub time null if current article not yet published, in Draft view
            if (current.getPubTime() != null) {
                params.add(size++, current.getPubTime());
                whereClause.append(" AND e.pubTime < ?").append(size);
            }
        }
        
        if (catName != null) {
            category = getWeblogCategoryByName(current.getWebsite(), catName);
            if (category != null) {
                params.add(size++, category);
                whereClause.append(" AND e.category = ?").append(size);
            } else {
                throw new WebloggerException("Cannot find category: " + catName);
            } 
        }
        
        if(locale != null) {
            params.add(size++, locale + '%');
            whereClause.append(" AND e.locale like ?").append(size);
        }
        
        if (next) {
            whereClause.append(" ORDER BY e.pubTime ASC");
        } else {
            whereClause.append(" ORDER BY e.pubTime DESC");
        }
        query = strategy.getDynamicQuery(queryString + whereClause.toString(), WeblogEntry.class);
        for (int i=0; i<params.size(); i++) {
            query.setParameter(i+1, params.get(i));
        }
        query.setMaxResults(maxEntries);
        
        return query.getResultList();
    }
    public List<WeblogEntry> getWeblogEntriesPinnedToMain(Integer max)
    throws WebloggerException {
        TypedQuery<WeblogEntry> query = strategy.getNamedQuery(
                "WeblogEntry.getByPinnedToMain&statusOrderByPubTimeDesc", WeblogEntry.class);
        query.setParameter(1, Boolean.TRUE);
        query.setParameter(2, PubStatus.PUBLISHED);
        if (max != null) {
            query.setMaxResults(max);
        }
        return query.getResultList();
    }
    

};