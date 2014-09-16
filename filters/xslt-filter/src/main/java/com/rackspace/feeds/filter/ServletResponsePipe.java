package com.rackspace.feeds.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class's main purpose is to create a pipe between an HttpServletResponse's output
 * stream and a new input stream. In other words, the HttpServletResponse's output
 * stream becomes an input to something else.
 *
 * User: shin4590
 * Date: 9/11/14
 */
public class ServletResponsePipe {

    private static Logger LOG = LoggerFactory.getLogger(ServletResponsePipe.class);

    private PipedInputStream pipedInputStream = null;
    private PipedOutputStream pipedOutputStream = null;
    private OutputStreamResponseWrapper wrappedResponse = null;
    private HttpServletRequest httpServletRequest = null;

    /**
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws IOException
     */
    public ServletResponsePipe(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException {
        // set up Pipes to pipe the OutputStream to the InputStream/Source of the
        // Transformer.
        pipedInputStream = new PipedInputStream();
        pipedOutputStream = new PipedOutputStream(pipedInputStream);
        ServletOutputStreamWrapper sosw = new ServletOutputStreamWrapper(pipedOutputStream);
        wrappedResponse = new OutputStreamResponseWrapper(httpServletResponse, sosw);
        this.httpServletRequest = httpServletRequest;
    }

    /**
     * This method calls the FilterChain.doFilter() in a separate thread,
     * so the filter chain can start writing to the response without
     * blocking the caller thread from reading from the input stream
     * that is a pipe of the output stream.
     *
     * @param chain
     */
    public void doFilterAsynch(final FilterChain chain) {

        // call the filter chain in a different thread, so the current thread
        // can start reading as soon as the child thread writes something
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute( new Runnable() {
            public void run() {
                try {
                    chain.doFilter(httpServletRequest, wrappedResponse);
                } catch(Exception ex) {
                    LOG.error("Got exception: ", ex);
                } finally {
                    try {
                        pipedOutputStream.close();
                    } catch (IOException ex) {
                        LOG.error("Unable to close output stream", ex);
                    }
                }
            }
        });
        executorService.shutdown();
    }

    /**
     * Must be called after you call the doFilterAsynch() method
     * @return
     */
    public InputStream getInputStream() {
        return pipedInputStream;
    }

    public String getContentType() {
        return wrappedResponse.getContentType();
    }
}