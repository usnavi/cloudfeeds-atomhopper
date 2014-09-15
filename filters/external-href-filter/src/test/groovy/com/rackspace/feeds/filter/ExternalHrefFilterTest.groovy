package com.rackspace.feeds.filter

import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static org.mockito.Mockito.*

/**
 * User: shin4590
 * Date: 9/14/14
 */
class ExternalHrefFilterTest extends Specification {

    def "Should throw ServletException, when envFile is not specified"() {
        when:
        ExternalHrefFilter filter = new ExternalHrefFilter()
        FilterConfig config = mock(FilterConfig)
        when(config.getInitParameter("envFile")).thenReturn(null)
        filter.init(config)

        then:
        thrown ServletException
    }

    def "Should throw ServletException, when envFile is specified but file doesn't exist"() {
        when:
        ExternalHrefFilter filter = new ExternalHrefFilter()
        FilterConfig config = mock(FilterConfig)
        when(config.getInitParameter("envFile")).thenReturn("/some/nonexistent/path")
        filter.init(config)

        then:
        thrown ServletException
    }

    def "Should pass through, when envFile is specified, file exists, and x-external-loc header does not exists"() {
        when:
        ExternalHrefFilter filter = new ExternalHrefFilter()
        FilterConfig config = mock(FilterConfig)
        HttpServletRequest request = mock(HttpServletRequest)
        HttpServletResponse response = mock(HttpServletResponse)
        FilterChain chain = mock(FilterChain)
        File tempFile = File.createTempFile(this.getClass().canonicalName, "tmp")
        PrintWriter writer = new PrintWriter(tempFile)
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<environment>\n" +
                "   <region>DFW</region>\n" +
                "   <vipURL>https://atom.test.ord1.us.ci.rackspace.net</vipURL>\n" +
                "   <externalVipURL>https://test.ord.feeds.api.rackspacecloud.com</externalVipURL>\n" +
                "</environment>")
        when(config.getInitParameter("envFile")).thenReturn(tempFile.absolutePath)
        when(request.getHeader("x-external-loc")).thenReturn("")
        filter.init(config)
        filter.doFilter(request, response, chain)

        then:
        verify(chain, only()).doFilter(request, response)
        tempFile.deleteOnExit()
    }
}
