package com.rackspace.feeds.filter;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * This class is meant to be used from within a Servlet Filter, is <b>not</b>
 * thread safe, and a single instance of this is meant to be used for one
 * pair of servlet request and response.
 *
 * Its main goal is to set up a pipe between the output stream of a servlet
 * response to an input stream. The pipe's input stream is then used to
 * transform the response body.
 *
 * This class performs the following:
 * <ul>
 *     <li>Calls the rest of the FilterChain, using the ServletResponsePipe class</li>
 *     <li>If the status code is between 200 and 300 & response contains XML, transforms the response from the
 *     wrappedResponse using the given XSLT, and then writes
 *     the response to the originalResponse</li>
 * </ul>
 *
 * User: shin4590
 * Date: 9/10/14
 */
public class TransformerUtils {

    static Logger LOG = LoggerFactory.getLogger(TransformerUtils.class);

    private final XSLTTransformerUtil xsltTransformerUtil;

    public TransformerUtils(XSLTTransformerUtil xsltTransformerUtil) {
        this.xsltTransformerUtil = xsltTransformerUtil;
    }

    public void doTransform(HttpServletRequest wrappedRequest,
                            HttpServletResponse wrappedResponse,
                            HttpServletResponse originalResponse,
                            FilterChain chain,
                            Map<String, Object> xsltParameters)
            throws IOException, ServletException {

        ServletResponsePipe srp = new ServletResponsePipe(wrappedRequest, wrappedResponse);
        srp.doFilterAsynch(chain);

        try {
            InputStream bis = new BufferedInputStream(srp.getInputStream());
            int firstByte = getFirstByte(bis);
            int status = wrappedResponse.getStatus();

            // This is because we can't deterministically get Response's Content-Type
            // from HttpServletResponse until we actually read it. Instead of
            // relying on the Content-Type, we can look ahead 1 byte into the
            // input stream. If it is '<', then we take our chances and pass it
            // down to XSLT.
            if ( firstByte == '<' &&  (status >= 200 && status <300)) {
                xsltTransformerUtil.doTransform(xsltParameters,
                        new StreamSource(bis),
                        new StreamResult(originalResponse.getWriter()));
            } else {
                // the input is not XML
                LOG.debug("Skipping transform cuz input stream starts with '" + firstByte + "', does not look to be XML or Response has status=" + wrappedResponse.getStatus());
                IOUtils.copy(bis, originalResponse.getOutputStream());
            }
        } catch(TransformerException te) {
            throw new ServletException(te);
        }
    }

    /**
     * Reads input stream and returns the first byte as integer. After reading it
     * repositions the stream to the beginning.
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    private int getFirstByte(InputStream inputStream) throws IOException {

        int firstByte = -1;

        try {
            inputStream.mark(1);
            firstByte = inputStream.read();
        } finally {
            inputStream.reset();
        }

        return firstByte;
    }
}
