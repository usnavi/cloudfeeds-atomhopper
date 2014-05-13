package com.rackspace.feeds.adapter;

import org.atomhopper.jdbc.adapter.AbstractJdbcFeedSource;
import org.atomhopper.jdbc.model.PersistedEntry;
import org.atomhopper.jdbc.query.SearchToSqlConverter;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Implements the AbstractJdbcFeedSource to retrieve all categories with "tid:" from the "tenantid"
 * text column & all atom categories prefixed with "type:" from the "eventtype" column.
 */
public class CloudFeedsFeedSource extends AbstractJdbcFeedSource {

    private Map<String, String> mapPrefix;
    private Map<String, String> mapColumn;

    private String split;

    public void setPrefixColumnMap( Map<String, String> prefix ) {

        mapPrefix = new HashMap<String, String>( prefix );

        mapColumn = new HashMap<String, String>();

        for( String key : mapPrefix.keySet() ) {

            mapColumn.put( mapPrefix.get( key ), key );
        }
    }

    public void setDelimiter( String splitParam ) {

        split = splitParam;
    }

    @Override
    protected SearchToSqlConverter getSearchToSqlConverter() {

        return new SearchToSqlConverter( mapPrefix, split );
    }

    @Override
    protected RowMapper getRowMapper() {

        return new EntryRowMapper();
    }


    class EntryRowMapper implements RowMapper {

        @Override
        public Object mapRow( ResultSet rs, int rowNum ) throws SQLException {
            EntryResultSetExtractor extractor = new EntryResultSetExtractor();
            return extractor.extractData(rs);
        }
    }

    class EntryResultSetExtractor implements ResultSetExtractor {


        @Override
        public Object extractData( ResultSet rs ) throws SQLException, DataAccessException {

            PersistedEntry entry = new PersistedEntry();
            entry.setId(rs.getLong("id"));
            entry.setFeed(rs.getString("feed"));
            entry.setCreationDate(rs.getTimestamp("creationdate"));
            entry.setDateLastUpdated(rs.getTimestamp("datelastupdated"));
            entry.setEntryBody(rs.getString("entrybody"));
            entry.setEntryId(rs.getString("entryid"));


            List<String> cats = new ArrayList<String>( Arrays.asList( (String[])rs.getArray( "categories" ).getArray() ) );

            for( String column : mapColumn.keySet() ) {

                if( rs.getString( column) != null ) {

                    cats.add( mapColumn.get( column) + split + rs.getString( column) );
                }
            }

            entry.setCategories( cats.toArray( new String[ 0 ]) );
            return entry;
        }
    }
}
