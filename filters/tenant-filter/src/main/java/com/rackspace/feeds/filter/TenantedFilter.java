package com.rackspace.feeds.filter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
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
     *  This denotes what a tenanted URI should look like.
     */
    static Pattern TENANTED_URI_PATTERN = Pattern.compile("(.*/events/)([^/?]+)(/entries/[^/?]+)?/?(\\?.*)?");

    static final String TENANTED_SEARCH_FORMAT = "(AND(AND(cat=tid:%s)(NOT(cat=cloudfeeds:private)))%s)";

    static final String SEARCH_PARAM = "search";

    public void  init(FilterConfig config)
            throws ServletException {
        LOG.debug("initializing TenantedFilter");
    }

    public void  doFilter(ServletRequest request,
                          ServletResponse response,
                          FilterChain chain)
            throws java.io.IOException, ServletException {

        // Pass request back down the filter chain
        if ( isFeedsGetRequest((HttpServletRequest) request) ) {
            chain.doFilter(new TenantedRequest(request), response);
        } else {
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

    static class TenantedRequest extends HttpServletRequestWrapper {

        public TenantedRequest(ServletRequest request) {
            super((HttpServletRequest)request);
        }

        @Override
        public String getParameter(String parameterName) {

            String value = super.getParameter(parameterName);
            if ( SEARCH_PARAM.equals(parameterName) ) {
                String tenantId = getTenantId();
                if ( tenantId != null ) {
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

        String getTenantId() {
            String uri = super.getRequestURI();
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
}
