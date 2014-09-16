package com.rackspace.feeds.filter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Map;

/**
 * User: shin4590
 * Date: 9/10/14
 */
public class TransformerUtils {

    static Logger LOG = LoggerFactory.getLogger(TransformerUtils.class);

    static final TransformerFactory transformerFactory =
            TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);

    public void doTransform(HttpServletRequest wrappedRequest,
                               HttpServletResponse wrappedResponse,
                               HttpServletResponse originalResponse,
                               FilterChain chain,
                               StreamSource xsltStream,
                               Map<String, Object> xsltParameters)
            throws IOException, ServletException {


        ServletResponsePipe srp = new ServletResponsePipe(wrappedRequest, wrappedResponse);
        srp.doFilterAsynch(chain);

        int status = wrappedResponse.getStatus();

        // If it's not 20x, then we may not get an XML or even
        // a response body. So we only check for success code.
        if ( status >= 200 && status <300 ) {

            try {
                TransformerUtils transformer = new TransformerUtils();

                // This is because we can't deterministically get response Content-Type
                // from HttpServletResponse until we actually read it. Instead of
                // relying on the Content-Type, we can look ahead 1 byte into the
                // input stream. If it is '<', then we take our chances and pass it
                // down to XSLT.
                BufferedInputStream bis = new BufferedInputStream(srp.getInputStream());
                bis.mark(1);
                int char1 = bis.read();
                bis.reset();
                if ( char1 == '<') {
                    transformer.doTransform(xsltStream,
                                            xsltParameters,
                                            new StreamSource(bis),
                                            new StreamResult(originalResponse.getWriter()));
                } else {
                    // the input is not XML
                    IOUtils.copy(bis, originalResponse.getWriter());
                }
            } catch(TransformerException te) {
                String contentType = srp.getContentType();
                if ( contentType.contains("application/xml") ||
                     contentType.contains("application/atom+xml") ) {
                    throw new ServletException(te);
                } else {
                    srp.getInputStream().reset();
                    IOUtils.copy(srp.getInputStream(), originalResponse.getOutputStream());
                }
            }
        } else {
            // copy input to output as is
            LOG.debug("Response has status=" + status);
            IOUtils.copy(srp.getInputStream(), originalResponse.getOutputStream());
        }
    }

    /**
     * Utility method to make it easier for people who wants to transform an 'inputXml'
     * using an 'xslt' stylesheet and writes it to 'result'.
     *
     * @param xslt            the transformation stylesheet
     * @param xsltParameters  the parameters to the xslt
     * @param inputXml        the XML to be transformed
     * @param result         the resulting transformed output
     * @throws IOException
     * @throws TransformerException
     */
    protected void doTransform(Source xslt, Map<String, Object> xsltParameters, Source inputXml, Result result)
            throws IOException, TransformerException {

        Transformer trans = transformerFactory.newTransformer(xslt);

        // set transformer parameters, if any
        if ( xsltParameters != null && !xsltParameters.isEmpty() ) {
            for (String key: xsltParameters.keySet()) {
                trans.setParameter(key, xsltParameters.get(key));
            }
        }

        trans.transform(inputXml, result);
    }
}
