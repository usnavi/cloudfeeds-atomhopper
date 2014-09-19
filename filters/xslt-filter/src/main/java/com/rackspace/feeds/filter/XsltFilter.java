package com.rackspace.feeds.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.transform.stream.*;

/**
 *
 */
public class XsltFilter implements Filter {

    static Logger LOG = LoggerFactory.getLogger(XsltFilter.class);

    private String xsltFileName;

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
        this.xsltFileName = filterConfig.getServletContext( )
                                  .getRealPath(xsltPath);

        // verify that the file exists
        if (this.xsltFileName == null ||
                !new File(this.xsltFileName).exists( )) {
            throw new UnavailableException(
                    "Unable to locate stylesheet: " + this.xsltFileName, 30);
        }
    }

    public void doFilter (ServletRequest request, ServletResponse response,
                          FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        TransformerUtils transformerUtils = new TransformerUtils();
        transformerUtils.doTransform(httpServletRequest, httpServletResponse, httpServletResponse,
                                     chain, getXsltStreamSource(), new HashMap<String, Object>());

    }

    protected StreamSource getXsltStreamSource() throws IOException {
        return new StreamSource(new File(this.xsltFileName));
    }

    /**
     * The counterpart to the init( ) method.
     */
    public void destroy( ) {

    }

}
