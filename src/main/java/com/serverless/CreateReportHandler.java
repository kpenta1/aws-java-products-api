package com.serverless;

import static com.serverless.utils.RefreshSpApiAccessToken.getRefreshAccessToken;
import static java.util.logging.Level.INFO;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.StringUtils;
import com.serverless.utils.AwsClient;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class CreateReportHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    private final Logger logger = Logger.getLogger(CreateReportHandler.class.getName());

    private Gson gson = new Gson();

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {

        logger.log(INFO, "Request: " + gson.toJson(input));

        final String reportType = (String) input.get("reportType");
        final List<String> marketplaceIds = (List<String>) input.get("marketplaceIds");

        validateInput(reportType, marketplaceIds);

        Map<String, Object> httpBodyParams = new HashMap<>();
        httpBodyParams.put("reportType", reportType);
        httpBodyParams.put("marketplaceIds", marketplaceIds);

        final String createReportApiResponse = callCreateReportApi(httpBodyParams);
        final Map createReportApiResponseMap = gson.fromJson(createReportApiResponse, Map.class);
        final Map<String, Object> payload = (Map<String, Object>) createReportApiResponseMap.get("payload");

        return ApiGatewayResponse.builder()
                .setStatusCode(200)
                .setObjectBody(payload)
                .build();
    }

    private String callCreateReportApi(Map<String, Object> httpBodyParams) {
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


        final String httpPostBodyJson = gson.toJson(httpBodyParams);
        logger.log(INFO, "Post body parameters: " + httpPostBodyJson);

        final String postReportResponse = AwsClient.postRequest(
                spApiBaseUrl + "reports",
                spApiAccessKey,
                spApiSecretKey,
                spApiServiceName,
                spApiRegion,
                accessToken,
                httpPostBodyJson
        );
        logger.log(INFO, "Create report response: " + postReportResponse);

        return postReportResponse;
    }

    private void validateInput(String reportType,
                               List<String> ids) {
        if (StringUtils.isNullOrEmpty(reportType)) {
            throw new IllegalArgumentException("Input field `reportType` cannot be empty");
        }

        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Input field `marketplaceIds` cannot be empty");
        }
    }
}
