package com.rackspace.feeds.filter

import spock.lang.Specification
import spock.lang.Unroll

import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class XSLTTransformerUtilTest extends Specification {

    @Unroll
    def "Should throw IllegalArgumentException when xsltFilePath is invalid(#xsltFilePath)"(String xsltFilePath) {

        when:
        XSLTTransformerUtil.getInstance(xsltFilePath);

        then:
        thrown IllegalArgumentException

        where:
        xsltFilePath << [null,
                "/some/nonexistent/path"]

    }

    @Unroll
    def "Should throw IllegalArgumentException when external xsltFilePath is invalid(#xsltFilePath)"(String xsltFilePath) {

        when:
        XSLTTransformerUtil.getInstanceForExternalFile(xsltFilePath);

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
        def xsltTransformer = XSLTTransformerUtil.getInstance("/samples/test.xsl")
        xsltTransformer.doTransform(Collections.EMPTY_MAP,
                new StreamSource(this.getClass().getResourceAsStream("/samples/test.xml")),
                new StreamResult(writer))

        then:
        assert writer.toString().length() > 0
        assert writer.toString() == "Unstoppable Juggernaut"

    }

}
