package com.rackspace.feeds.filter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a servlet filter that does the following:
 * <ul>
 *     <li>Operates only on GET requests with certain URI pattern</li>
 *     <li>If tenantId exists in URI, removes it from URI</li>
 *     <li>If tenantId exists in URI, inserts tenanted 'search' parameter,
 *     taking account in both cases where there is an existing search parameter and
 *     where there is no search parameter in the request.</li>
 * </ul>
 *
 * On the response, the filter does the opposite of the above:
 * <ul>
 *     <li>If tenantId exists in the URI, it puts it back in the URI</li>
 *     <li>If tenantId exists in the URI, it removes the tenanted 'search' from
 *     the following header(s): LINK</li>
 *     <li>If tenantId exists in the URI, </li>
 * </ul>
 *
 * The response body will also contain several links (per Atom's spec) that contain
 * tenanted 'search' parameter that must be removed. This will be handled using an
 * XsltFilter.
 *
 * User: shin4590
 * Date: 9/3/14
 */
public class TenantedFilter implements Filter {

    private static Logger LOG = LoggerFactory.getLogger(TenantedFilter.class);

    /**
     * Denotes the URI pattern on which this Filter will operate on. If URI does not
     * match this pattern, then the Filter will act as a pass-through.
     *
     * We do this here because Servlet Filter's URL Pattern is limited.
     */
    static Pattern FEEDS_URI_PATTERN = Pattern.compile(".*/events.*");

    /**
     * Denotes what a tenanted URI should look like.
     */
    static final Pattern TENANTED_URI_PATTERN = Pattern.compile("(.*/events/)([^/?]+)(/entries/[^/?]+)?/?(\\?.*)?");

    /**
     * Denotes what a tenanted 'search' parameter looks like in a normalized URI.
     */
    static final Pattern NORMALIZED_TENANTED_SEARCH_PATTERN = Pattern.compile("(.*)%28AND%28AND%28cat%3Dtid%3A(.*)%29%28NOT%28cat%3Dcloudfeeds%3Aprivate%29%29%29(.*)%29(.*)");

    /**
     * Denotes the format of tenanted 'search'
     */
    static final String TENANTED_SEARCH_FORMAT = "(AND(AND(cat=tid:%s)(NOT(cat=cloudfeeds:private)))%s)";

    static final String SEARCH_PARAM = "search";

    static final String LINK_HEADER = "link";

    static final String LINK_DELIM = ",";

    static final String XSLT_PATH = "/xslt/rm-tenanted-search.xsl";

    public void  init(FilterConfig config)
            throws ServletException {
        LOG.debug("initializing TenantedFilter");
    }

    public void  doFilter(ServletRequest request,
                          ServletResponse response,
                          final FilterChain chain)
            throws java.io.IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        TenantedRequest tenantedRequest = new TenantedRequest(httpServletRequest);

        if ( isFeedsGetRequest(httpServletRequest) && StringUtils.isNotBlank(tenantedRequest.getTenantId()) ) {

            TenantedResponse tenantedResponse = new TenantedResponse(httpServletResponse, tenantedRequest.getTenantId());

            Map<String, Object> xsltParameters = new HashMap<String, Object>();
            xsltParameters.put("tenantId", tenantedRequest.getTenantId());

            TransformerUtils transformer = new TransformerUtils();
            transformer.doTransform(tenantedRequest,
                                    tenantedResponse,
                                    httpServletResponse,
                                    chain,
                                    getXsltStreamSource(),
                                    xsltParameters);

        } else {
            // pass through
            chain.doFilter(request, response);
        }
    }

    public void destroy( ) {
      /* Called before the Filter instance is removed
      from service by the web container*/
    }

    boolean isFeedsGetRequest(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        Matcher matcher = FEEDS_URI_PATTERN.matcher(requestURI);
        return matcher.matches() && request.getMethod().equalsIgnoreCase("get");
    }

    protected StreamSource getXsltStreamSource()
            throws IOException {
        InputStream is = getClass().getResourceAsStream(XSLT_PATH);
        return new StreamSource(is);
    }

    /**
     * Wrapper class for HttpServletRequest that knows how to look for the tenanted requests.
     * If the tenantId exists in the URI:
     * <ul>
     *     <li>it removes the tenantId from the URI</li>
     *     <li>it inserts tenanted search parameter into the 'search' query parameter</li>
     * </ul>
     */
    static class TenantedRequest extends HttpServletRequestWrapper {

        private String tenantId;

        public TenantedRequest(HttpServletRequest request) {
            super(request);
            this.tenantId = calculateTenantId(request);
        }

        @Override
        public String getParameter(String parameterName) {

            String value = super.getParameter(parameterName);
            if ( SEARCH_PARAM.equals(parameterName) ) {
                if ( StringUtils.isNotEmpty(tenantId) ) {
                    // If there's no search parameter, value will be null or empty. We will
                    // instead have a search parameter like this:
                    //   (AND(AND(cat=tid:5914283)(NOT(cat=cloudfeeds:private))))
                    // There will be an extra 'AND' there with empty operand. The search works
                    // this way too.
                    return String.format(TENANTED_SEARCH_FORMAT, tenantId, StringUtils.defaultIfEmpty(value, ""));
                }
            }
            return value;
        }

        @Override
        public String getRequestURI() {
            String uri = super.getRequestURI();

            // strip the tenantId part, if this is a tenanted URL
            Matcher matcher = TENANTED_URI_PATTERN.matcher(uri);
            if ( matcher.matches() ) {
                String afterTenantId = matcher.group(3);
                if ( afterTenantId != null ) {
                    return matcher.group(1).replaceAll("/$", "") + matcher.group(3);
                } else {
                    return matcher.group(1).replaceAll("/$", "");
                }
            }
            return uri;
        }

        public String getTenantId() {
            return tenantId;
        }

        String calculateTenantId(HttpServletRequest request) {
            String uri = request.getRequestURI();
            if ( StringUtils.isEmpty(uri) ) {
                throw new IllegalArgumentException("Empty uri");
            }

            Matcher matcher = TENANTED_URI_PATTERN.matcher(uri);
            if ( !matcher.matches() ) {
                return null;
            }
            return matcher.group(2);
        }
    }

    /**
     * Wrapper class for HttpServletResponse that understands tenanted requests.
     * If the tenantId exists in the URI:
     * <ul>
     *     <li>it adds the tenantId back to the URI</li>
     *     <li>it removes the tenanted search from the 'search' query parameter</li>
     * </ul>
     */
    static class TenantedResponse extends HttpServletResponseWrapper {

        private String tenantId = null;

        public TenantedResponse(HttpServletResponse response, String tenantId) {
            super(response);
            this.tenantId = tenantId;
        }

        public void setHeader(String name, String value) {
            String newLink = calculateNewLinkHeaderMultiple(name, value, tenantId);
            super.setHeader(name, newLink);
        }

        public void addHeader(String name, String value) {
            String newLink = calculateNewLinkHeaderMultiple(name, value, tenantId);
            super.addHeader(name, newLink);
        }

        String calculateNewLinkHeaderMultiple(String name, String value, String tenantId) {
            String newLink = value;
            if ( StringUtils.isNotBlank(value) ) {
                // in case Link header contains multiple links
                String[] links = value.split(LINK_DELIM);
                if ( links != null ) {
                    for (int idx=0; idx<links.length; idx++) {
                        links[idx] = calculateANewLinkHeader(name, links[idx], tenantId);
                    }
                    newLink = StringUtils.join(links, LINK_DELIM);
                    LOG.debug("joined multiple 'link' values together: " + newLink);
                }
            }
            return newLink;
        }

        String calculateANewLinkHeader(String name, String value, String tenantId) {
            if ( StringUtils.isNotBlank(tenantId) ) {
                if ( name.equalsIgnoreCase(LINK_HEADER) ) {
                    // re-insert tenantId
                    String newValue = value.replaceAll("/events", "/events/" + tenantId);

                    // strip the tenanted search format
                    Matcher customMatcher = NORMALIZED_TENANTED_SEARCH_PATTERN.matcher(newValue);
                    if ( customMatcher.matches() ) {
                        String stripped = customMatcher.group(1) + customMatcher.group(3) + customMatcher.group(4);
                        LOG.debug("Setting " + name + " header to " + stripped);
                        return stripped;
                    } else {
                        return newValue;
                    }

                }
            }
            return value;
        }
    }
}
