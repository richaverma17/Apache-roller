package org.apache.roller.weblogger.business.jpa;

import jakarta.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.MediaFile;
import org.apache.roller.weblogger.pojos.MediaFileFilter;
import org.apache.roller.weblogger.pojos.MediaFileType;
import org.apache.roller.weblogger.pojos.Weblog;

import java.util.ArrayList;
import java.util.List;

public class MediaFileQueryBuilder {
    private final JPAPersistenceStrategy strategy;
    private final StringBuilder whereClause = new StringBuilder();
    private final StringBuilder orderBy = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private int paramIndex = 0;

    public MediaFileQueryBuilder(JPAPersistenceStrategy strategy) {
        this.strategy = strategy;
    }

    // 1. Weblog filter method
    public MediaFileQueryBuilder forWeblog(Weblog weblog) {
        params.add(paramIndex++, weblog);
        whereClause.append("m.directory.weblog = ?").append(paramIndex);
        return this;
    }

    // 2. Name filter method
    public MediaFileQueryBuilder withNameFilter(String name) {
        if (StringUtils.isEmpty(name)) {
            return this;
        }

        String nameFilter = name.trim();
        if (!nameFilter.endsWith("%")) {
            nameFilter = nameFilter + "%";
        }

        params.add(paramIndex++, nameFilter);
        whereClause.append(" AND m.name like ?").append(paramIndex);
        return this;
    }

    // 3. Size filter method
    public MediaFileQueryBuilder withSizeFilter(long size, MediaFileFilter.SizeFilterType filterType) {
        if (size <= 0) {
            return this;
        }

        params.add(paramIndex++, size);
        whereClause.append(" AND m.length ");

        switch (filterType) {
            case GT:  whereClause.append(">"); break;
            case GTE: whereClause.append(">="); break;
            case LT:  whereClause.append("<"); break;
            case LTE: whereClause.append("<="); break;
            default:  whereClause.append("="); break;
        }

        whereClause.append(" ?").append(paramIndex);
        return this;
    }

    // 4. Tags filter method (THIS WAS MISSING!)
    public MediaFileQueryBuilder withTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return this;
        }

        if (tags.size() == 1) {
            // Single tag
            params.add(paramIndex++, tags.get(0));
            whereClause.append(" AND EXISTS (SELECT t FROM MediaFileTag t ")
                    .append("WHERE t.mediaFile = m and t.name = ?")
                    .append(paramIndex).append(")");
        } else {
            // Multiple tags
            whereClause.append(" AND EXISTS (SELECT t FROM MediaFileTag t ")
                    .append("WHERE t.mediaFile = m and t.name IN (");

            for (String tag : tags) {
                params.add(paramIndex++, tag);
                whereClause.append("?").append(paramIndex).append(",");
            }

            // Remove last comma
            whereClause.deleteCharAt(whereClause.lastIndexOf(","));
            whereClause.append("))");
        }

        return this;
    }

    // 5. Type filter method (THIS WAS MISSING!)
    public MediaFileQueryBuilder withTypeFilter(MediaFileType type) {
        if (type == null) {
            return this;
        }

        if (type == MediaFileType.OTHERS) {
            // For "OTHERS", exclude all known types
            for (MediaFileType knownType : MediaFileType.values()) {
                if (knownType != MediaFileType.OTHERS) {
                    params.add(paramIndex++, knownType.getContentTypePrefix() + "%");
                    whereClause.append(" AND m.contentType not like ?").append(paramIndex);
                }
            }
        } else {
            // For specific type
            params.add(paramIndex++, type.getContentTypePrefix() + "%");
            whereClause.append(" AND m.contentType like ?").append(paramIndex);
        }

        return this;
    }

    // 6. Order by method (THIS WAS MISSING!)
    public MediaFileQueryBuilder orderBy(MediaFileFilter.MediaFileOrder order) {
        if (order == null) {
            orderBy.append(" order by m.name");
            return this;
        }

        switch (order) {
            case NAME:
                orderBy.append(" order by m.name");
                break;
            case DATE_UPLOADED:
                orderBy.append(" order by m.dateUploaded");
                break;
            case TYPE:
                orderBy.append(" order by m.contentType");
                break;
            default:
                orderBy.append(" order by m.name");
                break;
        }

        return this;
    }

    // 7. Build method
    public TypedQuery<MediaFile> build(int startIndex, int length) throws WebloggerException{
        String fullQuery = "SELECT m FROM MediaFile m WHERE " +
                whereClause.toString() +
                orderBy.toString();

        TypedQuery<MediaFile> query = strategy.getDynamicQuery(fullQuery, MediaFile.class);

        // Set all parameters
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        // Set pagination
        if (startIndex >= 0) {
            query.setFirstResult(startIndex);
            query.setMaxResults(length);
        }

        return query;
    }
}