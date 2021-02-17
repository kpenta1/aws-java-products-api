package com.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Collections;
import java.util.Map;

public class CreateReportHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        return ApiGatewayResponse.builder()
                .setStatusCode(200)
                .setObjectBody("{ \"response\": \"create response\" }")
                .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                .build();
    }
}
