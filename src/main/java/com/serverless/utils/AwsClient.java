package com.serverless.utils;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

import com.google.common.base.Supplier;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import vc.inreach.aws.request.AWSSigner;
import vc.inreach.aws.request.AWSSigningRequestInterceptor;

public class AwsClient {

    private static Gson gson = new Gson();

    public static String getRequest(String getUrl,
                                    String accessKey,
                                    String secretKey,
                                    String serviceName,
                                    String region,
                                    String spApiAccessToken) {
        HttpGet httpGet = new HttpGet(getUrl);

        httpGet.addHeader("x-amz-access-token", spApiAccessToken);

        return execute(httpGet, accessKey, secretKey, serviceName, region);
    }


    public static String postRequest(String postUrl,
                                     String accessKey,
                                     String secretKey,
                                     String serviceName,
                                     String region,
                                     String spApiAccessToken,
                                     String postRequestBody) {

        System.out.println(" Posting to url: " + postUrl);

        HttpPost httpPost = new HttpPost(postUrl);
        httpPost.setHeader("Content-type", "application/json");

        httpPost.addHeader("x-amz-access-token", spApiAccessToken);

        try {
            httpPost.setEntity(new StringEntity(postRequestBody));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error when encoding post url parameters");
        }

        return execute(httpPost, accessKey, secretKey, serviceName, region);
    }

    // https://github.com/inreachventures/aws-signing-request-interceptor
    private static CloseableHttpClient getClientWithSignRequest(String serviceName,
                                                         String region,
                                                         String accessKey,
                                                         String secretKey) {

        final BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        final Supplier<LocalDateTime> clock = () -> LocalDateTime.now(ZoneOffset.UTC);
        final AWSSigner awsSigner = new AWSSigner(
                new AWSStaticCredentialsProvider(awsCredentials), region, serviceName, clock);

        HttpRequestInterceptor interceptor = new AWSSigningRequestInterceptor(awsSigner);

        return HttpClients.custom()
                .addInterceptorLast(interceptor)
                .build();
    }

    private static String execute(HttpUriRequest httpReq,
                           String accessKey,
                           String secretKey,
                           String serviceName,
                           String region) {
        try {
            final CloseableHttpClient client = getClientWithSignRequest(serviceName, region, accessKey, secretKey);

            CloseableHttpResponse response = client.execute(httpReq);

            final String responseString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

            System.out.println("Received post response: " + responseString);

            return responseString;
        } catch (IOException e) {
            System.out.println("ERROR HTTP request call: " + gson.toJson(e));
            throw new RuntimeException("ERROR when HTTP request call");
        }
    }
}
