package com.rackspace.feeds.filter

import spock.lang.Shared
import spock.lang.Specification

import javax.servlet.http.HttpServletResponse

import static org.mockito.Mockito.*

/**
 * User: shin4590
 * Date: 9/10/14
 */
class TenantedResponseTest extends Specification {

    @Shared String tenantId = "987654"

    def "Non-tenanted search, Link header should work as is"() {
        given:
        String linkHeader = "<https://atom.test.ord1.us.ci.rackspace.net/functest1/events/?marker=urn:uuid:dd07fb26-2380-7da3-76bb-6b47490b7429&limit=25&search=%28cat%3Dtype%3Acloudservers.slice.slice.info%29&direction=forward>; rel=\"previous\""

        when:
        HttpServletResponse mockedResponse = mock(HttpServletResponse)
        TenantedFilter.TenantedResponse response = new TenantedFilter.TenantedResponse(mockedResponse, null)
        response.setHeader(TenantedFilter.LINK_HEADER, linkHeader)

        then:
        verify(mockedResponse, only()).setHeader(TenantedFilter.LINK_HEADER, linkHeader)
    }

    def "Tenanted search and user search, Link header should be stripped out"() {
        given:
        String tenantedLink = "<https://atom.myserver.com/myfeed/events/?limit=25&amp;search=%28AND%28AND%28cat%3Dtid%3A" +
                              tenantId + "%29%28NOT%28cat%3Dcloudfeeds%3Aprivate%29%29%29%28cat%3Dtype%3Acloudservers.slice.slice.info%29%29&amp;direction=backward>; rel=next"
        String strippedOutLink = "<https://atom.myserver.com/myfeed/events/" +
                              tenantId + "/?limit=25&amp;search=%28cat%3Dtype%3Acloudservers.slice.slice.info%29&amp;direction=backward>; rel=next"

        when:
        HttpServletResponse mockedResponse = mock(HttpServletResponse)
        TenantedFilter.TenantedResponse response = new TenantedFilter.TenantedResponse(mockedResponse, tenantId)
        response.setHeader(TenantedFilter.LINK_HEADER, tenantedLink)

        then:
        verify(mockedResponse, only()).setHeader(TenantedFilter.LINK_HEADER, strippedOutLink)
    }

    def "Tenanted search and user search, multiple Link(s) header should be stripped out"() {
        given:
        String tenantedLink = "<https://atom.myserver.com/myfeed/events/?marker=urn:uuid:16263c9b-fb7d-231a-4567-3b46a81a5dac&limit=25&search=%28AND%28AND%28cat%3Dtid%3A" +
                              tenantId +
                              "%29%28NOT%28cat%3Dcloudfeeds%3Aprivate%29%29%29%28cat%3Dcloudservers.slice.slice.info%29%29&direction=backward>; rel=\"next\", " +
                              "<https://atom.myserver.com/myfeed/events/?marker=urn:uuid:3733686d-2c1f-6bd9-5679-fd1a81453a99&limit=25&search=%28AND%28AND%28cat%3Dtid%3A" +
                              tenantId +
                              "%29%28NOT%28cat%3Dcloudfeeds%3Aprivate%29%29%29%28cat%3Dcloudservers.slice.slice.info%29%29&direction=forward>; rel=\"previous\" "
        String strippedOutLink = "<https://atom.myserver.com/myfeed/events/" +
                              tenantId + "/?marker=urn:uuid:16263c9b-fb7d-231a-4567-3b46a81a5dac&limit=25&search=" +
                              "%28cat%3Dcloudservers.slice.slice.info%29&direction=backward>; rel=\"next\", " +
                              "<https://atom.myserver.com/myfeed/events/" +
                              tenantId + "/?marker=urn:uuid:3733686d-2c1f-6bd9-5679-fd1a81453a99&limit=25&search=" +
                              "%28cat%3Dcloudservers.slice.slice.info%29&direction=forward>; rel=\"previous\" "

        when:
        HttpServletResponse mockedResponse = mock(HttpServletResponse)
        TenantedFilter.TenantedResponse response = new TenantedFilter.TenantedResponse(mockedResponse, tenantId)
        response.setHeader(TenantedFilter.LINK_HEADER, tenantedLink)

        then:
        verify(mockedResponse, only()).setHeader(TenantedFilter.LINK_HEADER, strippedOutLink)
    }


}
