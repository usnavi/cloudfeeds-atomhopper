package com.rackspace.feeds.filter;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

/**
 * Object pool factory which pools instances of javax.xml.transform.Transformer
 *
 * @param <Transformer>
 */
public class XSLTTransformerPooledObjectFactory<Transformer> extends BasePooledObjectFactory<Transformer> {

    static final TransformerFactory transformerFactory =
            TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);

    private final String xsltAsString;

    public XSLTTransformerPooledObjectFactory(String xsltAsString) {
        this.xsltAsString = xsltAsString;
    }

    @Override
    public Transformer create() throws IllegalArgumentException, TransformerConfigurationException {
        synchronized ( this ) {
            return (Transformer) transformerFactory.newTransformer(new StreamSource(new StringReader(xsltAsString)));
        }
    }

    @Override
    public PooledObject<Transformer> wrap( Transformer transformer ) {
        return new DefaultPooledObject<Transformer>( transformer );
    }

}