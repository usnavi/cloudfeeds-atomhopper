package com.rackspace.feeds.filter;

import org.apache.abdera.protocol.server.ProviderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;

/**
 * This filter operates on the XML body of responses from cloud feeds.  It uses XSLT provide by the usage-schema rpm
 * (standard-usage-schemas) to convert XML response to JSON.
 *
 * It requires a Filter input parameter called 'xsltFile' which is the full path to the XSLT file to perform the
 * transformation.
 */
public class Xml2JsonFilter implements Filter {

    public static final String RAX_JSON_MEDIA_TYPE = "application/vnd.rackspace.atom+json";
    public static final String ATOM_XML_MEDIA_TYPE = "application/atom+xml";

    private static Logger LOG = LoggerFactory.getLogger( Xml2JsonFilter.class );

    private TransformerUtils transformer;

    public void  init(FilterConfig config)
            throws ServletException {
        LOG.debug( "initializing Xml2JsonFilter" );

        String xsltFilePath = config.getInitParameter( "xsltFile" );

        if ( xsltFilePath == null ) {
            throw new ServletException( "xsltFile parameter is required for this filter" );
        }
        try {
            transformer = TransformerUtils.getInstanceForXsltAsFile( xsltFilePath, "main" );
        } catch ( Exception e ) {
            LOG.error( "Error loading Xslt: " + xsltFilePath );
            throw new ServletException( e );
        }
    }

    public void  doFilter(ServletRequest servletRequest,
                          ServletResponse servletResponse,
                          final FilterChain chain)
            throws java.io.IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;

        if( jsonPreferred(request) ) {

            JsonResponseBodyWrapper wrapper = new JsonResponseBodyWrapper( response );
            transformer.doTransform(request,
                                    wrapper,
                                    response,
                                    chain,
                                    Collections.EMPTY_MAP );
        }
        else {
            chain.doFilter( servletRequest, servletResponse );
        }

    }

    @Override
    public void destroy() {
      /* Called before the Filter instance is removed
      from service by the web container*/
    }

    static class JsonResponseBodyWrapper extends HttpServletResponseWrapper {

        public JsonResponseBodyWrapper(HttpServletResponse response){
            super(response);
        }

        @Override
        public String getContentType() {
            // we have converted it to application/vnd.rackspace.atom+json
            return RAX_JSON_MEDIA_TYPE;
        }
    }

    boolean jsonPreferred(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if ( StringUtils.isNotEmpty(accept) ) {
            String[] orderedAccept = ProviderHelper.orderByQ(accept);
            System.out.println(Arrays.toString(orderedAccept));
            for ( String acceptHeader : orderedAccept ) {
                if ( acceptHeader.equals(ATOM_XML_MEDIA_TYPE) )  {
                    return false;
                } else if ( acceptHeader.equals(RAX_JSON_MEDIA_TYPE) ) {
                    return true;
                } else if ( acceptHeader.contains("json") ) {
                    // If it's json anything but it's not our vnd.rax.atom+json,
                    // return false immediately. It is not for this filter
                    // to process.
                    return false;
                }
            }
        }
        return false;
    }
}
