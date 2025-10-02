package com.shah_s.bakery_notification_service.controller;

import com.shah_s.bakery_notification_service.dto.DeviceRegistrationRequest;
import com.shah_s.bakery_notification_service.dto.DeviceTokenResponse;
import com.shah_s.bakery_notification_service.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/device-tokens")
@Validated
@Tag(name = "Device Tokens", description = "Device token management APIs for push notifications")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DeviceTokenController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceTokenController.class);

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Operation(summary = "Register device token", description = "Register a new device token for push notifications")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Device token registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Device token already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<DeviceTokenResponse> registerDeviceToken(
            @Valid @RequestBody DeviceRegistrationRequest request,
            @RequestHeader(value = "X-User-Agent", required = false) String userAgent,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Registering device token: userId={}, platform={}, requester={}",
                   request.getUserId(), request.getPlatform(), requestingUserId);

        try {
            // Set request metadata
            if (request.getUserAgent() == null && userAgent != null) {
                request.setUserAgent(userAgent);
            }
            if (request.getIpAddress() == null && clientIp != null) {
                request.setIpAddress(clientIp);
            }
            if (request.getRegisteredFrom() == null) {
                request.setRegisteredFrom("API");
            }

            DeviceTokenResponse response = deviceTokenService.registerDeviceToken(request);

            logger.info("Device token registered successfully: id={}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Failed to register device token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Update device token", description = "Update an existing device token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device token updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Device token not found")
    })
    @PutMapping("/{tokenId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<DeviceTokenResponse> updateDeviceToken(
            @Parameter(description = "Device token ID", required = true)
            @PathVariable UUID tokenId,
            @Valid @RequestBody DeviceRegistrationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Updating device token: id={}, requester={}", tokenId, requestingUserId);

        try {
            DeviceTokenResponse response = deviceTokenService.updateDeviceToken(tokenId, request);

            logger.info("Device token updated successfully: id={}", tokenId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to update device token {}: {}", tokenId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Get device token by ID", description = "Retrieve a specific device token by its ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device token found"),
        @ApiResponse(responseCode = "404", description = "Device token not found")
    })
    @GetMapping("/{tokenId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<DeviceTokenResponse> getDeviceTokenById(
            @Parameter(description = "Device token ID", required = true)
            @PathVariable UUID tokenId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting device token by ID: id={}, requester={}", tokenId, requestingUserId);

        try {
            DeviceTokenResponse response = deviceTokenService.getDeviceTokenById(tokenId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get device token {}: {}", tokenId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get device tokens by user", description = "Retrieve all device tokens for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device tokens retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (#userId.toString() == authentication.name)")
    public ResponseEntity<List<DeviceTokenResponse>> getDeviceTokensByUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting device tokens by user: userId={}, requester={}", userId, requestingUserId);

        try {
            List<DeviceTokenResponse> responses = deviceTokenService.getDeviceTokensByUser(userId);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get device tokens for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get active device tokens by user", description = "Retrieve active device tokens for a specific user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active device tokens retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/user/{userId}/active")
    @PreAuthorize("hasRole('ADMIN') or (#userId.toString() == authentication.name)")
    public ResponseEntity<List<DeviceTokenResponse>> getActiveDeviceTokensByUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting active device tokens by user: userId={}, requester={}", userId, requestingUserId);

        try {
            List<DeviceTokenResponse> responses = deviceTokenService.getActiveDeviceTokensByUser(userId);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get active device tokens for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get device tokens by platform", description = "Retrieve device tokens by platform")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device tokens retrieved successfully")
    })
    @GetMapping("/platform/{platform}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<List<DeviceTokenResponse>> getDeviceTokensByPlatform(
            @Parameter(description = "Platform (iOS, ANDROID, WEB)", required = true)
            @PathVariable String platform,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting device tokens by platform: platform={}, requester={}", platform, requestingUserId);

        try {
            List<DeviceTokenResponse> responses = deviceTokenService.getDeviceTokensByPlatform(platform);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get device tokens by platform {}: {}", platform, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get device tokens with pagination",
              description = "Retrieve device tokens for a user with pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device tokens retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/user/{userId}/paginated")
    @PreAuthorize("hasRole('ADMIN') or (#userId.toString() == authentication.name)")
    public ResponseEntity<Page<DeviceTokenResponse>> getDeviceTokensByUserPaginated(
            @Parameter(description = "User ID", required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting paginated device tokens: userId={}, page={}, size={}", userId, page, size);

        try {
            Page<DeviceTokenResponse> responses = deviceTokenService.getDeviceTokensByUser(
                userId, page, size, sortBy, sortDir);
            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            logger.error("Failed to get paginated device tokens for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Activate device token", description = "Activate a device token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device token activated successfully"),
        @ApiResponse(responseCode = "404", description = "Device token not found")
    })
    @PutMapping("/{tokenId}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> activateDeviceToken(
            @Parameter(description = "Device token ID", required = true)
            @PathVariable UUID tokenId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Activating device token: id={}, requester={}", tokenId, requestingUserId);

        try {
            deviceTokenService.activateDeviceToken(tokenId);

            Map<String, Object> response = Map.of(
                "status", "ACTIVATED",
                "tokenId", tokenId,
                "message", "Device token activated successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to activate device token {}: {}", tokenId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "ACTIVATION_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Deactivate device token", description = "Deactivate a device token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device token deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Device token not found")
    })
    @PutMapping("/{tokenId}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> deactivateDeviceToken(
            @Parameter(description = "Device token ID", required = true)
            @PathVariable UUID tokenId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Deactivating device token: id={}, requester={}", tokenId, requestingUserId);

        try {
            deviceTokenService.deactivateDeviceToken(tokenId);

            Map<String, Object> response = Map.of(
                "status", "DEACTIVATED",
                "tokenId", tokenId,
                "message", "Device token deactivated successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to deactivate device token {}: {}", tokenId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "DEACTIVATION_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Enable notifications", description = "Enable notifications for a device token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications enabled successfully"),
        @ApiResponse(responseCode = "404", description = "Device token not found")
    })
    @PutMapping("/{tokenId}/enable-notifications")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> enableNotifications(
            @Parameter(description = "Device token ID", required = true)
            @PathVariable UUID tokenId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Enabling notifications for device token: id={}, requester={}", tokenId, requestingUserId);

        try {
            deviceTokenService.enableNotifications(tokenId);

            Map<String, Object> response = Map.of(
                "status", "NOTIFICATIONS_ENABLED",
                "tokenId", tokenId,
                "message", "Notifications enabled successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to enable notifications for device token {}: {}", tokenId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "ENABLE_NOTIFICATIONS_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Disable notifications", description = "Disable notifications for a device token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications disabled successfully"),
        @ApiResponse(responseCode = "404", description = "Device token not found")
    })
    @PutMapping("/{tokenId}/disable-notifications")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> disableNotifications(
            @Parameter(description = "Device token ID", required = true)
            @PathVariable UUID tokenId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Disabling notifications for device token: id={}, requester={}", tokenId, requestingUserId);

        try {
            deviceTokenService.disableNotifications(tokenId);

            Map<String, Object> response = Map.of(
                "status", "NOTIFICATIONS_DISABLED",
                "tokenId", tokenId,
                "message", "Notifications disabled successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to disable notifications for device token {}: {}", tokenId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "DISABLE_NOTIFICATIONS_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Subscribe to topic", description = "Subscribe a device token to a notification topic")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscribed to topic successfully"),
        @ApiResponse(responseCode = "404", description = "Device token not found"),
        @ApiResponse(responseCode = "400", description = "Invalid topic ARN")
    })
    @PostMapping("/{tokenId}/subscribe")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> subscribeToTopic(
            @Parameter(description = "Device token ID", required = true)
            @PathVariable UUID tokenId,
            @Parameter(description = "Topic ARN to subscribe to", required = true)
            @RequestParam String topicArn,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Subscribing device token to topic: tokenId={}, topic={}, requester={}",
                   tokenId, topicArn, requestingUserId);

        try {
            deviceTokenService.subscribeToTopic(tokenId, topicArn);

            Map<String, Object> response = Map.of(
                "status", "SUBSCRIBED",
                "tokenId", tokenId,
                "topicArn", topicArn,
                "message", "Subscribed to topic successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to subscribe device token {} to topic {}: {}", tokenId, topicArn, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "SUBSCRIPTION_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Unsubscribe from topic", description = "Unsubscribe a device token from a notification topic")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Unsubscribed from topic successfully"),
        @ApiResponse(responseCode = "404", description = "Device token not found"),
        @ApiResponse(responseCode = "400", description = "Invalid subscription ARN")
    })
    @PostMapping("/{tokenId}/unsubscribe")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> unsubscribeFromTopic(
            @Parameter(description = "Device token ID", required = true)
            @PathVariable UUID tokenId,
            @Parameter(description = "Subscription ARN to unsubscribe from", required = true)
            @RequestParam String subscriptionArn,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Unsubscribing device token from topic: tokenId={}, subscription={}, requester={}",
                   tokenId, subscriptionArn, requestingUserId);

        try {
            deviceTokenService.unsubscribeFromTopic(tokenId, subscriptionArn);

            Map<String, Object> response = Map.of(
                "status", "UNSUBSCRIBED",
                "tokenId", tokenId,
                "subscriptionArn", subscriptionArn,
                "message", "Unsubscribed from topic successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to unsubscribe device token {} from topic {}: {}",
                        tokenId, subscriptionArn, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "UNSUBSCRIPTION_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Delete device token", description = "Delete a device token permanently")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Device token deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Device token not found")
    })
    @DeleteMapping("/{tokenId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> deleteDeviceToken(
            @Parameter(description = "Device token ID", required = true)
            @PathVariable UUID tokenId,
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Deleting device token: id={}, requester={}", tokenId, requestingUserId);

        try {
            deviceTokenService.deleteDeviceToken(tokenId);

            Map<String, Object> response = Map.of(
                "status", "DELETED",
                "tokenId", tokenId,
                "message", "Device token deleted successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to delete device token {}: {}", tokenId, e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "error", "DELETION_FAILED",
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Get device token statistics", description = "Get device token registration and usage statistics")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYTICS')")
    public ResponseEntity<Map<String, Object>> getDeviceTokenStatistics(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.debug("Getting device token statistics: requester={}", requestingUserId);

        try {
            Map<String, Object> statistics = deviceTokenService.getDeviceTokenStatistics();
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("Failed to get device token statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Validate device tokens", description = "Manually trigger validation of device tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Validation started"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/validate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<Map<String, Object>> validateDeviceTokens(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Validating device tokens: requester={}", requestingUserId);

        try {
            deviceTokenService.validateDeviceTokens();

            Map<String, Object> response = Map.of(
                "status", "VALIDATING",
                "message", "Device token validation started"
            );

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            logger.error("Failed to validate device tokens: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Cleanup device tokens", description = "Manually trigger cleanup of expired and invalid tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Cleanup started"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM')")
    public ResponseEntity<Map<String, Object>> cleanupDeviceTokens(
            @RequestHeader(value = "X-User-Id", required = false) String requestingUserId) {

        logger.info("Cleaning up device tokens: requester={}", requestingUserId);

        try {
            deviceTokenService.cleanupDeviceTokens();

            Map<String, Object> response = Map.of(
                "status", "CLEANING",
                "message", "Device token cleanup started"
            );

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            logger.error("Failed to cleanup device tokens: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Health check", description = "Check device token service health")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Device token service health check");

        try {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "device-token-service",
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "service", "device-token-service",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
