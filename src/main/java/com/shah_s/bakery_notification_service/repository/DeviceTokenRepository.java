package com.shah_s.bakery_notification_service.repository;

import com.shah_s.bakery_notification_service.entity.DeviceToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    // Find by device token
    Optional<DeviceToken> findByDeviceToken(String deviceToken);

    // Find by SNS endpoint ARN
    Optional<DeviceToken> findBySnsEndpointArn(String snsEndpointArn);

    // Find by user
    List<DeviceToken> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<DeviceToken> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Find active tokens by user
    List<DeviceToken> findByUserIdAndIsActiveTrueAndIsValidTrueOrderByCreatedAtDesc(UUID userId);

    // Find tokens by platform
    List<DeviceToken> findByPlatformOrderByCreatedAtDesc(String platform);

    Page<DeviceToken> findByPlatformOrderByCreatedAtDesc(String platform, Pageable pageable);

    // Find active tokens by platform
    List<DeviceToken> findByPlatformAndIsActiveTrueAndIsValidTrueOrderByCreatedAtDesc(String platform);

    // Find tokens by user and platform
    List<DeviceToken> findByUserIdAndPlatformOrderByCreatedAtDesc(UUID userId, String platform);

    // Find active tokens that can receive notifications
    @Query("SELECT d FROM DeviceToken d WHERE d.isActive = true AND d.isValid = true " +
           "AND d.notificationEnabled = true " +
           "AND (d.expiresAt IS NULL OR d.expiresAt > :now)")
    List<DeviceToken> findActiveTokens(@Param("now") LocalDateTime now);

    // Find active tokens by user that can receive notifications
    @Query("SELECT d FROM DeviceToken d WHERE d.userId = :userId " +
           "AND d.isActive = true AND d.isValid = true " +
           "AND d.notificationEnabled = true " +
           "AND (d.expiresAt IS NULL OR d.expiresAt > :now)")
    List<DeviceToken> findActiveTokensByUser(@Param("userId") UUID userId,
                                           @Param("now") LocalDateTime now);

    // Find expired tokens
    @Query("SELECT d FROM DeviceToken d WHERE d.expiresAt IS NOT NULL AND d.expiresAt <= :now")
    List<DeviceToken> findExpiredTokens(@Param("now") LocalDateTime now);

    // Find inactive tokens
    List<DeviceToken> findByIsActiveFalseOrderByUpdatedAtDesc();

    // Find invalid tokens
    List<DeviceToken> findByIsValidFalseOrderByLastErrorAtDesc();

    // Find tokens with errors
    @Query("SELECT d FROM DeviceToken d WHERE d.errorCount > :minErrors " +
           "ORDER BY d.errorCount DESC, d.lastErrorAt DESC")
    List<DeviceToken> findTokensWithErrors(@Param("minErrors") Integer minErrors);

    // Find tokens by device ID
    List<DeviceToken> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    // Find duplicate tokens (same user, same platform)
    @Query("SELECT d FROM DeviceToken d WHERE d.userId = :userId AND d.platform = :platform " +
           "AND d.id != :excludeId AND d.isActive = true")
    List<DeviceToken> findDuplicateTokens(@Param("userId") UUID userId,
                                        @Param("platform") String platform,
                                        @Param("excludeId") UUID excludeId);

    // Find tokens by registration source
    List<DeviceToken> findByRegisteredFromOrderByCreatedAtDesc(String registeredFrom);

    // Find tokens by country
    List<DeviceToken> findByCountryOrderByCreatedAtDesc(String country);

    // Find tokens by timezone
    List<DeviceToken> findByTimezoneOrderByCreatedAtDesc(String timezone);

    // Find tokens by app version
    List<DeviceToken> findByAppVersionOrderByCreatedAtDesc(String appVersion);

    // Find tokens by OS version
    List<DeviceToken> findByOsVersionOrderByCreatedAtDesc(String osVersion);

    // Find tokens by device model
    List<DeviceToken> findByDeviceModelOrderByCreatedAtDesc(String deviceModel);

    // Find tokens by date range
    List<DeviceToken> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate,
                                                               LocalDateTime endDate);

    // Find recently used tokens
    @Query("SELECT d FROM DeviceToken d WHERE d.lastUsedAt IS NOT NULL " +
           "ORDER BY d.lastUsedAt DESC")
    List<DeviceToken> findRecentlyUsedTokens(Pageable pageable);

    // Find unused tokens
    @Query("SELECT d FROM DeviceToken d WHERE d.lastUsedAt IS NULL " +
           "AND d.createdAt < :cutoffDate")
    List<DeviceToken> findUnusedTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find stale tokens (not validated recently)
    @Query("SELECT d FROM DeviceToken d WHERE d.lastValidatedAt IS NULL " +
           "OR d.lastValidatedAt < :cutoffDate")
    List<DeviceToken> findStaleTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Count methods
    Long countByUserId(UUID userId);

    Long countByPlatform(String platform);

    Long countByIsActiveTrue();

    Long countByIsValidTrue();

    Long countByNotificationEnabledTrue();

    Long countByUserIdAndIsActiveTrueAndIsValidTrue(UUID userId);

    // Statistics queries
    @Query("SELECT d.platform, COUNT(d) FROM DeviceToken d " +
           "WHERE d.isActive = true AND d.isValid = true " +
           "GROUP BY d.platform")
    List<Object[]> getActiveTokenStatisticsByPlatform();

    @Query("SELECT d.country, COUNT(d) FROM DeviceToken d " +
           "WHERE d.isActive = true AND d.country IS NOT NULL " +
           "GROUP BY d.country " +
           "ORDER BY COUNT(d) DESC")
    List<Object[]> getTokenStatisticsByCountry();

    @Query("SELECT d.appVersion, COUNT(d) FROM DeviceToken d " +
           "WHERE d.isActive = true AND d.appVersion IS NOT NULL " +
           "GROUP BY d.appVersion " +
           "ORDER BY d.appVersion DESC")
    List<Object[]> getTokenStatisticsByAppVersion();

    @Query("SELECT DATE(d.createdAt), COUNT(d) FROM DeviceToken d " +
           "WHERE d.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(d.createdAt) " +
           "ORDER BY DATE(d.createdAt)")
    List<Object[]> getDailyRegistrationStatistics(@Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    // Error analysis
    @Query("SELECT d.lastErrorMessage, COUNT(d) as errorCount " +
           "FROM DeviceToken d " +
           "WHERE d.isValid = false AND d.lastErrorMessage IS NOT NULL " +
           "GROUP BY d.lastErrorMessage " +
           "ORDER BY errorCount DESC")
    List<Object[]> getErrorAnalysis();

    // Update operations
    @Modifying
    @Query("UPDATE DeviceToken d SET d.lastUsedAt = :now WHERE d.id = :tokenId")
    int updateLastUsedAt(@Param("tokenId") UUID tokenId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.isValid = true, d.errorCount = 0, " +
           "d.lastErrorMessage = null, d.lastValidatedAt = :now WHERE d.id = :tokenId")
    int markTokenAsValid(@Param("tokenId") UUID tokenId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.isValid = false, d.errorCount = d.errorCount + 1, " +
           "d.lastErrorMessage = :errorMessage, d.lastErrorAt = :now WHERE d.id = :tokenId")
    int markTokenAsInvalid(@Param("tokenId") UUID tokenId,
                          @Param("errorMessage") String errorMessage,
                          @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.isActive = false WHERE d.id = :tokenId")
    int deactivateToken(@Param("tokenId") UUID tokenId);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.isActive = true WHERE d.id = :tokenId")
    int activateToken(@Param("tokenId") UUID tokenId);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.notificationEnabled = :enabled WHERE d.id = :tokenId")
    int updateNotificationEnabled(@Param("tokenId") UUID tokenId, @Param("enabled") Boolean enabled);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.snsEndpointArn = :endpointArn WHERE d.id = :tokenId")
    int updateSnsEndpointArn(@Param("tokenId") UUID tokenId, @Param("endpointArn") String endpointArn);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.expiresAt = :expiresAt WHERE d.id = :tokenId")
    int updateExpirationDate(@Param("tokenId") UUID tokenId, @Param("expiresAt") LocalDateTime expiresAt);

    // Bulk operations
    @Modifying
    @Query("UPDATE DeviceToken d SET d.isActive = false " +
           "WHERE d.expiresAt IS NOT NULL AND d.expiresAt <= :now")
    int deactivateExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.isActive = false " +
           "WHERE d.errorCount >= :maxErrors")
    int deactivateTokensWithTooManyErrors(@Param("maxErrors") Integer maxErrors);

    @Modifying
    @Query("UPDATE DeviceToken d SET d.isActive = false " +
           "WHERE d.userId = :userId AND d.platform = :platform AND d.id != :keepTokenId")
    int deactivateDuplicateTokens(@Param("userId") UUID userId,
                                 @Param("platform") String platform,
                                 @Param("keepTokenId") UUID keepTokenId);

    // Cleanup operations
    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.isActive = false " +
           "AND d.updatedAt < :cutoffDate")
    int cleanupInactiveTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.isValid = false " +
           "AND d.errorCount >= :maxErrors " +
           "AND d.lastErrorAt < :cutoffDate")
    int cleanupInvalidTokens(@Param("maxErrors") Integer maxErrors,
                           @Param("cutoffDate") LocalDateTime cutoffDate);

    // Validation queries
    @Query("SELECT COUNT(d) > 0 FROM DeviceToken d " +
           "WHERE d.deviceToken = :deviceToken AND d.id != :excludeId")
    Boolean existsByDeviceTokenAndNotId(@Param("deviceToken") String deviceToken,
                                       @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(d) > 0 FROM DeviceToken d " +
           "WHERE d.snsEndpointArn = :endpointArn AND d.id != :excludeId")
    Boolean existsBySnsEndpointArnAndNotId(@Param("endpointArn") String endpointArn,
                                          @Param("excludeId") UUID excludeId);

    // Topic subscription queries
    @Query("SELECT d FROM DeviceToken d " +
           "WHERE d.subscribedTopics IS NOT NULL " +
           "AND d.subscribedTopics LIKE CONCAT('%', :topicArn, '%') " +
           "AND d.isActive = true AND d.isValid = true")
    List<DeviceToken> findTokensSubscribedToTopic(@Param("topicArn") String topicArn);

    // Performance queries
    @Query(value = "SELECT platform, COUNT(*) as token_count, " +
                   "AVG(error_count) as avg_errors, " +
                   "COUNT(CASE WHEN is_active = true THEN 1 END) as active_count " +
                   "FROM device_tokens " +
                   "GROUP BY platform", nativeQuery = true)
    List<Object[]> getPlatformPerformanceStatistics();

    // Geographic distribution
    @Query("SELECT d.country, d.timezone, COUNT(d) as tokenCount " +
           "FROM DeviceToken d " +
           "WHERE d.isActive = true AND d.country IS NOT NULL " +
           "GROUP BY d.country, d.timezone " +
           "ORDER BY tokenCount DESC")
    List<Object[]> getGeographicDistribution();
}
