package com.rackspace.feeds.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.transform.*;
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
                          final FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        ServletResponsePipe srp = new ServletResponsePipe(httpServletResponse);
        srp.doFilterAsynch(chain, httpServletRequest);

        // prepare the real response here
        response.setContentType(httpServletResponse.getContentType());

        Map<String, Object> xsltParameters = new HashMap<String, Object>();
        try {
            TransformerUtils transformer = new TransformerUtils();
            transformer.doTransform(getXsltStreamSource(),
                    xsltParameters,
                    new StreamSource(srp.getInputStream()),
                    new StreamResult(httpServletResponse.getWriter()));
        } catch(TransformerException te) {
            throw new ServletException(te);
        }

    }

    /**
     * The counterpart to the init( ) method.
     */
    public void destroy( ) {

    }

    protected StreamSource getXsltStreamSource() {
        return new StreamSource(new File(this.xsltFileName));
    }
}
