package com.rackspace.feeds.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
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
    public void doTransform(Source xslt, Map<String, Object> xsltParameters, Source inputXml, Result result)
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
