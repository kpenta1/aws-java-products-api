package com.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.serverless.external.portsp.CompressionAlgorithm;
import com.serverless.external.portsp.DownloadBundle;
import com.serverless.external.portsp.DownloadHelper;
import com.serverless.external.portsp.DownloadSpecification;
import com.serverless.external.portsp.exception.CryptoException;
import com.serverless.external.portsp.exception.HttpResponseException;
import com.serverless.external.portsp.exception.MissingCharsetException;
import com.serverless.external.portsp.impl.AESCryptoStreamFactory;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GetReportHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    private final Logger logger = Logger.getLogger(GetReportHandler.class.getName());

    Gson gson = new Gson();

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {

//        String s3url = "https://tortuga-prod-na.s3-external-1.amazonaws.com/%2FNinetyDays/amzn1.tortuga.3.50cf2cb4-5ef8-4737-bbb2-656d262eaf78.T3FC5H4LNLUVVI?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20210217T045005Z&X-Amz-SignedHeaders=host&X-Amz-Expires=300&X-Amz-Credential=AKIA5U6MO6RANYPNEUPL%2F20210217%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Signature=ede63a5dfdb4349e55972eee5123585d2388c714f690daaf0cd3623fb8815d85";
//        String key = "oqwEBfrZsCWvawe5MCkATFqinAAugr5Pe0wwU1ZiF1Y=";
//        String iv = "vvvrLs756Hp5jOQaVWBGPQ==";

        logger.log(Level.INFO, "Request: " + gson.toJson(input));

        String s3url = (String) input.get("url");
        String key = (String) input.get("key");
        String iv = (String) input.get("iv");

        final String response = decryptSPApiFile(key, iv, s3url, "AES");

        return ApiGatewayResponse.builder()
                .setStatusCode(200)
                .setObjectBody(response)
                .setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & Serverless"))
                .build();
    }

    private String decryptSPApiFile(String key,
                                    String initializationVector,
                                    String url,
                                    String compressionAlgorithm) {
        System.out.println(" \n\n\n Decrypting .....");

        logger.log(Level.INFO, "Start decrypting");
        logger.log(Level.INFO, "Key: " + key);
        logger.log(Level.INFO, "IV: " + initializationVector);
        logger.log(Level.INFO, "URL: " + url);

        final DownloadHelper downloadHelper = new DownloadHelper.Builder().build();

        AESCryptoStreamFactory aesCryptoStreamFactory =
                new AESCryptoStreamFactory.Builder(key, initializationVector).build();

        DownloadSpecification downloadSpec = new DownloadSpecification.Builder(aesCryptoStreamFactory, url)
                //.withCompressionAlgorithm(CompressionAlgorithm.fromEquivalent("GZIP"))
                .build();

        StringBuilder buffer = new StringBuilder();

        try (DownloadBundle downloadBundle = downloadHelper.download(downloadSpec)) {
            try (BufferedReader reader = downloadBundle.newBufferedReader()) {
                String line;
                do {
                    line = reader.readLine();
                    logger.log(Level.INFO, " Reading line: " + line);
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
