package com.serverless;

import static com.serverless.utils.RefreshSpApiAccessToken.getRefreshAccessToken;
import static com.serverless.utils.SpApiResponseParser.parseGetDocumentResponse;
import static java.util.logging.Level.INFO;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.serverless.external.portsp.DownloadBundle;
import com.serverless.external.portsp.DownloadHelper;
import com.serverless.external.portsp.DownloadSpecification;
import com.serverless.external.portsp.exception.CryptoException;
import com.serverless.external.portsp.exception.HttpResponseException;
import com.serverless.external.portsp.exception.MissingCharsetException;
import com.serverless.external.portsp.impl.AESCryptoStreamFactory;
import com.serverless.utils.AwsClient;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetReportHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    private final Logger logger = Logger.getLogger(GetReportHandler.class.getName());

    private Gson gson = new Gson();

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {

        final String refreshToken = System.getenv("refreshToken");
        final String clientId = System.getenv("clientId");
        final String clientSecret = System.getenv("clientSecret");
        final String oauthUrl = System.getenv("oauthUrl");

        final String spApiAccessKey = System.getenv("spApiAwsAccountAccessKey");
        final String spApiSecretKey = System.getenv("spApiAwsAccountSecretKey");
        final String spApiServiceName = System.getenv("spApiServiceName");
        final String spApiRegion = System.getenv("spApiRegion");
        final String spApiBaseUrl = System.getenv("spApiBaseUrl");

        logger.log(INFO, "Request: " + gson.toJson(input));

        final String accessToken = getRefreshAccessToken(oauthUrl, refreshToken, clientId, clientSecret);
        logger.log(INFO, "Refresh token: " + accessToken);

        final String getReportResponse = AwsClient.getRequest(
                spApiBaseUrl + "documents/amzn1.tortuga.3.50cf2cb4-5ef8-4737-bbb2-656d262eaf78.T3FC5H4LNLUVVI",
                spApiAccessKey,
                spApiSecretKey,
                spApiServiceName,
                spApiRegion,
                accessToken
        );
        logger.log(INFO, "Get report response: " + getReportResponse);

        final Map<String, String> getDocumentResponseMap = parseGetDocumentResponse(getReportResponse);

        final String response = decryptSPApiFile(
                getDocumentResponseMap.get("key"),
                getDocumentResponseMap.get("initializationVector"),
                getDocumentResponseMap.get("s3url"));

        return ApiGatewayResponse.builder()
                .setStatusCode(200)
                .setObjectBody(response)
                .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                .build();
    }

    private String decryptSPApiFile(String key,
                                    String initializationVector,
                                    String url) {
        System.out.println(" \n\n\n Decrypting .....");

        logger.log(INFO, "Start decrypting");
        logger.log(INFO, "Key: " + key);
        logger.log(INFO, "IV: " + initializationVector);
        logger.log(INFO, "URL: " + url);

        final DownloadHelper downloadHelper = new DownloadHelper.Builder().build();

        AESCryptoStreamFactory aesCryptoStreamFactory =
                new AESCryptoStreamFactory.Builder(key, initializationVector).build();

        DownloadSpecification downloadSpec = new DownloadSpecification.Builder(aesCryptoStreamFactory, url)
                .build();

        StringBuilder buffer = new StringBuilder();

        try (DownloadBundle downloadBundle = downloadHelper.download(downloadSpec)) {
            try (BufferedReader reader = downloadBundle.newBufferedReader()) {
                String line;
                do {
                    line = reader.readLine();
                    logger.log(INFO, " Reading line: " + line);
                    buffer.append(line);
                    System.out.println(line);
                } while (line != null);
            }
        }
        catch (HttpResponseException | IOException | MissingCharsetException | CryptoException e) {
            // Handle exception.
            logger.log(Level.SEVERE, "Error");
            logger.log(Level.SEVERE, e.getMessage());
            System.out.println(e);
            System.out.println(gson.toJson(e));
        }

        return buffer.toString();
    }
}
