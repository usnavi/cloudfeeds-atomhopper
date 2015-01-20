package com.rackspace.feeds.filter

import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletRequest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class Xml2JsonFilterTest extends Specification {

    @Unroll
    def "Should say json is preferred for #method request with #acceptHeader" (String method, String acceptHeader) {
        when:
        HttpServletRequest request = mock(HttpServletRequest)
        when(request.getHeader("Accept")).thenReturn(acceptHeader)
        when(request.getMethod()).thenReturn(method)

        Xml2JsonFilter jsonFilter = new Xml2JsonFilter()

        then:
        assert(jsonFilter.jsonPreferred(request))

        where:
        [method, acceptHeader] << [ ['GET', 'application/vnd.rackspace.atom+json'],
                                    ['GET', 'application/vnd.rackspace.atomsvc+json'],
                                    ['GET', 'application/vnd.rackspace.atom+json,application/json'],
                                    ['GET', 'application/vnd.rackspace.atomsvc+json,application/json'],
                                    ['GET', 'application/json;q=0.5, application/vnd.rackspace.atom+json'],
                                    ['GET', 'application/json;q=0.5, application/vnd.rackspace.atomsvc+json'],
                                    ['GET', 'application/json;q=0.5, application/vnd.rackspace.atom+json;q=0.7, text/html'],
                                    ['GET', 'application/json;q=0.5, application/vnd.rackspace.atomsvc+json;q=0.7, text/html'],
                                    ['POST', 'application/vnd.rackspace.atom+json'],
                                    ['POST', 'application/vnd.rackspace.atomsvc+json'],
                                    ['POST', 'application/vnd.rackspace.atom+json,application/json'],
                                    ['POST', 'application/vnd.rackspace.atomsvc+json,application/json'],
                                    ['POST', 'application/json;q=0.5, application/vnd.rackspace.atom+json'],
                                    ['POST', 'application/json;q=0.5, application/vnd.rackspace.atomsvc+json'],
                                    ['POST', 'application/json;q=0.5, application/vnd.rackspace.atom+json;q=0.7, text/html'],
                                    ['POST', 'application/json;q=0.5, application/vnd.rackspace.atomsvc+json;q=0.7, text/html'],
                        ]
    }

    @Unroll
    def "Should NOT say json is preferred for #method request with #acceptHeader" (String method, String acceptHeader) {
        when:
        HttpServletRequest request = mock(HttpServletRequest)
        when(request.getHeader("Accept")).thenReturn(acceptHeader)
        when(request.getMethod()).thenReturn(method)

        Xml2JsonFilter jsonFilter = new Xml2JsonFilter()

        then:
        assert(!jsonFilter.jsonPreferred(request))

        where:
        [method, acceptHeader] << [
                ['GET', 'application/json'],
                ['GET', 'application/vnd.rackspace.atom+json;q=0.1,application/json;q=0.5'],
                ['GET', 'application/json;q=0.5, application/vnd.rackspace.atom+json;q=0.3'],
                ['GET', 'application/json;q=0.5, text/html'],
                ['GET', 'application/json;q=0.5, application/vnd.rackspace.atom+json;q=0.8, application/atom+xml'],
                ['GET', 'application/json;q=0.5, application/vnd.rackspace.atom+xml;q=0.8, application/atom+xml'],
                ['GET', 'application/json;q=0.5, application/vnd.rackspace.atomsvc+xml;q=0.8, application/atom+xml'],

                ['POST', 'application/json'],
                ['POST', 'application/vnd.rackspace.atom+json;q=0.1,application/json;q=0.5'],
                ['POST', 'application/json;q=0.5, application/vnd.rackspace.atom+json;q=0.3'],
                ['POST', 'application/json;q=0.5, text/html'],
                ['POST', 'application/json;q=0.5, application/vnd.rackspace.atom+json;q=0.8, application/atom+xml'],
        ]
    }
}
