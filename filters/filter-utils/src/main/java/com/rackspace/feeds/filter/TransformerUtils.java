package com.rackspace.feeds.filter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
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
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Map;

/**
 * This class is meant to be used from within a Servlet Filter, a single instance of
 * this class should be used across requests for better performance.
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
 *     the response to the originalResponse. It leverages pooling of the xslt
 *     transformation objects</li>
 * </ul>
 *
 * User: shin4590
 * Date: 9/10/14
 */
public class TransformerUtils {

    static Logger LOG = LoggerFactory.getLogger(TransformerUtils.class);

    private final ObjectPool<Transformer> transformerPool;
    private final String xsltPath;

    static private GenericObjectPoolConfig CONFIG = new GenericObjectPoolConfig();

    static {

        CONFIG.setMinIdle( 2 );
        // We're using this value to initialize the pool
        // To evict idle instances, we'll need to set timeBetweenEvictionRunsMillis, but since things are working
        // fine, we won't mess with it for the time being
    }


    /**
     * Creates TransformerUtils instance to work with xsltPaths which are present in
     * classpath.
     *
     * @param xsltPath
     * @return
     */
    public static TransformerUtils getInstanceForXsltAsResource(String xsltPath) throws Exception {
        return new TransformerUtils(xsltPath, getXsltResourceAsString(xsltPath), null, null, CONFIG);
    }

    public static TransformerUtils getInstanceForXsltAsResource(String xsltPath, String initialTemplate ) throws Exception {
        return new TransformerUtils(xsltPath, getXsltResourceAsString(xsltPath), initialTemplate, null, CONFIG);
    }

    /**
     * Creates TransformerUtils instance to work with xsltPaths which are external.
     *
     * @param xsltPath
     * @return
     */
    public static TransformerUtils getInstanceForXsltAsFile(String xsltPath) throws Exception {
        return getInstanceForXsltAsFile( xsltPath, null );
    }

    public static TransformerUtils getInstanceForXsltAsFile(String xsltPath, String initialTemplate ) throws Exception {

        if ( StringUtils.isEmpty(xsltPath) ) {
            throw new IllegalArgumentException("xsltPath servlet filter parameter should not be empty or null");
        }
        try {
            File xsltFile = new File(xsltPath);
            String systemId = xsltFile.toURI().toURL().toExternalForm();
            return new TransformerUtils(xsltPath, getXsltFileAsString(xsltPath), initialTemplate, systemId, CONFIG );
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("File " + xsltPath + " can not be converted to URL", ex);
        }
    }

    private TransformerUtils(String xsltPath, String xsltAsString, String initialTemplate, String systemId, GenericObjectPoolConfig config ) throws Exception {
        this.xsltPath = xsltPath;
        this.transformerPool = new GenericObjectPool<Transformer>(new XSLTTransformerPooledObjectFactory<Transformer>(xsltAsString, initialTemplate, systemId), config );

        // The object pool doesn't actually initialize the pool, so we do it manually
        for( int i = 0; i < config.getMinIdle(); i++ )
            transformerPool.addObject();
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
            if ( firstByte == '<' &&  (status >= 200 && status <500)) {
                doTransform(xsltParameters,
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

    /**
     * Utility method to make it easier for people who wants to transform an 'inputXml'
     * using an 'xslt' stylesheet and writes it to 'result'.
     *
     * @param xsltParameters  the parameters to the xslt
     * @param inputXml        the XML to be transformed
     * @param result         the resulting transformed output
     * @throws java.io.IOException
     * @throws javax.xml.transform.TransformerException
     */
    public void doTransform(Map<String, Object> xsltParameters, Source inputXml, Result result)
            throws IOException, TransformerException {

        Transformer transformer = null;
        try {
            transformer = transformerPool.borrowObject();

            // set transformer parameters, if any
            if ( xsltParameters != null && !xsltParameters.isEmpty() ) {
                for (String key: xsltParameters.keySet()) {
                    transformer.setParameter(key, xsltParameters.get(key));
                }
            }

            transformer.transform(inputXml, result);
        } catch (Exception e) {
            LOG.error("Error transforming xml using xslt: " + xsltPath, e);
            throw new TransformerException(e);
        } finally {
            try {
                if (transformer != null) {
                    transformerPool.returnObject(transformer);
                }
            } catch (Exception e) {
                LOG.error("!!! Error returning xslt transformation object back to the pool. This would cause slowness. !!!", e);
            }
        }
    }


    /**
     * This methods converts the content present in the file path into a string.
     *
     * @param xsltPath file path containing the xslt
     * @return contents of the file as a string
     * @throws IllegalArgumentException for any problems reading the file or for empty content.
     */
    private static String getXsltResourceAsString(String xsltPath) throws IllegalArgumentException {

        if ( StringUtils.isBlank(xsltPath) ) {
            throw new IllegalArgumentException("Empty content in file:" + xsltPath);
        }

        InputStream is = null;
        String xsltStr;

        try {
            if (StringUtils.isEmpty(xsltPath)) {
                throw new IllegalArgumentException("Invalid xslt file:" + xsltPath);
            }

            is = TransformerUtils.class.getResourceAsStream(xsltPath);
            if ( is != null ) {
                xsltStr = IOUtils.toString(is);
            } else {
                throw new IllegalArgumentException("null input stream for " + xsltPath);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting xslt file to string:" + xsltPath, e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return xsltStr;
    }

    /**
     * This methods converts the content present in the external file path into a string.
     *
     * @param xsltPath file path containing the external file location of xslt
     * @return contents of the file as a string
     * @throws IllegalArgumentException for any problems reading the file or for empty content.
     */
    private static String getXsltFileAsString(String xsltPath) throws IllegalArgumentException {

        if (StringUtils.isEmpty(xsltPath)) {
            throw new IllegalArgumentException("Invalid xslt file:" + xsltPath);
        }

        InputStream is = null;
        String xsltStr;

        try {
            is = new BufferedInputStream(new FileInputStream(xsltPath));
            if ( is != null ) {
                xsltStr = IOUtils.toString(is);
            } else {
                throw new IllegalArgumentException("null input stream for " + xsltPath);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting xslt file to string:" + xsltPath, e);
        } finally {
            IOUtils.closeQuietly(is);
        }


        if ( StringUtils.isBlank(xsltStr) ) {
            throw new IllegalArgumentException("Empty content in file:" + xsltPath);
        }

        return xsltStr;
    }
}
