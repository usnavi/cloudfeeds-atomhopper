package com.rackspace.feeds.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

/**
 *
 */
public class XsltFilter implements Filter {

    static Logger LOG = LoggerFactory.getLogger(XsltFilter.class);
    static final TransformerFactory transformerFactory =
                      TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl",null);

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

        if (!(response instanceof HttpServletResponse)) {
            throw new ServletException("This filter only supports HTTP");
        }

        try {
            // do the XSLT transformation
            CharResponseWrapper wrappedResponse =
                    new CharResponseWrapper((HttpServletResponse)response);
            response.setContentType(wrappedResponse.getContentType());

            chain.doFilter(request, wrappedResponse);
            Source stream = new StreamSource(new StringReader(wrappedResponse.toString()));
            Transformer trans = transformerFactory.newTransformer(
                    new StreamSource(new File(this.xsltFileName)));

            CharArrayWriter caw = new CharArrayWriter();
            StreamResult result = new StreamResult(caw);
            trans.transform(stream, result);
            response.setContentLength(caw.toString().length());
            PrintWriter out = response.getWriter();
            out.write(caw.toString());
        } catch (TransformerException te) {
            LOG.error("Got transformer exception: ", te);
            throw new ServletException(te);
        }
    }

    /**
     * The counterpart to the init( ) method.
     */
    public void destroy( ) {

    }
}
