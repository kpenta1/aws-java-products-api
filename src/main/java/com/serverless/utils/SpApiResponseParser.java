package com.serverless.utils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

import java.util.Map;

public class SpApiResponseParser {

    private static Gson gson = new Gson();

    public static Map<String, String> parseGetDocumentResponse(String json) {
        final Map<String, Object> map = gson.fromJson(json, Map.class);

        final Map<String, Object> payloadMap = (Map<String, Object>) map.get("payload");

        final Map<String, Object> encryptionDetails = (Map<String, Object>) payloadMap.get("encryptionDetails");

        final String iv = (String) encryptionDetails.get("initializationVector");
        final String key = (String) encryptionDetails.get("key");
        final String url = (String) payloadMap.get("url");

        return ImmutableMap.of(
                "initializationVector", iv,
                "key", key,
                "s3url", url
        );
    }
}
