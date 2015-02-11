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
    public StringResponseWrapper(HttpServletResponse response) {
        super(response);
        stream = new ByteArrayOutputStream();
        servletOutputStream = new ServletOutputStreamWrapper(stream);
        writer = new PrintWriter(servletOutputStream);
    }

    @Override
    public PrintWriter getWriter() {
       return writer;
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return servletOutputStream;
    }

    /**
     * Return string of the response content
     * @return
     */
    public String getResponseString() {
        return stream.toString();
    }
}
