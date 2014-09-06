package com.rackspace.feeds.filter

import spock.lang.Shared

import javax.servlet.http.HttpServletRequest
import static org.mockito.Mockito.*

import spock.lang.Specification

class TenantedRequestTest extends Specification {

    @Shared String tenantId = "987654"
    @Shared String baseFeedUri = "/myfeed/events"

    def "Non-tenanted GET request with an empty search param, should return other params as is"() {
        given:
        String feedUri = baseFeedUri

        when:
        HttpServletRequest mockedRequest = mock(HttpServletRequest);
        when(mockedRequest.getParameter(TenantedFilter.SEARCH_PARAM)).thenReturn("")
        when(mockedRequest.getParameter("foo")).thenReturn("bar")
        when(mockedRequest.getRequestURI()).thenReturn(feedUri)
        TenantedFilter.TenantedRequest request = new TenantedFilter.TenantedRequest(mockedRequest);

        then:
        assert request.getParameter("foo") == "bar"
        assert request.getRequestURI() == feedUri
    }

    def "Non-tenanted GET request with a non empty search param, should not contain tenanted search"() {
        given:
        String feedUri = baseFeedUri

        when:
        HttpServletRequest mockedRequest = mock(HttpServletRequest);
        when(mockedRequest.getParameter(TenantedFilter.SEARCH_PARAM)).thenReturn("cat=cat1")
        when(mockedRequest.getParameter("foo")).thenReturn("bar")
        when(mockedRequest.getRequestURI()).thenReturn(feedUri)
        TenantedFilter.TenantedRequest request = new TenantedFilter.TenantedRequest(mockedRequest);

        then:
        String searchParam = request.getParameter("search")
        assert searchParam == "cat=cat1"
        assert !searchParam.contains("tid:")
        assert !searchParam.contains("cloudfeeds:private")
        assert request.getRequestURI() == feedUri
    }

    def "Non-tenanted GET entry request, should work as is"() {
        String entryUri = baseFeedUri + "/entries/urn:uuid:1415971f-ef5e-466f-b737-ed445bd36d29"

        when:
        HttpServletRequest mockedRequest = mock(HttpServletRequest);
        when(mockedRequest.getParameter(TenantedFilter.SEARCH_PARAM)).thenReturn("")
        when(mockedRequest.getParameter("foo")).thenReturn("bar")
        when(mockedRequest.getRequestURI()).thenReturn(entryUri)
        TenantedFilter.TenantedRequest request = new TenantedFilter.TenantedRequest(mockedRequest);

        then:
        String searchParam = request.getParameter("search")
        assert searchParam == ""
        assert request.getRequestURI() == entryUri
    }

    def "Tenanted GET request with an empty search param, return a tenanted search"() {
        given:
        String feedUri = baseFeedUri

        when:
        HttpServletRequest mockedRequest = mock(HttpServletRequest);
        when(mockedRequest.getParameter(TenantedFilter.SEARCH_PARAM)).thenReturn(null)
        when(mockedRequest.getParameter("foo")).thenReturn("bar")
        when(mockedRequest.getRequestURI()).thenReturn(feedUri + "/" + tenantId)
        TenantedFilter.TenantedRequest request = new TenantedFilter.TenantedRequest(mockedRequest);

        then:
        String searchParam = request.getParameter(TenantedFilter.SEARCH_PARAM)
        assert searchParam.contains(tenantId)
        assert searchParam.contains("cloudfeeds:private")
        assert request.getRequestURI() == feedUri
    }

    def "Tenanted GET request with a non empty search param, return a tenanted search"() {
        given:
        String feedUri = baseFeedUri

        when:
        HttpServletRequest mockedRequest = mock(HttpServletRequest);
        when(mockedRequest.getParameter(TenantedFilter.SEARCH_PARAM)).thenReturn("(AND(cat=cat1)(cat=cat2))")
        when(mockedRequest.getParameter("foo")).thenReturn("bar")
        when(mockedRequest.getRequestURI()).thenReturn(feedUri + "/" + tenantId)
        TenantedFilter.TenantedRequest request = new TenantedFilter.TenantedRequest(mockedRequest);

        then:
        String searchParam = request.getParameter(TenantedFilter.SEARCH_PARAM)
        assert searchParam.contains(tenantId)
        assert searchParam.contains("cloudfeeds:private")
        assert request.getRequestURI() == feedUri
    }

    def "Tenanted GET entry request, request.getRequestURI() should return non-tenanted"() {
        given:
        String feedUri = baseFeedUri
        String entries = "/entries/urn:uuid:1415971f-ef5e-466f-b737-ed445bd36d29"
        String tenantedEntryUri = feedUri + "/" + tenantId + entries
        String entryUri = feedUri + entries

        when:
        HttpServletRequest mockedRequest = mock(HttpServletRequest);
        when(mockedRequest.getParameter(TenantedFilter.SEARCH_PARAM)).thenReturn("")
        when(mockedRequest.getParameter("foo")).thenReturn("bar")
        when(mockedRequest.getRequestURI()).thenReturn(tenantedEntryUri)
        TenantedFilter.TenantedRequest request = new TenantedFilter.TenantedRequest(mockedRequest);

        then:
        assert request.getRequestURI() == entryUri
    }


}
