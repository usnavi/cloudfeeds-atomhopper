package com.rackspace.feeds.filter;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.xpath.XPathEvaluator;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This filter is to prevent observers reading an individual tenanted-entry from viewing other tenant's entries.
 *
 * If the request matches the entries URL pattern & is a 200, this filter verifies that the tenant id provided in the
 * tenanted-URI matches the tenant id for the entry.  If not, a 404 is returned.
 *
 * This filter has to be in the filter chain before TenantedFilter to be able to read tenantId from the request.
 */
public class TenantedEntryVerificationFilter implements Filter {

    public static final String notFound = "Resource not found.";

    public static final String internalError = "Internal Error: " +  TenantedEntryVerificationFilter.class.getName() + ": ";

    private static final ObjectPool<XPathExpression> tidPool = new GenericObjectPool<XPathExpression>( new TidXPathPooledObjectFactory<XPathExpression>() );

    private static final ObjectPool<XPathExpression> privatePool = new GenericObjectPool<XPathExpression>( new PrivateXPathPooledObjectFactory<XPathExpression>() );

    static final private Pattern patTidEntry = Pattern.compile( ".+/events/([^/?]+)/entries/[^?]+" );

    public static String getErrorMessage( int code, String mesg ) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<error xmlns=\"http://abdera.apache.org\">\n" +
                "<code>" + code + "</code>\n" +
                "<message>" + StringEscapeUtils.escapeXml(mesg) + "</message>\n" +
                "</error>";
    }

    // Following is specific to Saxon. We chose to do this because Saxon9 supports XPath 2.0.
    private static XPathEvaluator xPathEvaluator = new XPathEvaluator();

    private static final Logger LOG = LoggerFactory.getLogger(TenantedEntryVerificationFilter.class);

    @Override
    public void init( FilterConfig filterConfig ) throws ServletException {
        LOG.debug("initializing " + TenantedEntryVerificationFilter.class.getName());
    }

    @Override
    public void doFilter( ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain )
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        ResponseWrapper responseWrapper = new ResponseWrapper( (HttpServletResponse) servletResponse );

        // get tid before translation rips it out of the request
        String tid = getTenantIdFromRequestURI(httpServletRequest.getRequestURI());

        filterChain.doFilter(servletRequest, responseWrapper);

        String originalResponseContent = responseWrapper.getContent();

        if ( tid != null
                && StringUtils.isNotEmpty(originalResponseContent)
                && responseWrapper.getStatus() == HttpServletResponse.SC_OK) {

            validateEntryAndUpdateResponse(servletResponse, tid, originalResponseContent);
        } else {
            // copy original response as is
            LOG.debug("Skipping tenant id corresponding to entry validation cuz of non-tenantId request or error/empty response");
            setResponseContent(originalResponseContent, servletResponse);
        }
    }

    private String getTenantIdFromRequestURI(String url) {

        String tenantId = null;
        Matcher matcher = patTidEntry.matcher(url);
        if ( matcher.matches() ) {
            tenantId = matcher.group(1);
        }

        return tenantId;
    }

    private void validateEntryAndUpdateResponse(ServletResponse servletResponse, String tid, String originalResponseContent) throws IOException, ServletException {

        try {

            NodeInfo doc = xPathEvaluator.setSource(new SAXSource(new InputSource(new StringReader(originalResponseContent))));

            String contentTid = getTenantIdFromResponse(doc);

            // if no match return 404 & insert error message
            if ( !contentTid.equals( "tid:" + tid ) ) {
                LOG.debug(String.format("Tenant id mismatch. tenantIdFromRequest: %s tenantIdFromResponse: %s", tid, contentTid));
                setErrorResponse(servletResponse, HttpServletResponse.SC_NOT_FOUND, notFound);
                return;
            }

            // if private cat exists, insert error
            if( isPrivateEvent(doc) ) {
                LOG.debug("private category event requested for tenantID: " + tid);
                setErrorResponse(servletResponse, HttpServletResponse.SC_NOT_FOUND, notFound);
                return;
            }

            // copy original response as validation was successful.
            setResponseContent(originalResponseContent, servletResponse);

        } catch (Exception e) {
            // if internal error, report as such
            setErrorResponse(servletResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, internalError + e.getMessage());
            LOG.error( internalError, e );
        }
    }

    /**
     * Extracts tenant id from response which will be present in the response as shown below..
     *
     * <atom:category term="tid:<tenantid>"/>
     *
     * @param doc
     * @return contentTid which is a string with text "tid:<tenantid>"
     * @throws Exception
     */
    private String getTenantIdFromResponse(NodeInfo doc) throws Exception {

        XPathExpression tidPath = tidPool.borrowObject();
        String contentTid = tidPath.evaluate( doc );
        tidPool.returnObject( tidPath );

        return contentTid;
    }

    /**
     * Verifies if the event is private by verifying the response for the presence of this category element.
     *
     * <atom:category term="cloudfeeds:private"/>
     *
     * @param doc
     * @return true/false if the event has private category or not.
     * @throws Exception
     */
    private boolean isPrivateEvent(NodeInfo doc) throws Exception {

        XPathExpression privPath = privatePool.borrowObject();
        boolean privEvent = !privPath.evaluate( doc ).isEmpty();
        privatePool.returnObject( privPath );

        return privEvent;
    }

    private void setErrorResponse(ServletResponse servletResponse, int statusCode, String message) throws IOException {
        LOG.debug(String.format("Changing status code to %s because of the error message %s", statusCode, message));
        ((HttpServletResponse) servletResponse).setStatus(statusCode);
        setResponseContent(getErrorMessage(statusCode, message), servletResponse);
    }

    private void setResponseContent(String responseContent, ServletResponse servletResponse) throws IOException {
        servletResponse.getWriter().write(responseContent);
    }

    @Override
    public void destroy() {
    }

    static class PrivateXPathPooledObjectFactory<XPathExpression> extends BasePooledObjectFactory<XPathExpression> {

        @Override
        public XPathExpression create() throws XPathExpressionException {
            synchronized ( this ) {
                //In Xpath 2.0 use *:localname to select nodes with a given local name, regardless of namespace
                return (XPathExpression)xPathEvaluator.compile("/*:entry/*:category[@term = 'cloudfeeds:private']/@term");
            }
        }

        @Override
        public PooledObject<XPathExpression> wrap( XPathExpression xpath ) {

            return new DefaultPooledObject<XPathExpression>( xpath );
        }
    }


    static class TidXPathPooledObjectFactory<XPathExpression> extends BasePooledObjectFactory<XPathExpression> {

        @Override
        public XPathExpression create() throws XPathExpressionException {
            synchronized ( this ) {
                //In Xpath 2.0 use *:localname to select nodes with a given local name, regardless of namespace
                return (XPathExpression)xPathEvaluator.compile("/*:entry/*:category[starts-with(@term, \"tid:\")]/@term");
            }
        }

        @Override
        public PooledObject<XPathExpression> wrap( XPathExpression xpath ) {

            return new DefaultPooledObject<XPathExpression>( xpath );
        }
    }

    private class FilterServletOutputStream extends ServletOutputStream {

        private ByteArrayOutputStream stream;
        private WriteListener writeListener;

        public FilterServletOutputStream( ByteArrayOutputStream streamP ) {
            stream = streamP;
        }

        @Override
        public void write(int b) throws IOException  {
            stream.write(b);
            if (writeListener != null) {
                writeListener.notify();
            }
        }

        @Override
        public void write(byte[] b) throws IOException  {
            stream.write(b);
            if (writeListener != null) {
                writeListener.notify();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException  {
            stream.write(b,off,len);
            if (writeListener != null) {
                writeListener.notify();
            }
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            this.writeListener = writeListener;
        }
    }

    private class ResponseWrapper extends HttpServletResponseWrapper {

        private ByteArrayOutputStream stream = new ByteArrayOutputStream();
        private PrintWriter writer = new PrintWriter( stream );
        private ServletOutputStream soStream = new FilterServletOutputStream( stream );


        public String getContent() {
            return stream.toString();
        }

        public ResponseWrapper( HttpServletResponse resp ) {
            super( resp );
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return soStream;
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }
    }
}
