package com.shah_s.bakery_notification_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shah_s.bakery_notification_service.dto.CampaignRequest;
import com.shah_s.bakery_notification_service.dto.CampaignResponse;
import com.shah_s.bakery_notification_service.dto.SendNotificationRequest;
import com.shah_s.bakery_notification_service.entity.NotificationCampaign;
import com.shah_s.bakery_notification_service.entity.NotificationTemplate;
import com.shah_s.bakery_notification_service.exception.NotificationServiceException;
import com.shah_s.bakery_notification_service.repository.NotificationCampaignRepository;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CampaignService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignService.class);

    @Autowired
    private NotificationCampaignRepository campaignRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${notification.scheduling.cleanup-days:90}")
    private Integer cleanupDays;

    // Create campaign
    public CampaignResponse createCampaign(CampaignRequest request) {
        logger.info("Creating campaign: name={}, type={}", request.getName(), request.getCampaignType());

        try {
            // Validate campaign name uniqueness
            if (campaignRepository.findByName(request.getName()).isPresent()) {
                throw new NotificationServiceException("Campaign with name already exists: " + request.getName());
            }

            // Validate template if specified
            if (request.getTemplateId() != null) {
                NotificationTemplate template = templateRepository.findById(request.getTemplateId())
                        .orElseThrow(() -> new NotificationServiceException("Template not found: " + request.getTemplateId()));

                if (!template.isActive()) {
                    throw new NotificationServiceException("Template is not active: " + request.getTemplateId());
                }
            }

            NotificationCampaign campaign = createCampaignFromRequest(request);
            campaign = campaignRepository.save(campaign);

            logger.info("Campaign created successfully: {}", campaign.getId());
            return CampaignResponse.from(campaign);

        } catch (Exception e) {
            logger.error("Failed to create campaign: {}", e.getMessage(), e);
            throw new NotificationServiceException("Failed to create campaign: " + e.getMessage());
        }
    }

    // Update campaign
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public CampaignResponse updateCampaign(UUID campaignId, CampaignRequest request) {
        logger.info("Updating campaign: id={}, name={}", campaignId, request.getName());

        try {
            NotificationCampaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new NotificationServiceException("Campaign not found: " + campaignId));

            // Only allow updates to draft campaigns
            if (campaign.getStatus() != NotificationCampaign.CampaignStatus.DRAFT) {
                throw new NotificationServiceException("Cannot update campaign that is not in draft status");
            }

            // Validate name uniqueness (excluding current campaign)
            if (!campaign.getName().equals(request.getName()) &&
                campaignRepository.existsByNameAndNotId(request.getName(), campaignId)) {
                throw new NotificationServiceException("Campaign with name already exists: " + request.getName());
            }

            // Update campaign fields
            updateCampaignFromRequest(campaign, request);
            campaign = campaignRepository.save(campaign);

            logger.info("Campaign updated successfully: {}", campaignId);
            return CampaignResponse.from(campaign);

        } catch (Exception e) {
            logger.error("Failed to update campaign {}: {}", campaignId, e.getMessage(), e);
            throw new NotificationServiceException("Failed to update campaign: " + e.getMessage());
        }
    }

    // Get campaign by ID
    @Cacheable(value = "campaigns", key = "#campaignId")
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(UUID campaignId) {
        logger.debug("Getting campaign by ID: {}", campaignId);

        NotificationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotificationServiceException("Campaign not found: " + campaignId));

        return CampaignResponse.from(campaign);
    }

    // Get campaigns by status
    @Transactional(readOnly = true)
    public List<CampaignResponse> getCampaignsByStatus(NotificationCampaign.CampaignStatus status) {
        logger.debug("Getting campaigns by status: {}", status);

        return campaignRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(CampaignResponse::from)
                .collect(Collectors.toList());
    }

    // Get campaigns by type
    @Transactional(readOnly = true)
    public List<CampaignResponse> getCampaignsByType(NotificationCampaign.CampaignType campaignType) {
        logger.debug("Getting campaigns by type: {}", campaignType);

        return campaignRepository.findByCampaignTypeOrderByCreatedAtDesc(campaignType).stream()
                .map(CampaignResponse::from)
                .collect(Collectors.toList());
    }

    // Get all active campaigns
    @Transactional(readOnly = true)
    public List<CampaignResponse> getAllActiveCampaigns() {
        logger.debug("Getting all active campaigns");

        return campaignRepository.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(CampaignResponse::from)
                .collect(Collectors.toList());
    }

    // Get campaigns with pagination
    @Transactional(readOnly = true)
    public Page<CampaignResponse> getAllActiveCampaigns(int page, int size, String sortBy, String sortDir) {
        logger.debug("Getting active campaigns with pagination: page={}, size={}", page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return campaignRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)
                .map(CampaignResponse::from);
    }

    // Search campaigns
    @Transactional(readOnly = true)
    public List<CampaignResponse> searchCampaigns(String searchTerm) {
        logger.debug("Searching campaigns: {}", searchTerm);

        return campaignRepository.searchCampaigns(searchTerm).stream()
                .map(CampaignResponse::from)
                .collect(Collectors.toList());
    }

    // Start campaign
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public void startCampaign(UUID campaignId) {
        logger.info("Starting campaign: {}", campaignId);

        try {
            NotificationCampaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new NotificationServiceException("Campaign not found: " + campaignId));

            if (!campaign.canStart()) {
                throw new NotificationServiceException("Campaign cannot be started: " + campaign.getStatus());
            }

            // Calculate target recipients
            int targetCount = calculateTargetRecipients(campaign);
            campaign.setTotalRecipients(targetCount);

            // Start campaign
            campaign.start();
            campaignRepository.save(campaign);

            // Execute campaign asynchronously
            executeCampaignAsync(campaignId);

            logger.info("Campaign started successfully: {}", campaignId);

        } catch (Exception e) {
            logger.error("Failed to start campaign {}: {}", campaignId, e.getMessage(), e);
            throw new NotificationServiceException("Failed to start campaign: " + e.getMessage());
        }
    }

    // Pause campaign
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public void pauseCampaign(UUID campaignId) {
        logger.info("Pausing campaign: {}", campaignId);

        int updated = campaignRepository.pauseCampaign(campaignId);
        if (updated == 0) {
            throw new NotificationServiceException("Campaign not found or cannot be paused: " + campaignId);
        }

        logger.info("Campaign paused successfully: {}", campaignId);
    }

    // Resume campaign
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public void resumeCampaign(UUID campaignId) {
        logger.info("Resuming campaign: {}", campaignId);

        int updated = campaignRepository.resumeCampaign(campaignId);
        if (updated == 0) {
            throw new NotificationServiceException("Campaign not found or cannot be resumed: " + campaignId);
        }

        logger.info("Campaign resumed successfully: {}", campaignId);
    }

    // Complete campaign
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public void completeCampaign(UUID campaignId) {
        logger.info("Completing campaign: {}", campaignId);

        int updated = campaignRepository.completeCampaign(campaignId, LocalDateTime.now());
        if (updated == 0) {
            throw new NotificationServiceException("Campaign not found or cannot be completed: " + campaignId);
        }

        logger.info("Campaign completed successfully: {}", campaignId);
    }

    // Cancel campaign
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public void cancelCampaign(UUID campaignId) {
        logger.info("Cancelling campaign: {}", campaignId);

        try {
            int updated = campaignRepository.cancelCampaign(campaignId, LocalDateTime.now());
            if (updated == 0) {
                throw new NotificationServiceException("Campaign not found or cannot be cancelled: " + campaignId);
            }

            // Cancel pending notifications for this campaign
            notificationService.cancelNotificationsByCampaign(campaignId);

            logger.info("Campaign cancelled successfully: {}", campaignId);

        } catch (Exception e) {
            logger.error("Failed to cancel campaign {}: {}", campaignId, e.getMessage(), e);
            throw new NotificationServiceException("Failed to cancel campaign: " + e.getMessage());
        }
    }

    // Delete campaign
    @CacheEvict(value = "campaigns", key = "#campaignId")
    public void deleteCampaign(UUID campaignId) {
        logger.info("Deleting campaign: {}", campaignId);

        NotificationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotificationServiceException("Campaign not found: " + campaignId));

        // Only allow deletion of draft or cancelled campaigns
        if (campaign.getStatus() != NotificationCampaign.CampaignStatus.DRAFT &&
            campaign.getStatus() != NotificationCampaign.CampaignStatus.CANCELLED) {
            throw new NotificationServiceException("Cannot delete campaign that is not in draft or cancelled status");
        }

        campaignRepository.delete(campaign);
        logger.info("Campaign deleted successfully: {}", campaignId);
    }

    // Execute campaign
    @Async
    public void executeCampaignAsync(UUID campaignId) {
        logger.info("Executing campaign asynchronously: {}", campaignId);

        try {
            NotificationCampaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new NotificationServiceException("Campaign not found: " + campaignId));

            if (!campaign.isRunning()) {
                logger.warn("Campaign is not running, skipping execution: {}", campaignId);
                return;
            }

            // Get target recipients
            List<UUID> targetUserIds = getTargetUserIds(campaign);

            if (targetUserIds.isEmpty()) {
                logger.warn("No target recipients found for campaign: {}", campaignId);
                completeCampaign(campaignId);
                return;
            }

            // Create notifications for each recipient
            for (UUID userId : targetUserIds) {
                try {
                    // Check budget constraints
                    if (!campaign.withinBudget(campaign.getCostPerNotification())) {
                        logger.warn("Campaign {} exceeded budget, stopping execution", campaignId);
                        pauseCampaign(campaignId);
                        break;
                    }

                    SendNotificationRequest notificationRequest = createNotificationFromCampaign(campaign, userId);
                    notificationService.sendNotification(notificationRequest);

                    // Update campaign statistics
                    incrementSentCount(campaignId);

                    // Add to cost if specified
                    if (campaign.getCostPerNotification() != null) {
                        addToCost(campaignId, campaign.getCostPerNotification());
                    }

                } catch (Exception e) {
                    logger.error("Failed to send notification for campaign {} user {}: {}",
                               campaignId, userId, e.getMessage());
                    incrementFailedCount(campaignId);
                }
            }

            // Complete campaign if all notifications sent
            completeCampaign(campaignId);

            logger.info("Campaign execution completed: {}", campaignId);

        } catch (Exception e) {
            logger.error("Failed to execute campaign {}: {}", campaignId, e.getMessage(), e);
        }
    }

    // Increment campaign counters
    public void incrementSentCount(UUID campaignId) {
        campaignRepository.incrementSentCount(campaignId);
    }

    public void incrementDeliveredCount(UUID campaignId) {
        campaignRepository.incrementDeliveredCount(campaignId);
    }

    public void incrementFailedCount(UUID campaignId) {
        campaignRepository.incrementFailedCount(campaignId);
    }

    public void incrementOpenedCount(UUID campaignId) {
        campaignRepository.incrementOpenedCount(campaignId);
    }

    public void incrementClickedCount(UUID campaignId) {
        campaignRepository.incrementClickedCount(campaignId);
    }

    public void incrementBouncedCount(UUID campaignId) {
        campaignRepository.incrementBouncedCount(campaignId);
    }

    public void incrementUnsubscribedCount(UUID campaignId) {
        campaignRepository.incrementUnsubscribedCount(campaignId);
    }

    public void addToCost(UUID campaignId, BigDecimal cost) {
        campaignRepository.addToCost(campaignId, cost);
    }

    // Scheduled tasks
    @Scheduled(fixedRate = 60000) // Every minute
    public void processScheduledCampaigns() {
        logger.debug("Processing scheduled campaigns");

        try {
            // Start campaigns that are ready
            List<NotificationCampaign> campaignsToStart = campaignRepository.findCampaignsReadyToStart(LocalDateTime.now());
            for (NotificationCampaign campaign : campaignsToStart) {
                try {
                    startCampaign(campaign.getId());
                } catch (Exception e) {
                    logger.error("Failed to start scheduled campaign {}: {}", campaign.getId(), e.getMessage());
                }
            }

            // Complete campaigns that have reached end time
            List<NotificationCampaign> campaignsToComplete = campaignRepository.findCampaignsToComplete(LocalDateTime.now());
            for (NotificationCampaign campaign : campaignsToComplete) {
                try {
                    completeCampaign(campaign.getId());
                } catch (Exception e) {
                    logger.error("Failed to complete campaign {}: {}", campaign.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Failed to process scheduled campaigns: {}", e.getMessage(), e);
        }
    }

    // Get campaign statistics
    @Transactional(readOnly = true)
    public Map<String, Object> getCampaignStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Getting campaign statistics: {} to {}", startDate, endDate);

        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        Long totalCount = campaignRepository.countByStatus(NotificationCampaign.CampaignStatus.COMPLETED);
        stats.put("totalCampaigns", totalCount);

        // Status statistics
        List<Object[]> statusStats = campaignRepository.getCampaignStatisticsByStatus(startDate, endDate);
        Map<String, Long> statusCounts = statusStats.stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("statusCounts", statusCounts);

        // Type statistics
        List<Object[]> typeStats = campaignRepository.getCampaignStatisticsByType(startDate, endDate);
        Map<String, Long> typeCounts = typeStats.stream()
                .collect(Collectors.toMap(
                    arr -> arr[0].toString(),
                    arr -> ((Number) arr[1]).longValue()
                ));
        stats.put("typeCounts", typeCounts);

        // Performance statistics
        Object[] performanceStats = campaignRepository.getCampaignPerformanceStatistics(startDate, endDate);
        if (performanceStats != null) {
            stats.put("averageSent", performanceStats[0]);
            stats.put("averageDelivered", performanceStats[1]);
            stats.put("averageOpened", performanceStats[2]);
            stats.put("averageClicked", performanceStats[3]);
            stats.put("averageDeliveryRate", performanceStats[4]);
            stats.put("averageOpenRate", performanceStats[5]);
            stats.put("averageClickRate", performanceStats[6]);
        }

        // Top performing campaigns
        List<Object[]> topCampaigns = campaignRepository.getTopPerformingCampaigns(PageRequest.of(0, 10));
        stats.put("topPerformingCampaigns", topCampaigns);

        // Budget analysis
        Object[] budgetStats = campaignRepository.getBudgetAnalysis(startDate, endDate);
        if (budgetStats != null) {
            stats.put("totalSpent", budgetStats[0]);
            stats.put("averageCost", budgetStats[1]);
            stats.put("totalBudget", budgetStats[2]);
            stats.put("overBudgetCount", budgetStats[3]);
        }

        // A/B test results
        List<Object[]> abTestResults = campaignRepository.getAbTestResults(startDate, endDate);
        stats.put("abTestResults", abTestResults);

        // Daily statistics
        List<Object[]> dailyStats = campaignRepository.getDailyCampaignStatistics(startDate, endDate);
        stats.put("dailyStats", dailyStats);

        return stats;
    }

    // Cleanup old campaigns
    @Async
    public void cleanupOldCampaigns() {
        logger.info("Cleaning up old campaigns");

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
            int deleted = campaignRepository.cleanupCancelledCampaigns(cutoffDate);

            logger.info("Cleanup completed: deleted {} cancelled campaigns", deleted);

        } catch (Exception e) {
            logger.error("Failed to cleanup old campaigns: {}", e.getMessage(), e);
        }
    }

    // Private helper methods
    private NotificationCampaign createCampaignFromRequest(CampaignRequest request) {
        NotificationCampaign campaign = new NotificationCampaign(request.getName(), request.getCampaignType());

        updateCampaignFromRequest(campaign, request);

        return campaign;
    }

    private void updateCampaignFromRequest(NotificationCampaign campaign, CampaignRequest request) {
        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        campaign.setCampaignType(request.getCampaignType());
        campaign.setTemplateId(request.getTemplateId());
        campaign.setScheduledStartAt(request.getScheduledStartAt());
        campaign.setScheduledEndAt(request.getScheduledEndAt());
        campaign.setIsActive(request.getIsActive());
        campaign.setIsRecurring(request.getIsRecurring());
        campaign.setRecurrencePattern(request.getRecurrencePattern());
        campaign.setMaxRecipients(request.getMaxRecipients());
        campaign.setPriority(request.getPriority());
        campaign.setBudgetLimit(request.getBudgetLimit());
        campaign.setCostPerNotification(request.getCostPerNotification());
        campaign.setIsAbTest(request.getIsAbTest());
        campaign.setAbTestPercentage(request.getAbTestPercentage());
        campaign.setAbVariant(request.getAbVariant());
        campaign.setCreatedBy(request.getCreatedBy());

        // Convert complex objects to JSON
        try {
            if (request.getTargetAudience() != null) {
                campaign.setTargetAudience(objectMapper.writeValueAsString(request.getTargetAudience()));
            }
            if (request.getTargetUserIds() != null) {
                campaign.setTargetUserIds(objectMapper.writeValueAsString(request.getTargetUserIds()));
            }
            if (request.getTargetSegments() != null) {
                campaign.setTargetSegments(objectMapper.writeValueAsString(request.getTargetSegments()));
            }
            if (request.getContentVariations() != null) {
                campaign.setContentVariations(objectMapper.writeValueAsString(request.getContentVariations()));
            }
            if (request.getPersonalizationData() != null) {
                campaign.setPersonalizationData(objectMapper.writeValueAsString(request.getPersonalizationData()));
            }
            if (request.getTags() != null) {
                campaign.setTags(objectMapper.writeValueAsString(request.getTags()));
            }
            if (request.getMetadata() != null) {
                campaign.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            }
            if (request.getTrackingParams() != null) {
                campaign.setTrackingParams(objectMapper.writeValueAsString(request.getTrackingParams()));
            }

        } catch (Exception e) {
            logger.warn("Failed to serialize campaign data: {}", e.getMessage());
        }
    }

    private int calculateTargetRecipients(NotificationCampaign campaign) {
        // This would implement logic to calculate target recipients based on:
        // - targetAudience criteria
        // - targetUserIds list
        // - targetSegments
        // For now, return a placeholder
        return 100;
    }

    private List<UUID> getTargetUserIds(NotificationCampaign campaign) {
        // This would implement logic to get actual user IDs based on targeting criteria
        // For now, return a sample list
        List<UUID> userIds = new ArrayList<>();

        try {
            if (campaign.getTargetUserIds() != null) {
                List<String> stringIds = objectMapper.readValue(campaign.getTargetUserIds(), List.class);
                userIds = stringIds.stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
            }

            // Apply max recipients limit
            if (campaign.getMaxRecipients() != null && userIds.size() > campaign.getMaxRecipients()) {
                userIds = userIds.subList(0, campaign.getMaxRecipients());
            }

        } catch (Exception e) {
            logger.error("Failed to parse target user IDs for campaign {}: {}", campaign.getId(), e.getMessage());
        }

        return userIds;
    }

    private SendNotificationRequest createNotificationFromCampaign(NotificationCampaign campaign, UUID userId) {
        SendNotificationRequest request = new SendNotificationRequest();

        request.setUserId(userId);
        request.setCampaignId(campaign.getId());
        request.setTemplateId(campaign.getTemplateId());
        request.setType(determineNotificationType(campaign.getCampaignType()));
        request.setPriority(determineNotificationPriority(campaign.getPriority()));
        request.setSource("CAMPAIGN");
        request.setTriggeredBy("CAMPAIGN_EXECUTION");

        // Set basic content if no template
        if (campaign.getTemplateId() == null) {
            request.setTitle("Campaign: " + campaign.getName());
            request.setContent("This is a notification from campaign: " + campaign.getName());
        }

        // Add campaign tracking parameters
        Map<String, Object> trackingParams = new HashMap<>();
        trackingParams.put("campaignId", campaign.getId());
        trackingParams.put("campaignType", campaign.getCampaignType());
        trackingParams.put("abVariant", campaign.getAbVariant());
        request.setTrackingParams(trackingParams);

        return request;
    }

    private com.shah_s.bakery_notification_service.entity.Notification.NotificationType determineNotificationType(
            NotificationCampaign.CampaignType campaignType) {
        // Map campaign types to notification types
        return switch (campaignType) {
            case EMAIL_MARKETING, NEWSLETTER, WELCOME_SERIES, RE_ENGAGEMENT, FEEDBACK_REQUEST ->
                com.shah_s.bakery_notification_service.entity.Notification.NotificationType.EMAIL;
            case SMS_MARKETING ->
                com.shah_s.bakery_notification_service.entity.Notification.NotificationType.SMS;
            case PUSH_MARKETING ->
                com.shah_s.bakery_notification_service.entity.Notification.NotificationType.PUSH;
            default ->
                com.shah_s.bakery_notification_service.entity.Notification.NotificationType.EMAIL;
        };
    }

    private com.shah_s.bakery_notification_service.entity.Notification.NotificationPriority determineNotificationPriority(
            String priority) {
        if (priority == null) {
            return com.shah_s.bakery_notification_service.entity.Notification.NotificationPriority.NORMAL;
        }

        return switch (priority.toUpperCase()) {
            case "LOW" -> com.shah_s.bakery_notification_service.entity.Notification.NotificationPriority.LOW;
            case "HIGH" -> com.shah_s.bakery_notification_service.entity.Notification.NotificationPriority.HIGH;
            case "URGENT" -> com.shah_s.bakery_notification_service.entity.Notification.NotificationPriority.URGENT;
            default -> com.shah_s.bakery_notification_service.entity.Notification.NotificationPriority.NORMAL;
        };
    }
}
