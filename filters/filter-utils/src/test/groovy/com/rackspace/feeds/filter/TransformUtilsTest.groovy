package groovy.com.rackspace.feeds.filter

import com.rackspace.feeds.filter.TransformerUtils
import org.apache.commons.io.IOUtils
import spock.lang.Specification

class TransformUtilsTest extends Specification {

    def "verify first byte"() {

        given:
        def input = "apple"
        def transformerUtils = new TransformerUtils()
        def inputStream = IOUtils.toInputStream(input, "UTF-8")

        when:
        int firstByte = transformerUtils.getFirstByte(inputStream);

        then:
        assert firstByte == input.charAt(0)
        assert inputStream.read() == input.charAt(0) //verifies that stream is reset
    }

}
