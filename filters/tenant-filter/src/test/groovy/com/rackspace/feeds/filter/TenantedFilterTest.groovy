package com.rackspace.feeds.filter

import spock.lang.Specification
import spock.lang.Unroll

import static org.mockito.Mockito.*

import javax.servlet.http.HttpServletRequest

class TenantedFilterTest extends Specification {

    @Unroll
    def "#method request with URI #uri should not be a request to filter"(String method, String uri) {
        when:
        TenantedFilter tenantedFilter = new TenantedFilter()
        HttpServletRequest request = mock(HttpServletRequest)
        when(request.getRequestURI()).thenReturn(uri)
        when(request.getMethod()).thenReturn(method)

        then:
        assert tenantedFilter.isFeedsGetRequest(request) == false

        where:
        [method, uri] << [ ["get", "/random/333222"],
                           ["get", "/feedscatalog/catalog/333222"],
                           ["get", "/buildinfo"],
                           ["post", "/myfeed/events/333222"]
                         ]
    }

    @Unroll
    def "#method request with URI #uri should be a request to filter"(String method, String uri) {
        when:
        TenantedFilter tenantedFilter = new TenantedFilter()
        HttpServletRequest request = mock(HttpServletRequest)
        when(request.getRequestURI()).thenReturn(uri)
        when(request.getMethod()).thenReturn(method)

        then:
        assert tenantedFilter.isFeedsGetRequest(request) == true

        where:
        [method, uri] << [ ["get", "/myfeed/events/333222"],
                           ["get", "/myfeed/events"],
                           ["get", "/myfeed/notyours/events/333222"],
                           ["get", "/myfeed/notyours/events"],
                           ["get", "/myfeed/notyours/events/333222/entries/urn:uuid:389724"],
                           ["get", "/myfeed/notyours/events/entries/urn:uuid:3248972"],
                         ]
    }
}
