package com.rackspace.feeds.filter;

import net.sf.saxon.Controller;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.lang.StringUtils;
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
    private String initialTemplate = null;
    private String systemId = null;

    public XSLTTransformerPooledObjectFactory(String xsltAsString, String initialTemplateP, String systemId ) {
        this.xsltAsString = xsltAsString;
        initialTemplate = initialTemplateP;
        this.systemId = systemId;
    }

    @Override
    public Transformer create() throws IllegalArgumentException, TransformerConfigurationException, XPathException {
        synchronized ( this ) {
            StreamSource source;
            if (StringUtils.isNotBlank(systemId) ) {
                source = new StreamSource(new StringReader(xsltAsString), systemId);
            } else {
                source = new StreamSource(new StringReader(xsltAsString));
            }
            Transformer transformer = (Transformer) transformerFactory.newTransformer(source);

            if( StringUtils.isNotBlank(initialTemplate) )
                ((Controller)transformer).setInitialTemplate( initialTemplate );

            return transformer;
        }
    }

    @Override
    public PooledObject<Transformer> wrap( Transformer transformer ) {
        return new DefaultPooledObject<Transformer>( transformer );
    }

}