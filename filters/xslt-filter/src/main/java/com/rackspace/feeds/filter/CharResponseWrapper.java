package com.rackspace.feeds.filter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.PrintWriter;

/**
 * Created with IntelliJ IDEA.
 * User: shin4590
 * Date: 9/4/14
 * Time: 11:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class CharResponseWrapper extends
        HttpServletResponseWrapper {
    private CharArrayWriter output;
    public String toString() {
        return output.toString();
    }
    public CharResponseWrapper(HttpServletResponse response){
        super(response);
        output = new CharArrayWriter();
    }
    public PrintWriter getWriter(){
        return new PrintWriter(output);
    }
}
