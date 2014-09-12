package com.rackspace.feeds.filter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalHrefFilter implements Filter {

    private static Logger LOG = LoggerFactory.getLogger(ExternalHrefFilter.class);

    static final String XSLT_PATH = "/xslt/external-href.xsl";
    static final String LINK_HEADER = "link";
    static final String LOCATION_HEADER = "location";
    static final String EXTERNAL_LOC_HEADER = "x-external-loc";
    static final String LINK_DELIM = ",";

    private String correctUrl = null;

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
            byte[] bytes = Files.readAllBytes(Paths.get(envFilePath));
            String environment = new String(bytes);
            Pattern pattern = Pattern.compile(".*<externalVipURL>(.*)</externalVipURL>.*");
            Matcher matcher = pattern.matcher(environment);
            if ( matcher.find() ) {
                correctUrl = matcher.group(1);
            }
        } catch(IOException ioex) {
            throw new ServletException(ioex);
        }

    }

    public void  doFilter(ServletRequest request,
                          ServletResponse response,
                          FilterChain chain)
            throws java.io.IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String externalLocHeader = httpServletRequest.getHeader(EXTERNAL_LOC_HEADER);
        if ( StringUtils.isNotEmpty(externalLocHeader) ) {

            URLFixerResponse ufr = new URLFixerResponse(httpServletResponse, correctUrl);

            ServletResponsePipe srp = new ServletResponsePipe(ufr);
            srp.doFilterAsynch(chain, httpServletRequest);

            // prepare the real response here
            response.setContentType(ufr.getContentType());

            Map<String, Object> xsltParameters = new HashMap<String, Object>();
            xsltParameters.put("correct_url", correctUrl);
            try {
                TransformerUtils transformer = new TransformerUtils();
                transformer.doTransform(getXsltStreamSource(),
                                xsltParameters,
                                new StreamSource(srp.getInputStream()),
                                new StreamResult(httpServletResponse.getWriter()));
            } catch(TransformerException te) {
                throw new ServletException(te);
            }
        } else {
            // pass through
            chain.doFilter(request, response);
        }
    }

    public void destroy( ) {
      /* Called before the Filter instance is removed
      from service by the web container*/
    }

    protected StreamSource getXsltStreamSource()
            throws IOException {
        InputStream is = getClass().getResourceAsStream(XSLT_PATH);
        return new StreamSource(is);
    }

    /**
     * Wrapper class for HttpServletResponse that understands tenanted requests.
     * If the tenantId exists in the URI:
     * <ul>
     *     <li>it adds the tenantId back to the URI</li>
     *     <li>it removes the tenanted search from the 'search' query parameter</li>
     * </ul>
     */
    static class URLFixerResponse extends HttpServletResponseWrapper {

        private String correctUrl = null;
        private static final Pattern HOSTNAME_PATTERN = Pattern.compile("(.*)(https?)(://[^/:]+)/(.*)");

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
                newValue = matcher.replaceFirst("$1" + correctUrl + "/$4");
            }
            return newValue;
        }
    }
}