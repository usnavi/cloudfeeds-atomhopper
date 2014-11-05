package com.rackspace.feeds.filter

import org.apache.commons.pool2.ObjectPool
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import spock.lang.Specification
import spock.lang.Unroll

import javax.xml.transform.Transformer

class XSLTTransformerPooledObjectFactoryTest extends Specification {


    @Unroll
    def "should get a transformer object"() {

        when:
        def xsltAsString = TransformerUtils.getXsltResourceAsString("/samples/test.xsl" )
        ObjectPool<Transformer> transformerPool =
            new GenericObjectPool<Transformer>(new XSLTTransformerPooledObjectFactory<Transformer>(xsltAsString, null, null ));
        def transformer = transformerPool.borrowObject()

        then:
        assert transformer != null
        assert transformer instanceof Transformer
    }

}
