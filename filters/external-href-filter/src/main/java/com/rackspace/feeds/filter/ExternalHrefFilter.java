package com.rackspace.feeds.filter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a servlet filter that does the following:
 * <ul>
 *     <li>operates only both GET and POST requests</li>
 *     <li>if the x-external-loc header is present (injected by Repose),
 *     this filter will:
 *     <ul>
 *         <li>replace the URL in LINK and LOCATION headers to the URL specified
 *             in the 'envFile' file, inside &lt;externalVIPURL&gt; element</li>
 *         <li>replace the URL in all the @href attributes in the Response body
 *             to the URL specified in the 'envFile' file, inside
 *             &lt;externalVIPURL&gt; element</li>
 *     </ul>
 *     </li>
 * </ul>
 */
public class ExternalHrefFilter implements Filter {

    private static Logger LOG = LoggerFactory.getLogger(ExternalHrefFilter.class);

    /**
     * Where the XSLT is going to be loaded from classpath
     */
    static final String XSLT_PATH = "/xslt/external-href.xsl";
    static final String LINK_HEADER = "link";
    static final String LOCATION_HEADER = "location";
    static final String EXTERNAL_LOC_HEADER = "x-external-loc";
    static final String LINK_DELIM = ",";

    private String correctUrl = "";

    private TransformerUtils transformer;

    public void  init(FilterConfig config)
            throws ServletException {

        LOG.debug("initializing ExternalHrefFilter");
        String envFilePath = config.getInitParameter("envFile");
        if ( envFilePath == null ) {
            throw new ServletException("envFile parameter is required for this filter");
        }
        File envFile = new File(envFilePath);
        if ( !envFile.exists() ) {
            throw new ServletException("envFile parameter must be a valid existing file");
        }
        try {
            // quick and dirty, since the file is very small
            byte[] bytes = Files.readAllBytes(Paths.get(envFilePath));
            String environment = new String(bytes);
            Pattern pattern = Pattern.compile(".*<externalVipURL>(.*)</externalVipURL>.*");
            Matcher matcher = pattern.matcher(environment);
            if ( matcher.find() ) {
                correctUrl = matcher.group(1);
            }
        } catch(IOException ioex) {
            LOG.error("Error reading envFile: " + envFile, ioex);
            throw new ServletException(ioex);
        }
        transformer = TransformerUtils.getInstanceForXsltAsResource(XSLT_PATH);
    }

    public void  doFilter(ServletRequest request,
                          ServletResponse response,
                          FilterChain chain)
            throws java.io.IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String externalLocHeader = httpServletRequest.getHeader(EXTERNAL_LOC_HEADER);
        if ( StringUtils.isNotEmpty(externalLocHeader) ) {

            LOG.debug("Header " + EXTERNAL_LOC_HEADER + " = " + externalLocHeader);

            URLFixerResponse urlFixerResponse = new URLFixerResponse(httpServletResponse, correctUrl);

            Map<String, Object> xsltParameters = new HashMap<String, Object>();
            xsltParameters.put("correct_url", correctUrl);

            transformer.doTransform(httpServletRequest,
                                    urlFixerResponse,
                                    httpServletResponse,
                                    chain,
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

    /**
     * Wrapper class for HttpServletResponse that knows how to fix the URL in the
     * LINK and LOCATION header.
     */
    static class URLFixerResponse extends HttpServletResponseWrapper {

        private String correctUrl = null;

        // This pattern is slightly different from the existing XSLT.
        // This one is meant to support http://hostname:port, as well as
        // https://hostname
        private static final Pattern HOSTNAME_PATTERN = Pattern.compile("(.*)(http)(s?)(://[^/]+)/(.*)");

        public URLFixerResponse(HttpServletResponse response, String correctUrl) {
            super(response);
            this.correctUrl = correctUrl;
        }

        public void setHeader(String name, String value) {
            String newValue = calculateNewHeaderValue(name, value);
            super.setHeader(name, newValue);
        }

        public void addHeader(String name, String value) {
            String newValue = calculateNewHeaderValue(name, value);
            super.addHeader(name, newValue);
        }

        protected String calculateNewHeaderValue(String name, String value) {
            String newValue = value;
            if ( LINK_HEADER.equalsIgnoreCase(name) ) {
                if ( StringUtils.isNotEmpty(value) ) {
                    // link header can sometimes contain multiple links
                    String[] links = value.split(LINK_DELIM);
                    if ( links != null ) {
                        for (int idx=0; idx<links.length; idx++) {
                            links[idx] = calculateANewLink(links[idx], correctUrl);
                        }
                        newValue = StringUtils.join(links, LINK_DELIM);
                        LOG.debug("joined multiple 'link' values together: " + newValue);
                    }
                }
            }
            else if ( LOCATION_HEADER.equalsIgnoreCase(name) ) {
                if ( StringUtils.isNotEmpty(value) ) {
                    newValue = calculateANewLink(value, correctUrl);
                }
            }
            return newValue;
        }

        protected String calculateANewLink(String value, String correctUrl) {
            String newValue = value;
            Matcher matcher = HOSTNAME_PATTERN.matcher(value);
            if ( matcher.matches() ) {
                newValue = matcher.replaceFirst("$1" + correctUrl + "/$5");
            }
            return newValue;
        }
    }
}