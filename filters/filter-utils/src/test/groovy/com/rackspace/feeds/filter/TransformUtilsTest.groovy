package com.rackspace.feeds.filter

import org.apache.commons.io.IOUtils
import spock.lang.Specification
import spock.lang.Unroll

import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class TransformUtilsTest extends Specification {

    def "verify first byte"() {

        given:
        def input = "apple"
        def transformerUtils = TransformerUtils.getInstanceForXsltAsResource("/samples/test.xsl")
        def inputStream = IOUtils.toInputStream(input, "UTF-8")

        when:
        int firstByte = transformerUtils.getFirstByte(inputStream);

        then:
        assert firstByte == input.charAt(0)
        assert inputStream.read() == input.charAt(0) //verifies that stream is reset
    }

    @Unroll
    def "Should throw IllegalArgumentException when xsltFilePath is invalid(#xsltFilePath)"(String xsltFilePath) {

        when:
        TransformerUtils.getInstanceForXsltAsResource(xsltFilePath);

        then:
        thrown IllegalArgumentException

        where:
        xsltFilePath << [null,
                "/some/nonexistent/path"]

    }

    @Unroll
    def "Should throw IllegalArgumentException when external xsltFilePath is invalid(#xsltFilePath)"(String xsltFilePath) {

        when:
        TransformerUtils.getInstanceForXsltAsFile(xsltFilePath);

        then:
        thrown IllegalArgumentException

        where:
        xsltFilePath << [null,
                "/some/nonexistent/path"]
    }

    @Unroll
    def "should transform xml using xslt"() {

        when:
        def writer = new StringWriter();
        def xsltTransformer = TransformerUtils.getInstanceForXsltAsResource("/samples/test.xsl")
        xsltTransformer.doTransform(Collections.EMPTY_MAP,
                new StreamSource(this.getClass().getResourceAsStream("/samples/test.xml")),
                new StreamResult(writer))

        then:
        assert writer.toString().length() > 0
        assert writer.toString() == "Unstoppable Juggernaut"

    }
}
