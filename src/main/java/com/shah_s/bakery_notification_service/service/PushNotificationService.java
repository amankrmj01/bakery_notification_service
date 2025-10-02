package com.shah_s.bakery_notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shah_s.bakery_notification_service.entity.DeviceToken;
import com.shah_s.bakery_notification_service.entity.Notification;
import com.shah_s.bakery_notification_service.exception.NotificationServiceException;
import com.shah_s.bakery_notification_service.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    @Autowired
    private AwsSnsService awsSnsService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${notification.push.enabled:true}")
    private Boolean pushEnabled;

    @Value("${notification.push.retry-attempts:3}")
    private Integer retryAttempts;

    @Value("${notification.push.time-to-live:3600}")
    private Integer timeToLive;

    @Value("${notification.push.default-sound:default}")
    private String defaultSound;

    @Value("${notification.push.badge-count:1}")
    private Integer badgeCount;

    // Send push notification
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendPushNotification(Notification notification) {
        if (!pushEnabled) {
            logger.warn("Push notifications are disabled");
            throw new NotificationServiceException("Push notifications are disabled");
        }

        logger.info("Sending push notification: id={}, platform={}",
                   notification.getId(), notification.getPlatform());

        try {
            // Get device token info or use provided token
            String endpointArn = notification.getSnsEndpointArn();

            if (endpointArn == null && notification.getPushToken() != null) {
                // Find device token by token value
                Optional<DeviceToken> deviceToken = deviceTokenRepository.findByDeviceToken(notification.getPushToken());
                if (deviceToken.isPresent() && deviceToken.get().canReceiveNotifications()) {
                    endpointArn = deviceToken.get().getSnsEndpointArn();
                    notification.setSnsEndpointArn(endpointArn);
                }
            }

            if (endpointArn == null) {
                throw new NotificationServiceException("No valid endpoint ARN found for push notification");
            }

            // Prepare push data
            Map<String, Object> pushData = preparePushData(notification);

            // Send via AWS SNS
            awsSnsService.sendPushNotification(
                endpointArn,
                notification.getTitle(),
                notification.getContent(),
                pushData
            );

            notification.markAsSent("sns-" + UUID.randomUUID());
            logger.info("Push notification sent successfully: {}", notification.getId());

        } catch (Exception e) {
            logger.error("Failed to send push notification {}: {}", notification.getId(), e.getMessage());

            // Mark device token as invalid if endpoint error
            if (e.getMessage().contains("Endpoint") || e.getMessage().contains("Invalid")) {
                markDeviceTokenAsInvalid(notification.getSnsEndpointArn(), e.getMessage());
            }

            throw new NotificationServiceException("Failed to send push notification: " + e.getMessage());
        }
    }

    // Send push notification to user (all devices)
    public void sendPushNotificationToUser(UUID userId, String title, String body,
                                         Map<String, Object> data) {
        logger.info("Sending push notification to user: userId={}", userId);

        try {
            List<DeviceToken> userTokens = deviceTokenRepository.findActiveTokensByUser(userId, LocalDateTime.now());

            if (userTokens.isEmpty()) {
                logger.warn("No active device tokens found for user: {}", userId);
                return;
            }

            for (DeviceToken token : userTokens) {
                try {
                    awsSnsService.sendPushNotification(token.getSnsEndpointArn(), title, body, data);
                    token.markAsUsed();
                    deviceTokenRepository.save(token);

                } catch (Exception e) {
                    logger.error("Failed to send push to device {}: {}", token.getId(), e.getMessage());
                    token.markAsInvalid(e.getMessage());
                    deviceTokenRepository.save(token);
                }
            }

            logger.info("Push notifications sent to {} devices for user: {}", userTokens.size(), userId);

        } catch (Exception e) {
            logger.error("Failed to send push notifications to user {}: {}", userId, e.getMessage());
            throw new NotificationServiceException("Failed to send push notifications to user: " + e.getMessage());
        }
    }

    // Send push notification to topic
    public void sendPushNotificationToTopic(String topicArn, String title, String body,
                                          Map<String, Object> data) {
        logger.info("Sending push notification to topic: topic={}", topicArn);

        try {
            awsSnsService.sendTopicNotification(topicArn, title, body, data);
            logger.info("Push notification sent to topic successfully: {}", topicArn);

        } catch (Exception e) {
            logger.error("Failed to send push notification to topic {}: {}", topicArn, e.getMessage());
            throw new NotificationServiceException("Failed to send push to topic: " + e.getMessage());
        }
    }

    // Send order confirmation push
    public void sendOrderConfirmationPush(UUID userId, String orderNumber, String customerName) {
        Map<String, Object> data = Map.of(
            "type", "order_confirmation",
            "orderNumber", orderNumber,
            "action", "view_order"
        );

        sendPushNotificationToUser(
            userId,
            "Order Confirmed! 🎉",
            String.format("Hi %s! Your order %s has been confirmed.", customerName, orderNumber),
            data
        );
    }

    // Send order ready push
    public void sendOrderReadyPush(UUID userId, String orderNumber, String customerName) {
        Map<String, Object> data = Map.of(
            "type", "order_ready",
            "orderNumber", orderNumber,
            "action", "view_order"
        );

        sendPushNotificationToUser(
            userId,
            "Order Ready! 🍰",
            String.format("Hi %s! Your order %s is ready for pickup.", customerName, orderNumber),
            data
        );
    }

    // Send delivery notification push
    public void sendDeliveryNotificationPush(UUID userId, String orderNumber, String driverName, String eta) {
        Map<String, Object> data = Map.of(
            "type", "delivery_update",
            "orderNumber", orderNumber,
            "driverName", driverName,
            "eta", eta,
            "action", "track_order"
        );

        sendPushNotificationToUser(
            userId,
            "Order on the way! 🚚",
            String.format("Your order %s is out for delivery with %s. ETA: %s", orderNumber, driverName, eta),
            data
        );
    }

    // Send promotional push
    public void sendPromotionalPush(UUID userId, String customerName, String promoCode, String discount) {
        Map<String, Object> data = Map.of(
            "type", "promotion",
            "promoCode", promoCode,
            "discount", discount,
            "action", "browse_menu"
        );

        sendPushNotificationToUser(
            userId,
            "Special Offer! 🎁",
            String.format("Hi %s! Get %s off with code %s. Limited time only!", customerName, discount, promoCode),
            data
        );
    }

    // Send cart abandonment push
    public void sendCartAbandonmentPush(UUID userId, String customerName, int itemCount, String cartTotal) {
        Map<String, Object> data = Map.of(
            "type", "cart_abandonment",
            "itemCount", itemCount,
            "cartTotal", cartTotal,
            "action", "view_cart"
        );

        sendPushNotificationToUser(
            userId,
            "Don't forget your treats! 🛒",
            String.format("Hi %s! You have %d items waiting in your cart (Total: %s)",
                         customerName, itemCount, cartTotal),
            data
        );
    }

    // Send new product launch push
    public void sendNewProductLaunchPush(String productName, String description, String imageUrl) {
        Map<String, Object> data = Map.of(
            "type", "new_product",
            "productName", productName,
            "description", description,
            "imageUrl", imageUrl,
            "action", "view_product"
        );

        // Send to general topic (all subscribed users)
        sendPushNotificationToTopic(
            "arn:aws:sns:us-east-1:123456789:bakery-general-notifications",
            "New Arrival! 🆕",
            String.format("Try our new %s! %s", productName, description),
            data
        );
    }

    // Send birthday wishes push
    public void sendBirthdayWishesPush(UUID userId, String customerName, String specialOffer) {
        Map<String, Object> data = Map.of(
            "type", "birthday",
            "specialOffer", specialOffer,
            "action", "browse_menu"
        );

        sendPushNotificationToUser(
            userId,
            "Happy Birthday! 🎂",
            String.format("Happy Birthday %s! Enjoy %s on us today!", customerName, specialOffer),
            data
        );
    }

    // Send loyalty points push
    public void sendLoyaltyPointsPush(UUID userId, String customerName, int pointsEarned, int totalPoints) {
        Map<String, Object> data = Map.of(
            "type", "loyalty_points",
            "pointsEarned", pointsEarned,
            "totalPoints", totalPoints,
            "action", "view_rewards"
        );

        sendPushNotificationToUser(
            userId,
            "Points Earned! ⭐",
            String.format("Hi %s! You earned %d points. Total: %d points!", customerName, pointsEarned, totalPoints),
            data
        );
    }

    // Send feedback request push
    public void sendFeedbackRequestPush(UUID userId, String customerName, String orderNumber) {
        Map<String, Object> data = Map.of(
            "type", "feedback_request",
            "orderNumber", orderNumber,
            "action", "provide_feedback"
        );

        sendPushNotificationToUser(
            userId,
            "How was your experience? ⭐",
            String.format("Hi %s! Rate your recent order %s and help us improve!", customerName, orderNumber),
            data
        );
    }

    // Send system maintenance push
    public void sendMaintenanceNotificationPush(String maintenanceTime, String duration) {
        Map<String, Object> data = Map.of(
            "type", "maintenance",
            "maintenanceTime", maintenanceTime,
            "duration", duration,
            "action", "none"
        );

        sendPushNotificationToTopic(
            "arn:aws:sns:us-east-1:123456789:bakery-general-notifications",
            "Scheduled Maintenance 🔧",
            String.format("Our app will be under maintenance on %s for %s. We'll be back soon!",
                         maintenanceTime, duration),
            data
        );
    }

    // Test push notification
    public void sendTestPushNotification(String endpointArn) {
        Map<String, Object> data = Map.of(
            "type", "test",
            "timestamp", LocalDateTime.now().toString(),
            "action", "none"
        );

        try {
            awsSnsService.sendPushNotification(
                endpointArn,
                "Test Notification 🧪",
                "This is a test push notification from Shah's Bakery!",
                data
            );

            logger.info("Test push notification sent successfully to: {}", endpointArn);

        } catch (Exception e) {
            logger.error("Failed to send test push notification: {}", e.getMessage());
            throw new NotificationServiceException("Failed to send test push notification: " + e.getMessage());
        }
    }

    // Get push service health
    public Map<String, Object> getPushServiceHealth() {
        return Map.of(
            "enabled", pushEnabled,
            "retryAttempts", retryAttempts,
            "timeToLive", timeToLive,
            "defaultSound", defaultSound,
            "badgeCount", badgeCount,
            "awsSnsConfigured", awsSnsService != null,
            "timestamp", LocalDateTime.now()
        );
    }

    // Private helper methods
    private Map<String, Object> preparePushData(Notification notification) {
        Map<String, Object> data = new HashMap<>();

        // Add basic notification info
        data.put("notificationId", notification.getId().toString());
        data.put("type", "general");
        data.put("timestamp", LocalDateTime.now().toString());

        // Add campaign info if available
        if (notification.getCampaignId() != null) {
            data.put("campaignId", notification.getCampaignId().toString());
        }

        // Add related entity info if available
        if (notification.getRelatedEntityType() != null && notification.getRelatedEntityId() != null) {
            data.put("entityType", notification.getRelatedEntityType());
            data.put("entityId", notification.getRelatedEntityId().toString());
        }

        // Parse tracking data if available
        if (notification.getTrackingData() != null) {
            try {
                Map<String, Object> trackingData = objectMapper.readValue(notification.getTrackingData(), Map.class);
                data.putAll(trackingData);
            } catch (Exception e) {
                logger.warn("Failed to parse tracking data for push notification: {}", e.getMessage());
            }
        }

        // Add platform-specific settings
        data.put("sound", defaultSound);
        data.put("badge", badgeCount);
        data.put("timeToLive", timeToLive);

        return data;
    }

    private void markDeviceTokenAsInvalid(String endpointArn, String errorMessage) {
        if (endpointArn == null) return;

        try {
            Optional<DeviceToken> tokenOpt = deviceTokenRepository.findBySnsEndpointArn(endpointArn);
            if (tokenOpt.isPresent()) {
                DeviceToken token = tokenOpt.get();
                token.markAsInvalid(errorMessage);
                deviceTokenRepository.save(token);
                logger.info("Marked device token as invalid: {}", token.getId());
            }
        } catch (Exception e) {
            logger.warn("Failed to mark device token as invalid: {}", e.getMessage());
        }
    }
}
