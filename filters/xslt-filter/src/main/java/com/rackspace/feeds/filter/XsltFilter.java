package com.rackspace.feeds.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 */
public class XsltFilter implements Filter {

    static Logger LOG = LoggerFactory.getLogger(XsltFilter.class);

    private XSLTTransformerUtil xsltTransformerUtil;

    /**
     * This method is called once when the filter is first loaded.
     */
    public void init(FilterConfig filterConfig) throws ServletException {

        // xsltPath should be something like "/WEB-INF/xslt/a.xslt"
        String xsltPath = filterConfig.getInitParameter("xsltPath");
        if (xsltPath == null) {
            throw new UnavailableException(
                         "xsltPath is a required parameter. Please "
                         + "check the deployment descriptor.");
        }

        // convert the context-relative path to a physical path name
        String xsltFileName = filterConfig.getServletContext( )
                                  .getRealPath(xsltPath);

        xsltTransformerUtil = XSLTTransformerUtil.getInstance(xsltPath);
    }

    public void doFilter (ServletRequest request, ServletResponse response,
                          FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        TransformerUtils transformerUtils = new TransformerUtils(xsltTransformerUtil);
        transformerUtils.doTransform(httpServletRequest, httpServletResponse, httpServletResponse,
                                     chain, new HashMap<String, Object>());

    }

    /**
     * The counterpart to the init( ) method.
     */
    public void destroy( ) {

    }

}
