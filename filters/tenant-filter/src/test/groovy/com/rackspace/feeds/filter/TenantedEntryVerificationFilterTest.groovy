package com.rackspace.feeds.filter

import org.w3c.dom.Document
import org.xml.sax.InputSource
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse

import static org.mockito.Matchers.eq
import static org.mockito.Mockito.*

class TenantedEntryVerificationFilterTest extends Specification {

    @Shared String tenantId = "5914283"

    public static String getSampleResponseXMLWithNamespace(tenantId) {
        return  "<?xml version=\"1.0\"?>\n" +
                "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
                "  <atom:id>urn:uuid:c2575a57-9c31-41dd-0888-86ff59a05541</atom:id>\n" +
                "  <atom:category term=\"tid:" + tenantId + "\"/>\n" +
                "  <atom:category term=\"rgn:DFW\"/>\n" +
                "  <atom:category term=\"dc:DFW1\"/>\n" +
                "  <atom:category term=\"rid:56\"/>\n" +
                "  <atom:category term=\"bigdata.bigdata.hbase_hdp1_3.usage\"/>\n" +
                "  <atom:category term=\"type:bigdata.bigdata.hbase_hdp1_3.usage\"/>\n" +
                "  <atom:title type=\"text\">Cloud Big Data</atom:title>\n" +
                "  <atom:author>\n" +
                "    <atom:name>Atom Hopper Team</atom:name>\n" +
                "  </atom:author>\n" +
                "  <atom:category label=\"atom-hopper-test\" term=\"atom-hopper-test\"/>\n" +
                "  <atom:content type=\"application/xml\">\n" +
                "    <event xmlns=\"http://docs.rackspace.com/core/event\" xmlns:bigdata=\"http://docs.rackspace.com/usage/bigdata\" dataCenter=\"DFW1\" endTime=\"2013-03-16T11:51:11Z\" environment=\"PROD\" id=\"c2575a57-9c31-41dd-0888-86ff59a05541\" region=\"DFW\" resourceId=\"56\" startTime=\"2013-03-15T11:51:11Z\" tenantId=\"5914283\" type=\"USAGE\" version=\"1\">\n" +
                "      <bigdata:product aggregatedClusterDuration=\"259200000\" bandwidthIn=\"1024\" bandwidthOut=\"19992\" flavorId=\"10\" flavorName=\"some flavor\" numberServersInCluster=\"3000\" resourceType=\"HBASE_HDP1_3\" serviceCode=\"BigData\" version=\"1\"/>\n" +
                "    </event>\n" +
                "  </atom:content>\n" +
                "  <atom:link href=\"https://atom.test.ord1.us.ci.rackspace.net/functest1/events/entries/urn:uuid:c2575a57-9c31-41dd-0888-86ff59a05541\" rel=\"self\"/>\n" +
                "  <atom:updated>2014-09-28T05:38:59.995Z</atom:updated>\n" +
                "  <atom:published>2014-09-28T05:38:59.995Z</atom:published>\n" +
                "</atom:entry>";
    }

    public static String getSampleResponseXMLWithDefaultNamespace(tenantId) {
        return  "<?xml version=\"1.0\"?>\n" +
                "<entry xmlns=\"http://www.w3.org/2005/Atom\">\n" +
                "  <id>urn:uuid:c2575a57-9c31-41dd-0888-86ff59a05541</id>\n" +
                "  <category term=\"tid:" + tenantId + "\"/>\n" +
                "  <category term=\"rgn:DFW\"/>\n" +
                "  <category term=\"dc:DFW1\"/>\n" +
                "  <category term=\"rid:56\"/>\n" +
                "  <category term=\"bigdata.bigdata.hbase_hdp1_3.usage\"/>\n" +
                "  <category term=\"type:bigdata.bigdata.hbase_hdp1_3.usage\"/>\n" +
                "  <title type=\"text\">Cloud Big Data</title>\n" +
                "  <author>\n" +
                "    <name>Atom Hopper Team</name>\n" +
                "  </author>\n" +
                "  <category label=\"atom-hopper-test\" term=\"atom-hopper-test\"/>\n" +
                "  <content type=\"application/xml\">\n" +
                "    <event xmlns=\"http://docs.rackspace.com/core/event\" xmlns:bigdata=\"http://docs.rackspace.com/usage/bigdata\" dataCenter=\"DFW1\" endTime=\"2013-03-16T11:51:11Z\" environment=\"PROD\" id=\"c2575a57-9c31-41dd-0888-86ff59a05541\" region=\"DFW\" resourceId=\"56\" startTime=\"2013-03-15T11:51:11Z\" tenantId=\"5914283\" type=\"USAGE\" version=\"1\">\n" +
                "      <bigdata:product aggregatedClusterDuration=\"259200000\" bandwidthIn=\"1024\" bandwidthOut=\"19992\" flavorId=\"10\" flavorName=\"some flavor\" numberServersInCluster=\"3000\" resourceType=\"HBASE_HDP1_3\" serviceCode=\"BigData\" version=\"1\"/>\n" +
                "    </event>\n" +
                "  </content>\n" +
                "  <link href=\"https://atom.test.ord1.us.ci.rackspace.net/functest1/events/entries/urn:uuid:c2575a57-9c31-41dd-0888-86ff59a05541\" rel=\"self\"/>\n" +
                "  <updated>2014-09-28T05:38:59.995Z</updated>\n" +
                "  <published>2014-09-28T05:38:59.995Z</published>\n" +
                "</entry>";
    }

    //Contains category with 'cloudfeeds:private' value for term attribute.
    @Shared String sampleResponseXMLPrivateCategoryEvent =
        "<?xml version=\"1.0\"?>\n" +
        "<atom:entry xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
        "  <atom:id>urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb899</atom:id>\n" +
        "  <atom:category term=\"cloudfeeds:private\"/>\n" +
        "  <atom:category term=\"tid:5914283\"/>\n" +
        "  <atom:category term=\"rgn:IAD\"/>\n" +
        "  <atom:category term=\"dc:IAD3\"/>\n" +
        "  <atom:category term=\"rid:f37bca20-29c5-4e08-97f4-e47908887bc1\"/>\n" +
        "  <atom:category term=\"cloudserversopenstack.nova.server.usage\"/>\n" +
        "  <atom:category term=\"type:cloudserversopenstack.nova.server.usage\"/>\n" +
        "  <atom:title type=\"text\">Nagios Event</atom:title>\n" +
        "  <atom:content type=\"application/xml\">\n" +
        "    <event xmlns=\"http://docs.rackspace.com/core/event\" xmlns:nova=\"http://docs.rackspace.com/event/nova\" dataCenter=\"IAD3\" endTime=\"2013-05-16T11:51:11Z\" environment=\"PROD\" id=\"e53d007a-fc23-11e1-975c-cfa6b29bb899\" region=\"IAD\" resourceId=\"f37bca20-29c5-4e08-97f4-e47908887bc1\" resourceName=\"testserver78193259535\" startTime=\"2013-05-15T11:51:11Z\" tenantId=\"5914283\" type=\"USAGE\" version=\"1\">\n" +
        "      <nova:product bandwidthIn=\"640034\" bandwidthOut=\"345123\" flavorId=\"3\" flavorName=\"1024MB\" isManaged=\"false\" osLicenseType=\"RHEL\" resourceType=\"SERVER\" serviceCode=\"CloudServersOpenStack\" status=\"ACTIVE\" version=\"1\"/>\n" +
        "    </event>\n" +
        "  </atom:content>\n" +
        "  <atom:link href=\"https://atom.test.ord1.us.ci.rackspace.net/nova/events/entries/urn:uuid:e53d007a-fc23-11e1-975c-cfa6b29bb899\" rel=\"self\"/>\n" +
        "  <atom:updated>2014-09-29T21:27:47.446Z</atom:updated>\n" +
        "  <atom:published>2014-09-29T21:27:47.446Z</atom:published>\n" +
        "</atom:entry>";

    def "verify tenantId from the #responseXML"(String responseXML) {

        given:
        def verificationFilter = new TenantedEntryVerificationFilter();
        String expectedContentTid = "tid:" + tenantId;
        Document doc = verificationFilter.builderFactory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(responseXML)));

        when:
        String contentTid = verificationFilter.getTenantIdFromResponse(doc);

        then:
        assert contentTid == expectedContentTid

        where:
        responseXML << [ getSampleResponseXMLWithNamespace(tenantId),
                         getSampleResponseXMLWithDefaultNamespace(tenantId)
                       ]
    }

    def "check if entry is private from the #responseXML"(String responseXML, boolean result) {

        given:
        def verificationFilter = new TenantedEntryVerificationFilter();
        Document doc = verificationFilter.builderFactory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(responseXML)));

        when:
        boolean isPrivateEvent = verificationFilter.isPrivateEvent(doc);

        then:
        assert isPrivateEvent == result;

        where:
        [responseXML, result] << [ [getSampleResponseXMLWithNamespace(tenantId), false],
                                   [sampleResponseXMLPrivateCategoryEvent, true]
                                 ]
    }

    @Unroll
    def "method request with #uri should not match tenantId url pattern"(String uri) {
        when:
        def tenantedEntryVerificationFilter = new TenantedEntryVerificationFilter()

        then:
        assert tenantedEntryVerificationFilter.getTenantIdFromRequestURI(uri) == null

        where:
        uri << [ "/random/333222",
                 "/feedscatalog/catalog/333222",
                 "/buildinfo"]
    }

    @Unroll
    def "validate #responseStatusCode based on #responseXML"(String responseXML, int responseStatusCode) {
        given:
        def verificationFilter = new TenantedEntryVerificationFilter();
        def mockedResponse = mock(HttpServletResponse);
        def servletOutputStream = mock(ServletOutputStream)
        when(mockedResponse.getOutputStream()).thenReturn(servletOutputStream);

        when:
        verificationFilter.validateEntryAndUpdateResponse(mockedResponse, tenantId, responseXML);

        then:
        verify(mockedResponse).setStatus(eq(responseStatusCode));

        where:
        [responseXML, responseStatusCode] << [ [sampleResponseXMLPrivateCategoryEvent, HttpServletResponse.SC_NOT_FOUND],
                                               [getSampleResponseXMLWithNamespace("123456"), HttpServletResponse.SC_NOT_FOUND]
                                             ]
    }

}
