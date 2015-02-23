package com.rackspace.feeds.filter;

import org.apache.abdera.protocol.server.ProviderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
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
    public static final String RAX_SVC_JSON_MEDIA_TYPE = "application/vnd.rackspace.atomsvc+json";
    public static final String ATOM_XML_MEDIA_TYPE = "application/atom+xml";
    public static final String ATOM_SVC_XML_MEDIA_TYPE = "application/atomsvc+xml";
    public static final String XML_MEDIA_TYPE = "application/xml";
    public static final String JSON_MEDIA_TYPE = "application/json";
    public static final String CONTENT_TYPE_HEADER = "content-type";

    private static Logger LOG = LoggerFactory.getLogger( Xml2JsonFilter.class );

    private TransformerUtils transformer;

    /**
     * This flag controls whether or not this Xml2Json filter is supposed to trigger
     * when the Accept: header has the generic application/json media type.
     *
     * For the case of Cloud Feeds, we do not want *yet* to have this filter trigger
     * on application/json.
     *
     * For the case of Cloud Feeds Catalog, we do want to have this filter trigger
     * on application/json.
     */
    private boolean filterOnGenericJsonMediaType = false;

    public void init(FilterConfig config)
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

        String filterOnJsonStr = config.getInitParameter("filterOnGenericJsonMediaType");
        if ( StringUtils.isNotBlank(filterOnJsonStr) ) {
            filterOnGenericJsonMediaType = Boolean.parseBoolean(filterOnJsonStr);
        }
    }

    public void doFilter(ServletRequest servletRequest,
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

    TransformerUtils getTransformer() throws Exception {
        return transformer;
    }

    protected boolean isFilterOnGenericJsonMediaType() {
        return filterOnGenericJsonMediaType;
    }

    protected void setFilterOnGenericJsonMediaType(boolean flag) {
        filterOnGenericJsonMediaType = flag;
    }

    @Override
    public void destroy() {
      /* Called before the Filter instance is removed
      from service by the web container*/
    }

    static class JsonResponseBodyWrapper extends HttpServletResponseWrapper {

        public JsonResponseBodyWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setContentType(String contentType) {
            super.setContentType(swizzleContentType(contentType));
        }

        @Override
        public void setHeader(String name, String value) {
            if ( CONTENT_TYPE_HEADER.equalsIgnoreCase(name) ) {
                super.setHeader(name, swizzleContentType(value));
            } else {
                super.setHeader(name, value);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if ( CONTENT_TYPE_HEADER.equalsIgnoreCase(name) ) {
                super.addHeader(name, swizzleContentType(value));
            } else {
                super.addHeader(name, value);
            }
        }

        String swizzleContentType(String originalContentType) {
            if ( StringUtils.isNotBlank(originalContentType) && originalContentType.startsWith(ATOM_XML_MEDIA_TYPE) ) {
                return originalContentType.replace(ATOM_XML_MEDIA_TYPE, RAX_JSON_MEDIA_TYPE);
            } else if ( StringUtils.isNotBlank(originalContentType) && originalContentType.startsWith(XML_MEDIA_TYPE) ) {
                return originalContentType.replace(XML_MEDIA_TYPE, JSON_MEDIA_TYPE);
            } else if ( StringUtils.isNotBlank(originalContentType) && originalContentType.startsWith(ATOM_SVC_XML_MEDIA_TYPE) ) {
                return originalContentType.replace(ATOM_SVC_XML_MEDIA_TYPE, RAX_SVC_JSON_MEDIA_TYPE);
            } else {
                LOG.debug("Not swizzling original content-type: " + originalContentType);
                return originalContentType;
            }
        }
    }

    boolean jsonPreferred(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if ( StringUtils.isNotEmpty(accept) ) {
            String[] orderedAccept = ProviderHelper.orderByQ(accept);
            for ( String acceptHeader : orderedAccept ) {
                if ( acceptHeader.equals(ATOM_XML_MEDIA_TYPE) )  {
                    return false;
                } else if ( acceptHeader.equals(RAX_JSON_MEDIA_TYPE) || acceptHeader.equals(RAX_SVC_JSON_MEDIA_TYPE) ) {
                    return true;
                } else if ( filterOnGenericJsonMediaType && acceptHeader.contains(JSON_MEDIA_TYPE) ) {
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
