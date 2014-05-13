package com.rackspace.feeds.adapter;

import org.atomhopper.jdbc.adapter.AbstractJdbcFeedPublisher;
import org.atomhopper.jdbc.model.PersistedEntry;
import org.atomhopper.jdbc.query.PostgreSQLTextArray;

import java.util.*;

/**
 * Implements the AbstractJdbcFeedSource to allow specific category prefixes to be saved to specific columns.
 *
 *
 *
 */
public class CloudFeedsFeedPublisher extends AbstractJdbcFeedPublisher {

    private Map<String, String> mapPrefix = new HashMap<String, String>();
    private Set<String> setBothSet = new HashSet<String>();

    private String split;

    public void setAsCategorySet( Set<String> set ) {

        setBothSet = new HashSet<String>( set );
    }

    public void setPrefixColumnMap( Map<String, String> prefix ) {

        mapPrefix = new HashMap<String, String>( prefix );
    }

    public void setDelimiter( String splitParam ) {

        split = splitParam;
    }

    @Override
    protected void insertDb( PersistedEntry persistedEntry ) {

        Categories categories = new Categories( persistedEntry.getCategories() );

        String insertSql1 = "INSERT INTO entries (entryid, entrybody, feed, categories";
        String insertSql2 = ") VALUES (?, ?, ?, ?";

        String sql = createSql( insertSql1, insertSql2 );

        List<Object> params = new ArrayList<Object>();
        params.add( persistedEntry.getEntryId() );
        params.add( persistedEntry.getEntryBody() );
        params.add( persistedEntry.getFeed() );
        params.add( new PostgreSQLTextArray( categories.getCategories() ) );

        for( String prefix : mapPrefix.keySet() ) {

            params.add( categories.getPrefix( prefix ) );
        }

        getJdbcTemplate().update( sql, params.toArray( new Object[0] ));
    }


    @Override
    protected void insertDbOverrideDate( PersistedEntry persistedEntry ) {

        String insertSql1 = "INSERT INTO entries (entryid, creationdate, datelastupdated, entrybody, feed, categories";
        String insertSql2 = ") VALUES (?, ?, ?, ?, ?, ?";

        String sql = createSql( insertSql1, insertSql2 );

        Categories categories = new Categories( persistedEntry.getCategories() );

        List<Object> params = new ArrayList<Object>();
        params.add( persistedEntry.getEntryId() );
        params.add( persistedEntry.getCreationDate() );
        params.add( persistedEntry.getDateLastUpdated() );
        params.add( persistedEntry.getEntryBody() );
        params.add( persistedEntry.getFeed() );
        params.add( new PostgreSQLTextArray( categories.getCategories() ) );

        for( String prefix : mapPrefix.keySet() ) {

            params.add( categories.getPrefix( prefix ) );
        }

        getJdbcTemplate().update(sql, params.toArray( new Object[0] ));
    }


    private String createSql( String insertSql1, String insertSql2 ) {
        String insertSqlEnd = ")";

        StringBuilder sbSql = new StringBuilder();
        sbSql.append( insertSql1 );

        for( String prefix : mapPrefix.keySet() ) {

            sbSql.append( ", " + mapPrefix.get( prefix ) );
        }

        sbSql.append( insertSql2 );

        for( int i = 0; i < mapPrefix.size(); i++ ) {

            sbSql.append( ", ?" );
        }

        sbSql.append( insertSqlEnd );
        return sbSql.toString();
    }

    class Categories {

        private String[] categories = new String[ 0 ];

        private Map<String, String> mapByPrefix = new HashMap<String, String>();

        public Categories( String[] cats ) {

            List<String> list = new ArrayList<String>();

            for( String cat : cats ) {

                boolean isPrefix = false;

                for( String prefix : mapPrefix.keySet() ) {

                    String prefixSplit =  prefix + split;

                    if( cat.startsWith( prefixSplit ) ) {

                        mapByPrefix.put( prefix, cat.substring( prefixSplit.length() ) );

                        // if we are setting both, we want it in the column as well as the generic categories array
                        if( !setBothSet.contains( prefix ) )
                            isPrefix = true;

                        break;
                    }
                }

                if( !isPrefix ) {
                    list.add( cat );
                }
            }

            categories = list.toArray( categories );
        }

        public String getPrefix( String prefix ) {

            return mapByPrefix.get( prefix );
        }

        public String[] getCategories() {
            return categories;
        }

    }
}
