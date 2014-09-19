package com.rackspace.feeds.filter

import com.rackspace.feeds.filter.mockito.*
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.http.HttpServletResponse

import static org.mockito.Mockito.*

class URLFixerResponseTest extends Specification {

    @Shared String correctHttpsUrl = "https://myfeed.myserver.com"
    @Shared String oldHttpsUrl = "https://myfeed-internal.myserver.com"
    @Shared String correctHttpUrl = "http://myfeed.myserver.com:9090"
    @Shared String oldHttpUrl = "http://myfeed-internal.myserver.com:9090"

    @Unroll
    def "When link header(s) (#link) is added, the URL should be corrected"(String link) {
        when:
        HttpServletResponse mockServletResponse = mock(HttpServletResponse)
        ExternalHrefFilter.URLFixerResponse urlFixerResponse = new ExternalHrefFilter.URLFixerResponse(mockServletResponse, correctHttpsUrl)
        urlFixerResponse.addHeader(ExternalHrefFilter.LINK_HEADER, link)

        then:
        verify(mockServletResponse).addHeader(eq(ExternalHrefFilter.LINK_HEADER), argThat(new NotContains(oldHttpsUrl)))

        where:
        link << [ "<" + oldHttpsUrl + "/myworkspace/5914283/?limit=25&amp;search=&amp;direction=backward>; rel=\"previous\"",
                  "<" + oldHttpsUrl + "/myworkspace/5914283/?limit=25&amp;search=&amp;direction=backward>; rel=\"previous\", " +
                      "<" + oldHttpsUrl + "/myworkspace/5914283/?limit=25&amp;search=&amp;direction=forward>; rel=\"next\"" ]

    }

    @Unroll
    def "When link header (#link) is set, the URL should be corrected"(String link) {
        when:
        HttpServletResponse mockServletResponse = mock(HttpServletResponse)
        ExternalHrefFilter.URLFixerResponse urlFixerResponse = new ExternalHrefFilter.URLFixerResponse(mockServletResponse, correctHttpsUrl)
        urlFixerResponse.setHeader(ExternalHrefFilter.LINK_HEADER, link)

        then:
        verify(mockServletResponse).setHeader(eq(ExternalHrefFilter.LINK_HEADER), argThat(new NotContains(oldHttpsUrl)))

        where:
        link << [ "<" + oldHttpsUrl + "/myworkspace/5914283/?limit=25&amp;search=&amp;direction=backward>; rel=\"previous\"",
                  "<" + oldHttpsUrl + "/myworkspace/5914283/?limit=25&amp;search=&amp;direction=backward>; rel=\"previous\", " +
                        "<" + oldHttpsUrl + "/myworkspace/5914283/?limit=25&amp;search=&amp;direction=forward>; rel=\"next\"" ]

    }
    @Unroll
    def "When location header(s) (#location) is set, the URL should be corrected to #correctedHostname"(String location, String correctedHostname) {
        when:
        HttpServletResponse mockServletResponse = mock(HttpServletResponse)
        ExternalHrefFilter.URLFixerResponse urlFixerResponse = new ExternalHrefFilter.URLFixerResponse(mockServletResponse, correctedHostname)
        urlFixerResponse.setHeader(ExternalHrefFilter.LOCATION_HEADER, location)

        then:
        verify(mockServletResponse).setHeader(eq(ExternalHrefFilter.LOCATION_HEADER), argThat(new NotContains(oldHttpsUrl)))

        where:
        [location, correctedHostname] << [ [oldHttpsUrl + "/myworkspace/5914283/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814",
                                                correctHttpsUrl],
                                           [oldHttpUrl + "/myworkspace/5914283/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814",
                                                correctHttpUrl ],
        ]
    }

    @Unroll
    def "When location header(s) (#location) is added, the URL should be corrected to #correctedHostname"(String location, String correctedHostname) {
        when:
        HttpServletResponse mockServletResponse = mock(HttpServletResponse)
        ExternalHrefFilter.URLFixerResponse urlFixerResponse = new ExternalHrefFilter.URLFixerResponse(mockServletResponse, correctedHostname)
        urlFixerResponse.addHeader(ExternalHrefFilter.LOCATION_HEADER, location)

        then:
        verify(mockServletResponse).addHeader(eq(ExternalHrefFilter.LOCATION_HEADER), argThat(new NotContains(oldHttpsUrl)))

        where:
        [location, correctedHostname] << [ [oldHttpsUrl + "/myworkspace/5914283/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814",
                                                correctHttpsUrl],
                                           [oldHttpUrl + "/myworkspace/5914283/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb814",
                                                correctHttpUrl ],
                                          ]
    }
}
