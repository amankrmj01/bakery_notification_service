package com.shah_s.bakery_notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shah_s.bakery_notification_service.dto.DeviceRegistrationRequest;
import com.shah_s.bakery_notification_service.dto.DeviceTokenResponse;
import com.shah_s.bakery_notification_service.entity.DeviceToken;
import com.shah_s.bakery_notification_service.exception.NotificationServiceException;
import com.shah_s.bakery_notification_service.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DeviceTokenService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceTokenService.class);

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private AwsSnsService awsSnsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${notification.scheduling.cleanup-days:90}")
    private Integer cleanupDays;

    // Register device token
    public DeviceTokenResponse registerDeviceToken(DeviceRegistrationRequest request) {
        logger.info("Registering device token: userId={}, platform={}", request.getUserId(), request.getPlatform());

        try {
            // Check for existing token
            Optional<DeviceToken> existingToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());

            if (existingToken.isPresent()) {
                DeviceToken token = existingToken.get();

                // Update existing token
                updateDeviceTokenFromRequest(token, request);
                token.markAsValid();
                token.refreshExpiration();

                // Update SNS endpoint if needed
                updateSnsEndpoint(token);

                token = deviceTokenRepository.save(token);
                logger.info("Device token updated: {}", token.getId());
                return DeviceTokenResponse.from(token);
            }

            // Create new token
            DeviceToken newToken = createDeviceTokenFromRequest(request);

            // Create SNS endpoint
            createSnsEndpoint(newToken);

            newToken = deviceTokenRepository.save(newToken);

            // Deactivate duplicate tokens for same user and platform
            if (newToken.getUserId() != null) {
                deviceTokenRepository.deactivateDuplicateTokens(
                    newToken.getUserId(),
                    newToken.getPlatform(),
                    newToken.getId()
                );
            }

            logger.info("Device token registered successfully: {}", newToken.getId());
            return DeviceTokenResponse.from(newToken);

        } catch (Exception e) {
            logger.error("Failed to register device token: {}", e.getMessage(), e);
            throw new NotificationServiceException("Failed to register device token: " + e.getMessage());
        }
    }

    // Update device token
    @CacheEvict(value = "device-tokens", key = "#tokenId")
    public DeviceTokenResponse updateDeviceToken(UUID tokenId, DeviceRegistrationRequest request) {
        logger.info("Updating device token: {}", tokenId);

        try {
            DeviceToken token = deviceTokenRepository.findById(tokenId)
                    .orElseThrow(() -> new NotificationServiceException("Device token not found: " + tokenId));

            // Update token fields
            updateDeviceTokenFromRequest(token, request);
            token.markAsValid();

            // Update SNS endpoint if device token changed
            if (!token.getDeviceToken().equals(request.getDeviceToken())) {
                token.setDeviceToken(request.getDeviceToken());
                updateSnsEndpoint(token);
            }

            token = deviceTokenRepository.save(token);

            logger.info("Device token updated successfully: {}", tokenId);
            return DeviceTokenResponse.from(token);

        } catch (Exception e) {
            logger.error("Failed to update device token {}: {}", tokenId, e.getMessage(), e);
            throw new NotificationServiceException("Failed to update device token: " + e.getMessage());
        }
    }

    // Get device token by ID
    @Cacheable(value = "device-tokens", key = "#tokenId")
    @Transactional(readOnly = true)
    public DeviceTokenResponse getDeviceTokenById(UUID tokenId) {
        logger.debug("Getting device token by ID: {}", tokenId);

        DeviceToken token = deviceTokenRepository.findById(tokenId)
                .orElseThrow(() -> new NotificationServiceException("Device token not found: " + tokenId));

        return DeviceTokenResponse.from(token);
    }

    // Get device tokens by user
    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getDeviceTokensByUser(UUID userId) {
        logger.debug("Getting device tokens by user: {}", userId);

        return deviceTokenRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DeviceTokenResponse::from)
                .collect(Collectors.toList());
    }

    // Get active device tokens by user
    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getActiveDeviceTokensByUser(UUID userId) {
        logger.debug("Getting active device tokens by user: {}", userId);

        List<DeviceToken> tokens = deviceTokenRepository.findActiveTokensByUser(userId, LocalDateTime.now());
        return tokens.stream()
                .map(DeviceTokenResponse::from)
                .collect(Collectors.toList());
    }

    // Get device tokens by platform
    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getDeviceTokensByPlatform(String platform) {
        logger.debug("Getting device tokens by platform: {}", platform);

        return deviceTokenRepository.findByPlatformOrderByCreatedAtDesc(platform).stream()
                .map(DeviceTokenResponse::from)
                .collect(Collectors.toList());
    }

    // Get all active device tokens
    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getAllActiveDeviceTokens() {
        logger.debug("Getting all active device tokens");

        List<DeviceToken> tokens = deviceTokenRepository.findActiveTokens(LocalDateTime.now());
        return tokens.stream()
                .map(DeviceTokenResponse::from)
                .collect(Collectors.toList());
    }

    // Get device tokens with pagination
    @Transactional(readOnly = true)
    public Page<DeviceTokenResponse> getDeviceTokensByUser(UUID userId, int page, int size, String sortBy, String sortDir) {
        logger.debug("Getting device tokens by user with pagination: userId={}, page={}, size={}", userId, page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return deviceTokenRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(DeviceTokenResponse::from);
    }

    // Activate device token
    @CacheEvict(value = "device-tokens", key = "#tokenId")
    public void activateDeviceToken(UUID tokenId) {
        logger.info("Activating device token: {}", tokenId);

        int updated = deviceTokenRepository.activateToken(tokenId);
        if (updated == 0) {
            throw new NotificationServiceException("Device token not found: " + tokenId);
        }

        logger.info("Device token activated successfully: {}", tokenId);
    }

    // Deactivate device token
    @CacheEvict(value = "device-tokens", key = "#tokenId")
    public void deactivateDeviceToken(UUID tokenId) {
        logger.info("Deactivating device token: {}", tokenId);

        int updated = deviceTokenRepository.deactivateToken(tokenId);
        if (updated == 0) {
            throw new NotificationServiceException("Device token not found: " + tokenId);
        }

        logger.info("Device token deactivated successfully: {}", tokenId);
    }

    // Enable notifications for token
    @CacheEvict(value = "device-tokens", key = "#tokenId")
    public void enableNotifications(UUID tokenId) {
        logger.info("Enabling notifications for device token: {}", tokenId);

        int updated = deviceTokenRepository.updateNotificationEnabled(tokenId, true);
        if (updated == 0) {
            throw new NotificationServiceException("Device token not found: " + tokenId);
        }

        logger.info("Notifications enabled for device token: {}", tokenId);
    }

    // Disable notifications for token
    @CacheEvict(value = "device-tokens", key = "#tokenId")
    public void disableNotifications(UUID tokenId) {
        logger.info("Disabling notifications for device token: {}", tokenId);

        int updated = deviceTokenRepository.updateNotificationEnabled(tokenId, false);
        if (updated == 0) {
            throw new NotificationServiceException("Device token not found: " + tokenId);
        }

        logger.info("Notifications disabled for device token: {}", tokenId);
    }

    // Delete device token
    @CacheEvict(value = "device-tokens", key = "#tokenId")
    public void deleteDeviceToken(UUID tokenId) {
        logger.info("Deleting device token: {}", tokenId);

        DeviceToken token = deviceTokenRepository.findById(tokenId)
                .orElseThrow(() -> new NotificationServiceException("Device token not found: " + tokenId));

        // Delete SNS endpoint if exists
        if (token.getSnsEndpointArn() != null) {
            try {
                awsSnsService.deletePlatformEndpoint(token.getSnsEndpointArn());
            } catch (Exception e) {
                logger.warn("Failed to delete SNS endpoint {}: {}", token.getSnsEndpointArn(), e.getMessage());
            }
        }

        deviceTokenRepository.delete(token);
        logger.info("Device token deleted successfully: {}", tokenId);
    }

    // Validate device tokens
    @Async
    public void validateDeviceTokens() {
        logger.debug("Validating device tokens");

        try {
            List<DeviceToken> staleTokens = deviceTokenRepository.findStaleTokens(LocalDateTime.now().minusDays(7));

            for (DeviceToken token : staleTokens) {
                try {
                    validateSingleToken(token);
                } catch (Exception e) {
                    logger.warn("Failed to validate token {}: {}", token.getId(), e.getMessage());
                }
            }

            logger.debug("Validated {} device tokens", staleTokens.size());

        } catch (Exception e) {
            logger.error("Failed to validate device tokens: {}", e.getMessage(), e);
        }
    }

    // Subscribe to topic
    public void subscribeToTopic(UUID tokenId, String topicArn) {
        logger.info("Subscribing device token to topic: tokenId={}, topic={}", tokenId, topicArn);

        try {
            DeviceToken token = deviceTokenRepository.findById(tokenId)
                    .orElseThrow(() -> new NotificationServiceException("Device token not found: " + tokenId));

            if (token.getSnsEndpointArn() == null) {
                throw new NotificationServiceException("Device token does not have SNS endpoint");
            }

            String subscriptionArn = awsSnsService.subscribeToTopic(topicArn, token.getSnsEndpointArn());

            // Update subscribed topics
            updateSubscribedTopics(token, topicArn, true);

            logger.info("Device token subscribed to topic successfully: {}", subscriptionArn);

        } catch (Exception e) {
            logger.error("Failed to subscribe to topic: {}", e.getMessage(), e);
            throw new NotificationServiceException("Failed to subscribe to topic: " + e.getMessage());
        }
    }

    // Unsubscribe from topic
    public void unsubscribeFromTopic(UUID tokenId, String subscriptionArn) {
        logger.info("Unsubscribing device token from topic: tokenId={}, subscription={}", tokenId, subscriptionArn);

        try {
            DeviceToken token = deviceTokenRepository.findById(tokenId)
                    .orElseThrow(() -> new NotificationServiceException("Device token not found: " + tokenId));

            awsSnsService.unsubscribeFromTopic(subscriptionArn);

            // Update subscribed topics (would need to parse the topic ARN from subscription)
            // updateSubscribedTopics(token, topicArn, false);

            logger.info("Device token unsubscribed from topic successfully");

        } catch (Exception e) {
            logger.error("Failed to unsubscribe from topic: {}", e.getMessage(), e);
            throw new NotificationServiceException("Failed to unsubscribe from topic: " + e.getMessage());
        }
    }

    // Get device token statistics
    @Transactional(readOnly = true)
    public Map<String, Object> getDeviceTokenStatistics() {
        logger.debug("Getting device token statistics");

        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        stats.put("totalTokens", deviceTokenRepository.count());
        stats.put("activeTokens", deviceTokenRepository.countByIsActiveTrue());
        stats.put("validTokens", deviceTokenRepository.countByIsValidTrue());
        stats.put("notificationEnabledTokens", deviceTokenRepository.countByNotificationEnabledTrue());

        // Platform statistics
        List<Object[]> platformStats = deviceTokenRepository.getActiveTokenStatisticsByPlatform();
        Map<String, Long> platformCounts = platformStats.stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("byPlatform", platformCounts);

        // Country statistics
        List<Object[]> countryStats = deviceTokenRepository.getTokenStatisticsByCountry();
        Map<String, Long> countryCounts = countryStats.stream()
                .limit(10) // Top 10 countries
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("topCountries", countryCounts);

        // App version statistics
        List<Object[]> versionStats = deviceTokenRepository.getTokenStatisticsByAppVersion();
        Map<String, Long> versionCounts = versionStats.stream()
                .limit(5) // Top 5 versions
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("topAppVersions", versionCounts);

        // Error analysis
        List<Object[]> errorStats = deviceTokenRepository.getErrorAnalysis();
        stats.put("errorAnalysis", errorStats);

        return stats;
    }

    // Cleanup expired and invalid tokens
    @Async
    public void cleanupDeviceTokens() {
        logger.info("Cleaning up device tokens");

        try {
            // Deactivate expired tokens
            int expiredCount = deviceTokenRepository.deactivateExpiredTokens(LocalDateTime.now());

            // Deactivate tokens with too many errors
            int errorCount = deviceTokenRepository.deactivateTokensWithTooManyErrors(5);

            // Delete inactive tokens older than cleanup days
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
            int inactiveCount = deviceTokenRepository.cleanupInactiveTokens(cutoffDate);
            int invalidCount = deviceTokenRepository.cleanupInvalidTokens(10, cutoffDate);

            logger.info("Device token cleanup completed: expired={}, errors={}, inactive={}, invalid={}",
                       expiredCount, errorCount, inactiveCount, invalidCount);

        } catch (Exception e) {
            logger.error("Failed to cleanup device tokens: {}", e.getMessage(), e);
        }
    }

    // Private helper methods
    private DeviceToken createDeviceTokenFromRequest(DeviceRegistrationRequest request) {
        DeviceToken token = new DeviceToken(request.getUserId(), request.getDeviceToken(), request.getPlatform());

        updateDeviceTokenFromRequest(token, request);

        return token;
    }

    private void updateDeviceTokenFromRequest(DeviceToken token, DeviceRegistrationRequest request) {
        token.setUserId(request.getUserId());
        token.setDeviceId(request.getDeviceId());
        token.setAppVersion(request.getAppVersion());
        token.setOsVersion(request.getOsVersion());
        token.setDeviceModel(request.getDeviceModel());
        token.setNotificationEnabled(request.getNotificationEnabled());
        token.setRegisteredFrom(request.getRegisteredFrom());
        token.setUserAgent(request.getUserAgent());
        token.setIpAddress(request.getIpAddress());
        token.setCountry(request.getCountry());
        token.setTimezone(request.getTimezone());

        // Convert metadata to JSON
        if (request.getMetadata() != null) {
            try {
                token.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (Exception e) {
                logger.warn("Failed to serialize device token metadata: {}", e.getMessage());
            }
        }

        // Convert topic subscriptions to JSON
        if (request.getTopicSubscriptions() != null && !request.getTopicSubscriptions().isEmpty()) {
            try {
                token.setSubscribedTopics(objectMapper.writeValueAsString(request.getTopicSubscriptions()));
            } catch (Exception e) {
                logger.warn("Failed to serialize topic subscriptions: {}", e.getMessage());
            }
        }
    }

    private void createSnsEndpoint(DeviceToken token) {
        try {
            String endpointArn = awsSnsService.createPlatformEndpoint(
                token.getDeviceToken(),
                token.getPlatform(),
                token.getUserId() != null ? token.getUserId().toString() : null
            );

            token.setSnsEndpointArn(endpointArn);
            token.markAsValid();

        } catch (Exception e) {
            logger.warn("Failed to create SNS endpoint for token {}: {}", token.getId(), e.getMessage());
            token.markAsInvalid("SNS endpoint creation failed: " + e.getMessage());
        }
    }

    private void updateSnsEndpoint(DeviceToken token) {
        try {
            if (token.getSnsEndpointArn() != null) {
                // Check if endpoint is still valid
                Map<String, String> attributes = awsSnsService.getEndpointAttributes(token.getSnsEndpointArn());

                if (attributes.isEmpty() || "false".equals(attributes.get("Enabled"))) {
                    // Endpoint is invalid, create new one
                    createSnsEndpoint(token);
                } else {
                    token.markAsValid();
                }
            } else {
                createSnsEndpoint(token);
            }

        } catch (Exception e) {
            logger.warn("Failed to update SNS endpoint for token {}: {}", token.getId(), e.getMessage());
            token.markAsInvalid("SNS endpoint update failed: " + e.getMessage());
        }
    }

    private void validateSingleToken(DeviceToken token) {
        try {
            if (token.getSnsEndpointArn() != null) {
                Map<String, String> attributes = awsSnsService.getEndpointAttributes(token.getSnsEndpointArn());

                if (attributes.isEmpty() || "false".equals(attributes.get("Enabled"))) {
                    token.markAsInvalid("SNS endpoint is disabled or not found");
                } else {
                    token.markAsValid();
                }
            } else {
                // Try to create SNS endpoint
                createSnsEndpoint(token);
            }

            deviceTokenRepository.save(token);

        } catch (Exception e) {
            logger.warn("Failed to validate token {}: {}", token.getId(), e.getMessage());
            token.markAsInvalid("Validation failed: " + e.getMessage());
            deviceTokenRepository.save(token);
        }
    }

    private void updateSubscribedTopics(DeviceToken token, String topicArn, boolean subscribe) {
        try {
            Set<String> topics = new HashSet<>();

            if (token.getSubscribedTopics() != null) {
                topics.addAll(objectMapper.readValue(token.getSubscribedTopics(), List.class));
            }

            if (subscribe) {
                topics.add(topicArn);
            } else {
                topics.remove(topicArn);
            }

            token.setSubscribedTopics(objectMapper.writeValueAsString(new ArrayList<>(topics)));
            deviceTokenRepository.save(token);

        } catch (Exception e) {
            logger.warn("Failed to update subscribed topics for token {}: {}", token.getId(), e.getMessage());
        }
    }
}
