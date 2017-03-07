package com.serverless;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.*;
import com.linecorp.bot.model.event.message.*;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import org.apache.log4j.Logger;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Handler implements RequestHandler<DynamodbEvent, ApiGatewayResponse> {

    private static final Logger LOG = Logger.getLogger(Handler.class);

    private static final String CHANNEL_SECRET = System.getenv("CHANNEL_SECRET");
    private static final String CHANNEL_ACCESS_TOKEN = System.getenv("CHANNEL_ACCESS_TOKEN");

    private static ObjectMapper buildObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Register JSR-310(java.time.temporal.*) module and read number as
        // millsec.
        objectMapper.registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        return objectMapper;
    }

    @Override
    public ApiGatewayResponse handleRequest(DynamodbEvent ddbEvent, Context context) {

        ddbEvent.getRecords().forEach(event -> {
            try {
                CallbackRequest callbackRequest = buildCallbackRequest(event);
                callbackRequest.getEvents().forEach(e -> {

                    if (e instanceof MessageEvent) {
                        MessageEvent<MessageContent> messageEvent = (MessageEvent<MessageContent>) e;
                        String replyToken = messageEvent.getReplyToken();
                        MessageContent content = messageEvent.getMessage();

                        if (content instanceof TextMessageContent) {
                            String message = ((TextMessageContent) content).getText();

                            LineMessagingService client = LineMessagingServiceBuilder.create(CHANNEL_ACCESS_TOKEN).build();

                            List<Message> replyMessages = new ArrayList<>();
                            replyMessages.add(new TextMessage(message));

                            try {
                                Response<BotApiResponse> response = client.replyMessage(new ReplyMessage(replyToken, replyMessages)).execute();
                                if (response.isSuccessful()) {
                                    LOG.info(response.message());
                                } else {
                                    LOG.warn(response.errorBody().string());
                                }
                            } catch (IOException e1) {
                                LOG.error(e1);
                            }
                        }
                        if (content instanceof ImageMessageContent) {
                        }
                        if (content instanceof LocationMessageContent) {
                        }
                        if (content instanceof AudioMessageContent) {
                        }
                        if (content instanceof VideoMessageContent) {
                        }
                        if (content instanceof StickerMessageContent) {
                        }
                        if (content instanceof FileMessageContent) {
                        } else {
                        }
                    } else if (e instanceof UnfollowEvent) {
                    } else if (e instanceof FollowEvent) {
                    } else if (e instanceof JoinEvent) {
                    } else if (e instanceof LeaveEvent) {
                    } else if (e instanceof PostbackEvent) {
                    } else if (e instanceof BeaconEvent) {
                    } else {
                    }
                });
            } catch (Exception e) {
                LOG.error(e);
            }
        });
        return ApiGatewayResponse.builder().setStatusCode(200).build();
    }

    private CallbackRequest buildCallbackRequest(DynamodbEvent.DynamodbStreamRecord record) throws IOException {
        Map<String, AttributeValue> image = record.getDynamodb().getNewImage();

        String id = image.get("id").getS();
        String message = image.get("message").getS();

        return buildObjectMapper().readValue(message, CallbackRequest.class);
    }

}
