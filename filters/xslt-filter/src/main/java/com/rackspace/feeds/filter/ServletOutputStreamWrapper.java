package com.rackspace.feeds.filter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;

/**
 * User: shin4590
 * Date: 9/11/14
 */
public class ServletOutputStreamWrapper extends ServletOutputStream {

    private OutputStream outputStream;

    public ServletOutputStreamWrapper(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void write(int data) throws IOException {
        this.outputStream.write(data);
    }

    public void write(byte b[], int off, int len) throws IOException {
        this.outputStream.write(b, off, len);
    }

    public void flush() throws IOException {
        this.outputStream.flush();
    }

    public void setWriteListener(WriteListener writeListener) {
        //TODO: what to write here?
    }

    public boolean isReady() {
        return true;
    }

}
