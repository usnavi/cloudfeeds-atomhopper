package com.rackspace.feeds.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.PrintWriter;

/**
 * User: shin4590
 * Date: 9/11/14
 */
public class OutputStreamResponseWrapper extends HttpServletResponseWrapper {

    private PrintWriter writer = null;
    private ServletOutputStream outputStream = null;

    public OutputStreamResponseWrapper(HttpServletResponse response, ServletOutputStream outputStream){
        super(response);
        this.outputStream = outputStream;
    }

    @Override
    public PrintWriter getWriter() {
        if ( writer != null ) {
            return writer;
        }
        writer = new PrintWriter(outputStream);
        return writer;
    }

    public ServletOutputStream getOutputStream() {
        if ( writer != null ) {
            throw new IllegalStateException("getWriter() has already been called");
        }
        return outputStream;
    }

}
