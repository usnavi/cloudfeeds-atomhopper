package com.rackspace.feeds.filter.mockito;

import org.hamcrest.Description
import org.mockito.ArgumentMatcher

public class NotContains extends ArgumentMatcher<String> implements Serializable {

    private static final long serialVersionUID = -1909837398271763801L;
    private final String substring;

    public NotContains(String substring) {
        this.substring = substring;
    }

    public boolean matches(Object actual) {
        return actual != null && !((String) actual).contains(substring);
    }

    public void describeTo(Description description) {
        description.appendText("notcontains(\"" + substring + "\")");
    }
}
