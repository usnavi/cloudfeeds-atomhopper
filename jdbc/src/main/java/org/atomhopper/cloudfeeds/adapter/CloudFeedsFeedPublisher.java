package org.atomhopper.cloudfeeds.adapter;

import org.atomhopper.jdbc.adapter.AbstractJdbcFeedPublisher;
import org.atomhopper.jdbc.model.PersistedEntry;
import org.atomhopper.jdbc.query.PostgreSQLTextArray;

import java.util.ArrayList;
import java.util.List;

import static org.atomhopper.cloudfeeds.adapter.CloudFeedsFeedSource.*;

/**
 * Implements the AbstractJdbcFeedSource to store all atom categories prefixed with "tid:" to store in the "tenantid"
 * text column & all atom categories prefixed with "type:" in the "eventtype" column.
 */
public class CloudFeedsFeedPublisher extends AbstractJdbcFeedPublisher {

    @Override
    protected void insertDb( PersistedEntry persistedEntry ) {

        String insertSQL = "INSERT INTO entries (entryid, entrybody, eventtype, tenantId, feed, categories) VALUES (?, ?, ?, ?, ?, ?)";

        Categories categories = new Categories( persistedEntry.getCategories() );

        Object[] params = new Object[]{
              persistedEntry.getEntryId(),
              persistedEntry.getEntryBody(),
              categories.getEventType(),
              categories.getTenantId(),
              persistedEntry.getFeed(),
              new PostgreSQLTextArray( categories.getCategories() )
        };
        getJdbcTemplate().update(insertSQL, params);
    }

    @Override
    protected void insertDbOverrideDate( PersistedEntry persistedEntry ) {

        String insertSQL = "INSERT INTO entries (entryid, creationdate, datelastupdated, entrybody, eventtype, tenantId, feed, categories) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Categories categories = new Categories( persistedEntry.getCategories() );

        Object[] params = new Object[]{
              persistedEntry.getEntryId(),
              persistedEntry.getCreationDate(),
              persistedEntry.getDateLastUpdated(),
              persistedEntry.getEntryBody(),
              categories.getEventType(),
              categories.getTenantId(),
              persistedEntry.getFeed(),
              new PostgreSQLTextArray( categories.getCategories() )
        };
        getJdbcTemplate().update(insertSQL, params);
    }

    class Categories {

        private String[] categories = new String[ 0 ];
        private String eventType;
        private String tenantId;

        private final String tidPrefix = TID + SPLIT;
        private final String typePrefix = TYPE + SPLIT;

        public Categories( String[] cats ) {

            List<String> list = new ArrayList<String>();

            for( String cat : cats ) {


                if( cat.startsWith( tidPrefix  ) ) {

                    tenantId = cat.substring( tidPrefix.length() );

                    // TID:  remove when enabling tid column search
                    list.add( cat );
                    //
                }
                else if ( cat.startsWith( typePrefix ) ) {

                    eventType = cat.substring( typePrefix.length() );
                }
                else {
                    list.add( cat );
                }
            }

            categories = list.toArray( categories );
        }


        public String[] getCategories() {
            return categories;
        }

        public String getEventType() {
            return eventType;
        }

        public String getTenantId() {
            return tenantId;
        }
    }
}
