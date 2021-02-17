package com.serverless.external.portsp.exception;

public class HttpResponseException extends Exception {
    private final String body;
    private final int code;

    public HttpResponseException(String message, String body, int code) {
        super(message);
        this.body = body;
        this.code = code;
    }

    public HttpResponseException(String message, Throwable cause, String body, int code) {
        super(message, cause);
        this.body = body;
        this.code = code;
    }

    public String getBody() {
        return this.body;
    }

    public int getCode() {
        return this.code;
    }

    public String toString() {
        return super.toString() + " {code=" + this.getCode() + ", body=" + this.getBody() + '}';
    }
}
