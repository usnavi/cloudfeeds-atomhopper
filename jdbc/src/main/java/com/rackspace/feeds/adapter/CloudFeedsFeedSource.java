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


}
