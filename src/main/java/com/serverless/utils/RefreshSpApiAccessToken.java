package com.serverless.utils;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

public class RefreshSpApiAccessToken {

    private static Gson gson = new Gson();

    public static String getRefreshAccessToken(final String url,
                                               final String refresh_token_value,
                                               final String clientId,
                                               final String clientSecret) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();

            HttpPost httpPost = new HttpPost(url);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("grant_type", "refresh_token"));
            params.add(new BasicNameValuePair("refresh_token", refresh_token_value));
            params.add(new BasicNameValuePair("client_id", clientId));
            params.add(new BasicNameValuePair("client_secret", clientSecret));

            httpPost.setEntity(new UrlEncodedFormEntity(params));

            CloseableHttpResponse response = client.execute(httpPost);

            final String responseString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

            System.out.println("Received access token response: " + responseString);

            final Map<String, Object> responseTokenMap = gson.fromJson(responseString, Map.class);

            return (String) responseTokenMap.get("access_token");
        } catch (IOException e) {
            System.out.println("ERROR getting access token: " + gson.toJson(e));
            throw new RuntimeException("Error getting refresh token");
        }
    }
}
