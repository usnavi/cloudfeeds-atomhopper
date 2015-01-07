package com.rackspace.feeds.filter

import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

import static org.mockito.Mockito.*

/**
 * Created by shin4590 on 1/5/15.
 */
class JsonResponseBodyWrapperTest extends Specification {

    @Unroll
    def "Should change response contentType #originalContentType to #eventualContentType" (String originalContentType, String eventualContentType) {
        when:
        HttpServletResponse response = mock(HttpServletResponse)
        Xml2JsonFilter.JsonResponseBodyWrapper wrapper = new Xml2JsonFilter.JsonResponseBodyWrapper(response);

        then:
        assert(wrapper.swizzleContentType(originalContentType).equals(eventualContentType));

        where:
        [originalContentType, eventualContentType] << [
                [ 'application/atom+xml; type=feed;charset=UTF-8',  'application/vnd.rackspace.atom+json; type=feed;charset=UTF-8'],
                [ 'application/atom+xml; type=entry;charset=UTF-8', 'application/vnd.rackspace.atom+json; type=entry;charset=UTF-8'],
                [ 'application/xml',                                'application/json'],
        ]
    }
}
