package com.serverless;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Request {

    private String body;
    private String signature;

    public Request() {
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getDecodedBody() {
        return new String(Base64.getDecoder().decode(getBody().getBytes(StandardCharsets.UTF_8)));
    }
}
