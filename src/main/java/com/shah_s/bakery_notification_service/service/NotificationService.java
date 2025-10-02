
package com.shah_s.bakery_notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shah_s.bakery_notification_service.dto.NotificationResponse;
import com.shah_s.bakery_notification_service.dto.SendNotificationRequest;
import com.shah_s.bakery_notification_service.entity.Notification;
import com.shah_s.bakery_notification_service.entity.NotificationTemplate;
import com.shah_s.bakery_notification_service.exception.NotificationServiceException;
import com.shah_s.bakery_notification_service.repository.NotificationRepository;
import com.shah_s.bakery_notification_service.repository.NotificationTemplateRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${notification.rate-limit.email-per-minute:100}")
    private Integer emailRateLimit;

    @Value("${notification.rate-limit.sms-per-minute:50}")
    private Integer smsRateLimit;

    @Value("${notification.rate-limit.push-per-minute:200}")
    private Integer pushRateLimit;

    @Value("${notification.scheduling.cleanup-days:90}")
    private Integer cleanupDays;

    // Send notification
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        logger.info("Sending notification: type={}, userId={}, title={}",
                   request.getType(), request.getUserId(), request.getTitle());

        try {
            // Create notification entity
            Notification notification = createNotificationFromRequest(request);

            // Apply template if specified
            if (request.getTemplateId() != null) {
                applyTemplate(notification, request.getTemplateId(), request.getTemplateVariables());
            }

            // Validate notification
            validateNotification(notification);

            // Check for duplicates
            if (isDuplicateNotification(notification)) {
                logger.warn("Duplicate notification detected and skipped: {}", notification.getTitle());
                throw new NotificationServiceException("Duplicate notification detected");
            }

            // Save notification
            notification = notificationRepository.save(notification);

            // Send immediately if not scheduled
            if (notification.shouldSendNow()) {
                sendNotificationNow(notification);
            }

            logger.info("Notification created successfully: {}", notification.getId());
            return NotificationResponse.from(notification);

        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage(), e);
            throw new NotificationServiceException("Failed to send notification: " + e.getMessage());
        }
    }

    // Send bulk notifications
    @Async
    public CompletableFuture<List<NotificationResponse>> sendBulkNotifications(
            List<SendNotificationRequest> requests) {
        logger.info("Sending bulk notifications: count={}", requests.size());

        try {
            List<NotificationResponse> responses = new ArrayList<>();

            for (SendNotificationRequest request : requests) {
                try {
                    NotificationResponse response = sendNotification(request);
                    responses.add(response);
                } catch (Exception e) {
                    logger.error("Failed to send notification in bulk: {}", e.getMessage());
                    // Continue with other notifications
                }
            }

            logger.info("Bulk notifications completed: sent={}/{}", responses.size(), requests.size());
            return CompletableFuture.completedFuture(responses);

        } catch (Exception e) {
            logger.error("Failed to send bulk notifications: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    // Get notification by ID
    @Cacheable(value = "notifications", key = "#notificationId")
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID notificationId) {
        logger.debug("Getting notification by ID: {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationServiceException("Notification not found: " + notificationId));

        return NotificationResponse.from(notification);
    }

    // Get notifications by user
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUser(UUID userId) {
        logger.debug("Getting notifications by user: {}", userId);

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    // Get notifications by user with pagination
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByUser(UUID userId, int page, int size, String sortBy, String sortDir) {
        logger.debug("Getting notifications by user with pagination: userId={}, page={}, size={}", userId, page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    // Get notifications by status
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByStatus(Notification.NotificationStatus status) {
        logger.debug("Getting notifications by status: {}", status);

        return notificationRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    // Get notifications by type
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByType(Notification.NotificationType type) {
        logger.debug("Getting notifications by type: {}", type);

        return notificationRepository.findByTypeOrderByCreatedAtDesc(type).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    // Get notifications by campaign
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByCampaign(UUID campaignId) {
        logger.debug("Getting notifications by campaign: {}", campaignId);

        return notificationRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    // Cancel notification
    @CacheEvict(value = "notifications", key = "#notificationId")
    public void cancelNotification(UUID notificationId) {
        logger.info("Cancelling notification: {}", notificationId);

        int updated = notificationRepository.cancelNotification(notificationId);
        if (updated == 0) {
            throw new NotificationServiceException("Notification not found or cannot be cancelled: " + notificationId);
        }

        logger.info("Notification cancelled successfully: {}", notificationId);
    }

    // Cancel notifications by campaign
    public int cancelNotificationsByCampaign(UUID campaignId) {
        logger.info("Cancelling notifications by campaign: {}", campaignId);

        int cancelled = notificationRepository.cancelNotificationsByCampaign(campaignId);
        logger.info("Cancelled {} notifications for campaign: {}", cancelled, campaignId);

        return cancelled;
    }

    // Process pending notifications
    @Async
    public void processPendingNotifications() {
        logger.debug("Processing pending notifications");

        try {
            List<Notification> pendingNotifications = notificationRepository.findPendingNotifications(LocalDateTime.now());

            for (Notification notification : pendingNotifications) {
                try {
                    sendNotificationNow(notification);
                } catch (Exception e) {
                    logger.error("Failed to send pending notification {}: {}", notification.getId(), e.getMessage());
                    markNotificationAsFailed(notification, e.getMessage(), "SEND_ERROR");
                }
            }

            logger.debug("Processed {} pending notifications", pendingNotifications.size());

        } catch (Exception e) {
            logger.error("Failed to process pending notifications: {}", e.getMessage(), e);
        }
    }

    // Retry failed notifications
    @Async
    public void retryFailedNotifications() {
        logger.debug("Retrying failed notifications");

        try {
            LocalDateTime retryAfter = LocalDateTime.now().minusMinutes(5); // Retry after 5 minutes
            List<Notification> retryableNotifications = notificationRepository
                    .findRetryableNotifications(LocalDateTime.now(), retryAfter);

            for (Notification notification : retryableNotifications) {
                try {
                    notification.incrementRetryCount();
                    sendNotificationNow(notification);
                    logger.debug("Retried notification successfully: {}", notification.getId());
                } catch (Exception e) {
                    logger.error("Failed to retry notification {}: {}", notification.getId(), e.getMessage());
                    markNotificationAsFailed(notification, e.getMessage(), "RETRY_ERROR");
                }
            }

            logger.debug("Processed {} retryable notifications", retryableNotifications.size());

        } catch (Exception e) {
            logger.error("Failed to retry notifications: {}", e.getMessage(), e);
        }
    }

    // Mark notification as opened
    public void markNotificationAsOpened(UUID notificationId) {
        logger.debug("Marking notification as opened: {}", notificationId);

        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();
            notification.markAsOpened();
            notificationRepository.save(notification);

            // Update campaign statistics if applicable
            if (notification.getCampaignId() != null) {
                // This would be handled by CampaignService
                // campaignService.incrementOpenedCount(notification.getCampaignId());
            }
        }
    }

    // Mark notification as clicked
    public void markNotificationAsClicked(UUID notificationId) {
        logger.debug("Marking notification as clicked: {}", notificationId);

        Optional<Notification> optionalNotification = notificationRepository.findById(notificationId);
        if (optionalNotification.isPresent()) {
            Notification notification = optionalNotification.get();
            notification.markAsClicked();
            notificationRepository.save(notification);

            // Update campaign statistics if applicable
            if (notification.getCampaignId() != null) {
                // This would be handled by CampaignService
                // campaignService.incrementClickedCount(notification.getCampaignId());
            }
        }
    }

    // Get notification statistics
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Getting notification statistics: {} to {}", startDate, endDate);

        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        Long totalCount = notificationRepository.countByDateRange(startDate, endDate);
        stats.put("totalNotifications", totalCount);

        // Status statistics
        List<Object[]> statusStats = notificationRepository.getNotificationStatisticsByStatus(startDate, endDate);
        Map<String, Long> statusCounts = statusStats.stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("statusCounts", statusCounts);

        // Type statistics
        List<Object[]> typeStats = notificationRepository.getNotificationStatisticsByType(startDate, endDate);
        Map<String, Long> typeCounts = typeStats.stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("typeCounts", typeCounts);

        // Delivery statistics
        Object[] deliveryStats = notificationRepository.getDeliveryStatistics(startDate, endDate);
        if (deliveryStats != null) {
            stats.put("sent", deliveryStats[0]);
            stats.put("delivered", deliveryStats[1]);
            stats.put("failed", deliveryStats[2]);
            stats.put("bounced", deliveryStats[3]);
            stats.put("opened", deliveryStats[4]);
            stats.put("clicked", deliveryStats[5]);

            // Calculate rates
            Long sent = ((Number) deliveryStats[0]).longValue();
            Long delivered = ((Number) deliveryStats[1]).longValue();
            Long opened = ((Number) deliveryStats[4]).longValue();
            Long clicked = ((Number) deliveryStats[5]).longValue();

            if (sent > 0) {
                stats.put("deliveryRate", (delivered.doubleValue() / sent.doubleValue()) * 100);
            }
            if (delivered > 0) {
                stats.put("openRate", (opened.doubleValue() / delivered.doubleValue()) * 100);
                stats.put("clickRate", (clicked.doubleValue() / delivered.doubleValue()) * 100);
            }
        }

        // Daily statistics
        List<Object[]> dailyStats = notificationRepository.getDailyNotificationStatistics(startDate, endDate);
        stats.put("dailyStats", dailyStats);

        // Performance metrics
        Double avgDeliveryTime = notificationRepository.getAverageDeliveryTime(startDate, endDate);
        stats.put("averageDeliveryTimeSeconds", avgDeliveryTime);

        return stats;
    }

    // Cleanup old notifications
    @Async
    public void cleanupOldNotifications() {
        logger.info("Cleaning up old notifications");

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);

            int deletedNotifications = notificationRepository.cleanupOldNotifications(cutoffDate);
            int deletedFailedNotifications = notificationRepository.cleanupFailedNotifications(cutoffDate);
            int cancelledExpiredNotifications = notificationRepository.cancelExpiredNotifications(LocalDateTime.now());

            logger.info("Cleanup completed: deleted={}, failed={}, cancelled={}",
                       deletedNotifications, deletedFailedNotifications, cancelledExpiredNotifications);

        } catch (Exception e) {
            logger.error("Failed to cleanup old notifications: {}", e.getMessage(), e);
        }
    }

    // Private helper methods
    private Notification createNotificationFromRequest(SendNotificationRequest request) {
        Notification notification = new Notification(request.getType(), request.getTitle(), request.getContent());

        notification.setUserId(request.getUserId());
        notification.setRecipientEmail(request.getRecipientEmail());
        notification.setRecipientPhone(request.getRecipientPhone());
        notification.setRecipientName(request.getRecipientName());
        notification.setPriority(request.getPriority());
        notification.setTemplateId(request.getTemplateId());
        notification.setCampaignId(request.getCampaignId());
        notification.setHtmlContent(request.getHtmlContent());
        notification.setSubject(request.getSubject());
        notification.setPushToken(request.getPushToken());
        notification.setPlatform(request.getPlatform());
        notification.setScheduledAt(request.getScheduledAt());
        notification.setExpiresAt(request.getExpiresAt());
        notification.setMaxRetryCount(request.getMaxRetryCount());
        notification.setRelatedEntityType(request.getRelatedEntityType());
        notification.setRelatedEntityId(request.getRelatedEntityId());
        notification.setSource(request.getSource());
        notification.setTriggeredBy(request.getTriggeredBy());

        // Convert metadata to JSON
        if (request.getMetadata() != null) {
            try {
                notification.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (Exception e) {
                logger.warn("Failed to serialize metadata: {}", e.getMessage());
            }
        }

        // Convert tracking data to JSON
        if (request.getTrackingParams() != null) {
            try {
                notification.setTrackingData(objectMapper.writeValueAsString(request.getTrackingParams()));
            } catch (Exception e) {
                logger.warn("Failed to serialize tracking data: {}", e.getMessage());
            }
        }

        return notification;
    }

    private void applyTemplate(Notification notification, UUID templateId, Map<String, Object> variables) {
        try {
            NotificationTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new NotificationServiceException("Template not found: " + templateId));

            if (!template.isActive()) {
                throw new NotificationServiceException("Template is not active: " + templateId);
            }

            // Apply template content based on notification type
            switch (notification.getType()) {
                case EMAIL -> {
                    if (template.getSubjectTemplate() != null) {
                        notification.setSubject(templateService.processTemplate(template.getSubjectTemplate(), variables));
                    }
                    if (template.getHtmlTemplate() != null) {
                        notification.setHtmlContent(templateService.processTemplate(template.getHtmlTemplate(), variables));
                    }
                }
                case SMS -> {
                    if (template.getSmsTemplate() != null) {
                        notification.setContent(templateService.processTemplate(template.getSmsTemplate(), variables));
                    }
                }
                case PUSH -> {
                    if (template.getPushTemplate() != null) {
                        notification.setContent(templateService.processTemplate(template.getPushTemplate(), variables));
                    }
                }
            }

            // Apply common templates
            if (template.getTitleTemplate() != null) {
                notification.setTitle(templateService.processTemplate(template.getTitleTemplate(), variables));
            }
            if (template.getContentTemplate() != null && notification.getContent() == null) {
                notification.setContent(templateService.processTemplate(template.getContentTemplate(), variables));
            }

            // Increment template usage
            template.incrementUsageCount();
            templateRepository.save(template);

        } catch (Exception e) {
            logger.error("Failed to apply template {}: {}", templateId, e.getMessage());
            throw new NotificationServiceException("Failed to apply template: " + e.getMessage());
        }
    }

    private void validateNotification(Notification notification) {
        // Validate based on notification type
        switch (notification.getType()) {
            case EMAIL -> {
                if (notification.getRecipientEmail() == null || notification.getRecipientEmail().trim().isEmpty()) {
                    throw new NotificationServiceException("Email address is required for email notifications");
                }
                if (notification.getSubject() == null || notification.getSubject().trim().isEmpty()) {
                    throw new NotificationServiceException("Subject is required for email notifications");
                }
            }
            case SMS -> {
                if (notification.getRecipientPhone() == null || notification.getRecipientPhone().trim().isEmpty()) {
                    throw new NotificationServiceException("Phone number is required for SMS notifications");
                }
            }
            case PUSH -> {
                if (notification.getPushToken() == null || notification.getPushToken().trim().isEmpty()) {
                    throw new NotificationServiceException("Push token is required for push notifications");
                }
                if (notification.getPlatform() == null || notification.getPlatform().trim().isEmpty()) {
                    throw new NotificationServiceException("Platform is required for push notifications");
                }
            }
        }

        // Common validations
        if (notification.getTitle() == null || notification.getTitle().trim().isEmpty()) {
            throw new NotificationServiceException("Title is required");
        }
        if (notification.getContent() == null || notification.getContent().trim().isEmpty()) {
            throw new NotificationServiceException("Content is required");
        }
    }

    private boolean isDuplicateNotification(Notification notification) {
        if (notification.getUserId() == null) {
            return false; // Don't check duplicates for anonymous notifications
        }

        // Check for duplicate in last 5 minutes
        LocalDateTime since = LocalDateTime.now().minusMinutes(5);
        return notificationRepository.existsByUserAndContentSince(
            notification.getUserId(),
            notification.getTitle(),
            notification.getContent(),
            since
        );
    }

    private void sendNotificationNow(Notification notification) {
        try {
            switch (notification.getType()) {
                case EMAIL -> emailService.sendEmail(notification);
                case SMS -> smsService.sendSms(notification);
                case PUSH -> pushNotificationService.sendPushNotification(notification);
                case IN_APP -> {
                    // In-app notifications are just stored, not sent externally
                    notification.markAsSent("in-app-" + UUID.randomUUID());
                    notification.markAsDelivered();
                }
            }

            notificationRepository.save(notification);

        } catch (Exception e) {
            logger.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
            markNotificationAsFailed(notification, e.getMessage(), "SEND_ERROR");
            throw e;
        }
    }

    private void markNotificationAsFailed(Notification notification, String errorMessage, String errorCode) {
        notification.markAsFailed(errorMessage, errorCode);
        notificationRepository.save(notification);
    }
}
