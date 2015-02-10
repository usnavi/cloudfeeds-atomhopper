package com.rackspace.feeds.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * Wrapper for HttpServletResponse that collect the response output
 * that can be retrieved as string
 */
public class StringResponseWrapper extends HttpServletResponseWrapper {
    private PrintWriter writer = null;
    private ByteArrayOutputStream stream;
    private ServletOutputStreamWrapper servletOutputStream;

    /**
     * Create a wrapper for servlet response
     * @param response
     */
    public StringResponseWrapper(HttpServletResponse response){
        super(response);
        this.stream = new ByteArrayOutputStream();
        this.servletOutputStream = new ServletOutputStreamWrapper(stream);
    }

    @Override
    public PrintWriter getWriter() {
        if ( writer != null ) {
            return writer;
        }
        writer = new PrintWriter(servletOutputStream);
        return writer;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        if ( writer != null ) {
            throw new IllegalStateException("getWriter() has already been called");
        }
        return servletOutputStream;
    }

    /**
     * Return string of the response content
     * @return
     */
    public String getResponseString() {
        return this.stream.toString();
    }
}
