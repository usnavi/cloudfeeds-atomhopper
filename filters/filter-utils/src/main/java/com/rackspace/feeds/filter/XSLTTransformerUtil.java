package com.rackspace.feeds.filter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * This class does xslt transformation and is designed to leverage pooling of the xslt
 * transformation objects.
 *
 * Single instance of this class should be used across threads to get efficient performance.
 *
 */
public class XSLTTransformerUtil {

    private static Logger LOG = LoggerFactory.getLogger(XSLTTransformerUtil.class);

    private final ObjectPool<Transformer> transformerPool;
    private final String xsltPath;

    /**
     * Creates XSLTTransformerUtil instance to work with xsltPaths which are present in
     * classpath.
     *
     * @param xsltPath
     * @return
     */
    public static XSLTTransformerUtil getInstance(String xsltPath) {
        return new XSLTTransformerUtil(xsltPath, getXsltFileAsString(xsltPath));
    }

    /**
     * Creates XSLTTransformerUtil instance to work with xsltPaths which are external.
     *
     * @param xsltPath
     * @return
     */
    public static XSLTTransformerUtil getInstanceForExternalFile(String xsltPath) {
        return new XSLTTransformerUtil(xsltPath, getExternalXsltFileAsString(xsltPath));
    }

    private XSLTTransformerUtil(String xsltPath, String xsltAsString) {
        this.xsltPath = xsltPath;
        this.transformerPool = new GenericObjectPool<Transformer>(new XSLTTransformerPooledObjectFactory<Transformer>(xsltAsString));
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
            new TransformerException(e);
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
    private static String getXsltFileAsString(String xsltPath) throws IllegalArgumentException {

        if ( StringUtils.isBlank(xsltPath) ) {
            throw new IllegalArgumentException("Empty content in file:" + xsltPath);
        }

        InputStream is = null;
        String xsltStr;

        try {
            if (StringUtils.isEmpty(xsltPath)) {
                throw new IllegalArgumentException("Invalid xslt file:" + xsltPath);
            }

            is = XSLTTransformerUtil.class.getResourceAsStream(xsltPath);
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
    private static String getExternalXsltFileAsString(String xsltPath) throws IllegalArgumentException {

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
