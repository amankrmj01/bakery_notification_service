package com.shah_s.bakery_notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class AwsSnsService {

    private static final Logger logger = LoggerFactory.getLogger(AwsSnsService.class);

    @Autowired
    private SnsClient snsClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${aws.sns.ios-platform-arn}")
    private String iosPlatformArn;

    @Value("${aws.sns.android-platform-arn}")
    private String androidPlatformArn;

    @Value("${aws.sns.general-topic-arn}")
    private String generalTopicArn;

    @Value("${notification.push.time-to-live:3600}")
    private Integer timeToLive;

    // Create platform endpoint (register device token)
    public String createPlatformEndpoint(String deviceToken, String platform, String userId) {
        try {
            String platformArn = platform.equalsIgnoreCase("ios") ? iosPlatformArn : androidPlatformArn;

            CreatePlatformEndpointRequest request = CreatePlatformEndpointRequest.builder()
                    .platformApplicationArn(platformArn)
                    .token(deviceToken)
                    .customUserData(userId)
                    .build();

            CreatePlatformEndpointResponse response = snsClient.createPlatformEndpoint(request);
            String endpointArn = response.endpointArn();

            logger.info("Created SNS endpoint for user {}: {}", userId, endpointArn);
            return endpointArn;

        } catch (Exception e) {
            logger.error("Failed to create platform endpoint for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to create platform endpoint", e);
        }
    }

    // Send push notification to specific user
    public void sendPushNotification(String endpointArn, String title, String body, Map<String, Object> data) {
        try {
            Map<String, String> messageMap = new HashMap<>();

            // Android (FCM) message
            Map<String, Object> fcmMessage = new HashMap<>();
            fcmMessage.put("notification", Map.of("title", title, "body", body));
            if (data != null && !data.isEmpty()) {
                fcmMessage.put("data", data);
            }
            messageMap.put("GCM", objectMapper.writeValueAsString(fcmMessage));

            // iOS (APNS) message
            Map<String, Object> apnsMessage = new HashMap<>();
            Map<String, Object> aps = new HashMap<>();
            aps.put("alert", Map.of("title", title, "body", body));
            aps.put("sound", "default");
            aps.put("badge", 1);
            apnsMessage.put("aps", aps);
            if (data != null && !data.isEmpty()) {
                apnsMessage.putAll(data);
            }
            messageMap.put("APNS", objectMapper.writeValueAsString(apnsMessage));

            // Default message (fallback)
            messageMap.put("default", body);

            PublishRequest request = PublishRequest.builder()
                    .targetArn(endpointArn)
                    .messageStructure("json")
                    .message(objectMapper.writeValueAsString(messageMap))
                    .build();

            PublishResponse response = snsClient.publish(request);
            logger.info("Push notification sent successfully. MessageId: {}", response.messageId());

        } catch (Exception e) {
            logger.error("Failed to send push notification to endpoint {}: {}", endpointArn, e.getMessage());
            throw new RuntimeException("Failed to send push notification", e);
        }
    }

    // Send push notification to topic (broadcast)
    public void sendTopicNotification(String topicArn, String title, String body, Map<String, Object> data) {
        try {
            Map<String, String> messageMap = new HashMap<>();

            // Android (FCM) message for topic
            Map<String, Object> fcmMessage = new HashMap<>();
            fcmMessage.put("notification", Map.of("title", title, "body", body));
            if (data != null && !data.isEmpty()) {
                fcmMessage.put("data", data);
            }
            messageMap.put("GCM", objectMapper.writeValueAsString(fcmMessage));

            // iOS (APNS) message for topic
            Map<String, Object> apnsMessage = new HashMap<>();
            Map<String, Object> aps = new HashMap<>();
            aps.put("alert", Map.of("title", title, "body", body));
            aps.put("sound", "default");
            apnsMessage.put("aps", aps);
            if (data != null && !data.isEmpty()) {
                apnsMessage.putAll(data);
            }
            messageMap.put("APNS", objectMapper.writeValueAsString(apnsMessage));

            // Default message
            messageMap.put("default", body);

            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .messageStructure("json")
                    .message(objectMapper.writeValueAsString(messageMap))
                    .subject(title)
                    .build();

            PublishResponse response = snsClient.publish(request);
            logger.info("Topic notification sent successfully. MessageId: {}", response.messageId());

        } catch (Exception e) {
            logger.error("Failed to send topic notification to {}: {}", topicArn, e.getMessage());
            throw new RuntimeException("Failed to send topic notification", e);
        }
    }

    // Subscribe user to topic
    public String subscribeToTopic(String topicArn, String endpointArn) {
        try {
            SubscribeRequest request = SubscribeRequest.builder()
                    .topicArn(topicArn)
                    .protocol("application")
                    .endpoint(endpointArn)
                    .build();

            SubscribeResponse response = snsClient.subscribe(request);
            String subscriptionArn = response.subscriptionArn();

            logger.info("User subscribed to topic. SubscriptionArn: {}", subscriptionArn);
            return subscriptionArn;

        } catch (Exception e) {
            logger.error("Failed to subscribe to topic {}: {}", topicArn, e.getMessage());
            throw new RuntimeException("Failed to subscribe to topic", e);
        }
    }

    // Unsubscribe user from topic
    public void unsubscribeFromTopic(String subscriptionArn) {
        try {
            UnsubscribeRequest request = UnsubscribeRequest.builder()
                    .subscriptionArn(subscriptionArn)
                    .build();

            snsClient.unsubscribe(request);
            logger.info("User unsubscribed from topic. SubscriptionArn: {}", subscriptionArn);

        } catch (Exception e) {
            logger.error("Failed to unsubscribe from topic {}: {}", subscriptionArn, e.getMessage());
            throw new RuntimeException("Failed to unsubscribe from topic", e);
        }
    }

    // Delete platform endpoint
    public void deletePlatformEndpoint(String endpointArn) {
        try {
            DeleteEndpointRequest request = DeleteEndpointRequest.builder()
                    .endpointArn(endpointArn)
                    .build();

            snsClient.deleteEndpoint(request);
            logger.info("Platform endpoint deleted: {}", endpointArn);

        } catch (Exception e) {
            logger.error("Failed to delete platform endpoint {}: {}", endpointArn, e.getMessage());
            throw new RuntimeException("Failed to delete platform endpoint", e);
        }
    }

    // Get endpoint attributes (check if valid)
    public Map<String, String> getEndpointAttributes(String endpointArn) {
        try {
            GetEndpointAttributesRequest request = GetEndpointAttributesRequest.builder()
                    .endpointArn(endpointArn)
                    .build();

            GetEndpointAttributesResponse response = snsClient.getEndpointAttributes(request);
            return response.attributes();

        } catch (Exception e) {
            logger.error("Failed to get endpoint attributes for {}: {}", endpointArn, e.getMessage());
            return new HashMap<>();
        }
    }
}
