package com.rackspace.feeds.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: shin4590
 * Date: 9/11/14
 */
public class ServletResponsePipe {

    private static Logger LOG = LoggerFactory.getLogger(ServletResponsePipe.class);

    private PipedInputStream pipedInputStream = null;
    private PipedOutputStream pipedOutputStream = null;
    private OutputStreamResponseWrapper wrappedResponse = null;

    public ServletResponsePipe(HttpServletResponse httpServletResponse) throws IOException {
        // set up Pipes to pipe the OutputStream to the InputStream/Source of the
        // Transformer.
        pipedInputStream = new PipedInputStream();
        pipedOutputStream = new PipedOutputStream(pipedInputStream);
        ServletOutputStreamWrapper sosw = new ServletOutputStreamWrapper(pipedOutputStream);
        wrappedResponse = new OutputStreamResponseWrapper(httpServletResponse, sosw);
    }

    public void doFilterAsynch(final FilterChain chain, final HttpServletRequest httpServletRequest) {

        // call the filter chain in a different thread, so the current thread
        // can start reading as soon as the child thread writes something
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute( new Runnable() {
            public void run() {
                try {
                    chain.doFilter(httpServletRequest, wrappedResponse);
                    pipedOutputStream.close();
                } catch(Exception ex) {
                    LOG.error("Got exception: ", ex);
                }
            }
        });
        executorService.shutdown();
    }

    /**
     * Must be called after you call the doFilterAsycnh() method.
     * @return
     */
    public InputStream getInputStream() {
        return pipedInputStream;
    }
}
