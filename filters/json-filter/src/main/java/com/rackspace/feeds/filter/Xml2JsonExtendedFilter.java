package com.rackspace.feeds.filter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Collections;

/**
 * This filter extends the Xml2JsonFilter and modify the behavior to not use piped async call,
 * and to set the contentLength of the transformed response.
 *
 * It requires a Filter input parameter called 'xsltFile' which is the full path to the XSLT file to perform the
 * transformation.
 */
public class Xml2JsonExtendedFilter extends Xml2JsonFilter {
    private static Logger LOG = LoggerFactory.getLogger(Xml2JsonExtendedFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         final FilterChain chain)
            throws java.io.IOException, ServletException {

        LOG.debug( "Xml2JsonExtendedFilter doFilter()" );
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        if( jsonPreferred(httpServletRequest) ) {

            //create wrapper response with output stream to collect transformed content
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ServletOutputStreamWrapper outputStreamWrapper = new ServletOutputStreamWrapper(stream);
            OutputStreamResponseWrapper wrappedResponse =
                    new OutputStreamResponseWrapper(httpServletResponse, outputStreamWrapper);

            // apply filter further down the chain on wrapped response
            chain.doFilter(httpServletRequest, wrappedResponse);

            // obtain response content
            String originalResponseContent = stream.toString();
            LOG.debug("Original response content length = " + originalResponseContent.length());

            if (StringUtils.isNotEmpty(originalResponseContent)) {

                try {
                    OutputStream outputStream = new ByteArrayOutputStream();

                    // transform response content with the xml to json xslt
                    transformer.doTransform(Collections.EMPTY_MAP,
                            new StreamSource(new StringReader(originalResponseContent)),
                            new StreamResult(outputStream));

                    String jsonResponseContent = outputStream.toString();
                    LOG.debug("New response content length = " + jsonResponseContent.length());

                    // set new json response wrapper with the transformed json content
                    JsonResponseBodyWrapper jsonBodyWrapper = new JsonResponseBodyWrapper( httpServletResponse );
                    jsonBodyWrapper.setContentLength(jsonResponseContent.length());
                    jsonBodyWrapper.setContentType(RAX_SVC_JSON_MEDIA_TYPE);
                    jsonBodyWrapper.getWriter().write(jsonResponseContent);
                }
                catch (Exception e) {
                    LOG.error("Error transforming xml: " + e.getMessage());
                }
                finally {
                    httpServletResponse.getWriter().close();
                }
            }
        }
        else {
            chain.doFilter( servletRequest, servletResponse );
        }
    }
}
