package com.rackspace.feeds.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * This filter operates on the XML body of responses from cloud feeds.  It uses XSLT provide by the usage-schema rpm
 * (standard-usage-schemas) to filter out private attributes from the responses.
 *
 * It requires a Filter input parameter called 'xsltFile' which is the full path to the XSLT file to perform the
 * transformation.
 *
 * If the the response header 'x-roles' does not contain the role 'cloudfeeds:service-admin' then the the XSLT
 * in the 'xsltFile' param is executed on the response.  Otherwise, the response is not modified.
 */
public class PrivateAttrsFilter implements Filter {

    public static String X_ROLES = "x-roles";
    public static String CF_ADMIN = "cloudfeeds:service-admin";

    private static Logger LOG = LoggerFactory.getLogger( PrivateAttrsFilter.class );

    private XSLTTransformerUtil xsltTransformerUtil;

    public void  init(FilterConfig config)
            throws ServletException {
        LOG.debug("initializing PrivateAttrsFilter");

        String xsltFilePath = config.getInitParameter("xsltFile");

        if ( xsltFilePath == null ) {
            throw new ServletException("xsltFile parameter is required for this filter");
        }
        xsltTransformerUtil = XSLTTransformerUtil.getInstanceForExternalFile(xsltFilePath);
    }

    public void  doFilter(ServletRequest servletRequest,
                          ServletResponse servletResponse,
                          final FilterChain chain)
            throws java.io.IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;


        ResponseBodyWrapper wrapper = new ResponseBodyWrapper( response );

        Enumeration<String> roleEnum = request.getHeaders( X_ROLES );

        Set<String> roles = new HashSet<String>( Collections.list( roleEnum ) );

        if( !roles.contains( CF_ADMIN ) ) {

            TransformerUtils transformer = new TransformerUtils(xsltTransformerUtil);

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

    static class ResponseBodyWrapper extends HttpServletResponseWrapper {

        private StringWriter writer = new StringWriter();

        public ResponseBodyWrapper(HttpServletResponse response){
            super(response);
        }

        public String toString() {

            return writer.toString();

        }

        public PrintWriter getWriter(){

            return new PrintWriter( writer );

        }
    }
}
