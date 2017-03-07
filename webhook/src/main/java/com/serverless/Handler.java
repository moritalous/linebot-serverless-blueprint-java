package com.serverless;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.linecorp.bot.client.LineSignatureValidator;
import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Request, ApiGatewayResponse> {

    private static final Logger LOG = Logger.getLogger(Handler.class);

    private static final String CHANNEL_SECRET = System.getenv("CHANNEL_SECRET");

    @Override
    public ApiGatewayResponse handleRequest(Request input, Context context) {

        // Signature validation
        // https://devdocs.line.me/ja/#webhooks
        // リクエストの送信元がLINEであることを確認するために署名検証を行わなくてはなりません。
        try {
            if (!validate(CHANNEL_SECRET, input.getDecodedBody(), input.getSignature())) {
                return ApiGatewayResponse.builder().setStatusCode(401).build();
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Base64 decode fail.");
            return ApiGatewayResponse.builder().setStatusCode(401).build();
        }

        // DynamoDB登録
        try {
            putMessage(input.getSignature(), input.getDecodedBody());
        } catch (Exception e) {
            LOG.error("DynamoDB putItem fail.");
            return ApiGatewayResponse.builder().setStatusCode(401).build();
        }

        return ApiGatewayResponse.builder().setStatusCode(200).build();
    }

    /***
     * 署名検証を行います。
     * @param channelSecret
     * @param body
     * @param signature
     * @return
     */
    private boolean validate(String channelSecret, String body, String signature) {
        LineSignatureValidator lineSignatureValidator = new LineSignatureValidator(channelSecret.getBytes());

        if (lineSignatureValidator.validateSignature(body.getBytes(StandardCharsets.UTF_8), signature)) {
            LOG.info("Validate OK");
            return true;
        } else {
            LOG.warn("Validate NG");
            return false;
        }
    }

    /**
     * DynamoDBへの登録を行います。
     *
     * @param signature
     * @param body
     * @return
     * @throws Exception
     */
    private boolean putMessage(String signature, String body) throws Exception {
        AmazonDynamoDB dynamoDb = AmazonDynamoDBClient.builder()
                .withRegion(Regions.AP_NORTHEAST_1)
                .build();

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", new AttributeValue(signature));
        item.put("message", new AttributeValue(body));
        PutItemRequest itemRequest = new PutItemRequest("LineBotDynamodb", item);
        PutItemResult result = dynamoDb.putItem(itemRequest);
        LOG.debug(result.toString());

        return true;
    }
}
