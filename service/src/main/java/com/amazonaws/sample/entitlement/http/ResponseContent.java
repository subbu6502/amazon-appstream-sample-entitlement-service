package com.amazonaws.sample.entitlement.http;


public class ResponseContent {

    private int statusCode;
    private String content;

    public ResponseContent(int statusCode, String content) {
        this.statusCode = statusCode;
        this.content = content;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContent(String defaultValue) {
        if (content == null) {
            return defaultValue;
        }
        return content;
    }

}
