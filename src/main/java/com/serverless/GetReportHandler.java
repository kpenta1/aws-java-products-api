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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GetReportHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    private final Logger logger = Logger.getLogger(GetReportHandler.class.getName());

    private Gson gson = new Gson();

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        logger.log(INFO, "Request: " + gson.toJson(input));

        final String refreshToken = System.getenv("refreshToken");
        final String clientId = System.getenv("clientId");
        final String clientSecret = System.getenv("clientSecret");
        final String oauthUrl = System.getenv("oauthUrl");

        final String spApiAccessKey = System.getenv("spApiAwsAccountAccessKey");
        final String spApiSecretKey = System.getenv("spApiAwsAccountSecretKey");
        final String spApiServiceName = System.getenv("spApiServiceName");
        final String spApiRegion = System.getenv("spApiRegion");
        final String spApiBaseUrl = System.getenv("spApiBaseUrl");

        final String accessToken = getRefreshAccessToken(oauthUrl, refreshToken, clientId, clientSecret);
        logger.log(INFO, "Refresh token: " + accessToken);

        final String reportId = "53434018674";
        final Map<String, Object> reportStatusMap = getReportStatus(
                reportId, spApiBaseUrl, spApiAccessKey, spApiSecretKey, spApiServiceName, spApiRegion, accessToken);

        final String processingStatus = (String) reportStatusMap.get("processingStatus");
        logger.log(INFO, " Processing status: " + processingStatus);

        if (!"DONE".equals(processingStatus)) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("IsComplete", false);

            return ApiGatewayResponse.builder()
                    .setStatusCode(200)
                    .setObjectBody(responseMap)
                    .build();
        }

        final String documentId = (String) reportStatusMap.get("reportDocumentId");

        final List<Map<String, String>> reportDocumentData = getReportDocumentData(
                spApiBaseUrl, spApiAccessKey, spApiSecretKey, spApiServiceName, spApiRegion, accessToken, documentId);

        return ApiGatewayResponse.builder()
                .setStatusCode(200)
                .setObjectBody(reportDocumentData)
                .build();
    }

    private Map<String, Object> getReportStatus(String reportId,
                                              String spApiBaseUrl,
                                              String spApiAccessKey,
                                              String spApiSecretKey,
                                              String spApiServiceName,
                                              String spApiRegion,
                                              String accessToken) {
        final String reportStatusResponse = AwsClient.getRequest(
                spApiBaseUrl + "reports/" + reportId,
                spApiAccessKey,
                spApiSecretKey,
                spApiServiceName,
                spApiRegion,
                accessToken
        );
        logger.log(INFO, "Get report status response: " + reportStatusResponse);

        if (reportStatusResponse.contains("errors")) {
            throw new RuntimeException("Sp-Api returns error response: " + reportStatusResponse);
        }

        final Map<String, Object> statusMap = gson.fromJson(reportStatusResponse, Map.class);

        final Map<String, Object> payloadMap = (Map<String, Object>) statusMap.get("payload");

        return payloadMap;
    }

    private List<Map<String, String>> getReportDocumentData(String spApiBaseUrl,
                                                            String spApiAccessKey,
                                                            String spApiSecretKey,
                                                            String spApiServiceName,
                                                            String spApiRegion,
                                                            String accessToken,
                                                            String documentId) {
        final String getReportResponse = AwsClient.getRequest(
                spApiBaseUrl + "documents/" + documentId,
                spApiAccessKey,
                spApiSecretKey,
                spApiServiceName,
                spApiRegion,
                accessToken
        );
        logger.log(INFO, "Get report document response: " + getReportResponse);

        final Map<String, String> getDocumentResponseMap = parseGetDocumentResponse(getReportResponse);

        final List<Map<String, String>> response = decryptSPApiFile(
                getDocumentResponseMap.get("key"),
                getDocumentResponseMap.get("initializationVector"),
                getDocumentResponseMap.get("s3url"));

        return response;
    }

    private List<Map<String, String>> decryptSPApiFile(String key,
                                                       String initializationVector,
                                                       String url) {
        logger.log(INFO, "Start decrypting ..");
        logger.log(INFO, "Key: " + key);
        logger.log(INFO, "IV: " + initializationVector);
        logger.log(INFO, "URL: " + url);

        final DownloadHelper downloadHelper = new DownloadHelper.Builder().build();

        AESCryptoStreamFactory aesCryptoStreamFactory =
                new AESCryptoStreamFactory.Builder(key, initializationVector).build();

        DownloadSpecification downloadSpec = new DownloadSpecification.Builder(aesCryptoStreamFactory, url)
                .build();


        List<Map<String, String>> resultList = new ArrayList<>();

        List<String> headers = new ArrayList<>();
        int count = 0;

        try (DownloadBundle downloadBundle = downloadHelper.download(downloadSpec)) {
            try (BufferedReader reader = downloadBundle.newBufferedReader()) {
                String line;
                do {
                    line = reader.readLine();
                    if (null != line) {
                        logger.log(INFO, " Reading line: " + line);
                        final String[] attributeValues = line.split("\\t");
                        logger.log(INFO, " Size: " + attributeValues.length);
                        if (count == 0) {
                            headers = Arrays.asList(attributeValues);
                        } else {
                            final Iterator<String> headersIter = headers.iterator();
                            final Iterator<String> values = Arrays.asList(attributeValues).iterator();
                            final Map<String, String> mergedData = IntStream.range(0, headers.size()).boxed()
                                    .collect(Collectors.toMap(_i -> headersIter.next(), _i -> values.next()));
                            resultList.add(mergedData);
                        }
                        count++;
                    }
                } while (line != null);
            }
        }
        catch (HttpResponseException | IOException | MissingCharsetException | CryptoException e) {
            // Handle exception.
            logger.log(Level.SEVERE, "Error");
            logger.log(Level.SEVERE, e.getMessage());
            System.out.println(gson.toJson(e));
            throw new RuntimeException("Error downloading/decrypting sp-api data.");
        }

        return resultList;
    }
}
