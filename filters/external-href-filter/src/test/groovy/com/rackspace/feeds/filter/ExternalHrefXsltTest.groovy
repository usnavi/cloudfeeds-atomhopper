package com.rackspace.feeds.filter

import com.sun.org.apache.xpath.internal.XPathAPI
import org.xml.sax.InputSource
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * User: shin4590
 * Date: 9/14/14
 */
class ExternalHrefXsltTest extends Specification {

    @Shared TransformerFactory transformerFactory =
            TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
    @Shared DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance()
    @Shared String correctUrl = "https://myfeed.myserver.com"

    @Unroll
    def "Tenanted Get feed response should be not have links with tenanted search"() {

        when:
            InputStream xsltInput = getClass().getResourceAsStream(ExternalHrefFilter.XSLT_PATH)
            InputStream xmlInput = getClass().getResourceAsStream("/responses/functest1-get-feed-response.xml")
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(xsltInput))
            transformer.setParameter("correct_url", correctUrl)
            CharArrayWriter caw = new CharArrayWriter()
            StreamResult result = new StreamResult(caw)

            transformer.transform(new StreamSource(xmlInput), result)
            Writer writer = result.writer
            String response = writer.toString();
            def builder = documentBuilderFactory.newDocumentBuilder()
            def root = builder.parse(new InputSource( new StringReader( response ) )).documentElement

        then:
            def linkRels = [ 'next', 'previous', 'last', 'self', 'current']
            linkRels.each {
                def nodes = XPathAPI.eval( root, "//link[@rel = '" + it + "']/@href")
                nodes.each {
                    assert it.toString().contains(correctUrl)
                }
            }
            def alternate = XPathAPI.eval(root, "//link[@rel='alternate']/@href")
            assert !alternate.toString().contains(correctUrl)
    }
}
