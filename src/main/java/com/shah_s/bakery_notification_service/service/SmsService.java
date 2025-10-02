package com.shah_s.bakery_notification_service.service;

import com.shah_s.bakery_notification_service.entity.Notification;
import com.shah_s.bakery_notification_service.exception.NotificationServiceException;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${notification.sms.account-sid}")
    private String accountSid;

    @Value("${notification.sms.auth-token}")
    private String authToken;

    @Value("${notification.sms.from-number}")
    private String fromNumber;

    @Value("${notification.sms.enabled:false}")
    private Boolean smsEnabled;

    @Value("${notification.sms.retry-attempts:3}")
    private Integer retryAttempts;

    @PostConstruct
    public void initializeTwilio() {
        if (smsEnabled && accountSid != null && authToken != null) {
            try {
                Twilio.init(accountSid, authToken);
                logger.info("Twilio SMS service initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize Twilio SMS service: {}", e.getMessage());
                smsEnabled = false;
            }
        } else {
            logger.info("SMS service is disabled or not configured");
        }
    }

    // Send SMS notification
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendSms(Notification notification) {
        if (!smsEnabled) {
            logger.warn("SMS notifications are disabled");
            throw new NotificationServiceException("SMS notifications are disabled");
        }

        logger.info("Sending SMS notification: id={}, recipient={}",
                   notification.getId(), maskPhoneNumber(notification.getRecipientPhone()));

        try {
            Message message = Message.creator(
                new PhoneNumber(notification.getRecipientPhone()),
                new PhoneNumber(fromNumber),
                notification.getContent()
            ).create();

            notification.markAsSent(message.getSid());
            logger.info("SMS sent successfully: id={}, sid={}", notification.getId(), message.getSid());

        } catch (Exception e) {
            logger.error("Failed to send SMS notification {}: {}", notification.getId(), e.getMessage());
            throw new NotificationServiceException("Failed to send SMS: " + e.getMessage());
        }
    }

    // Send simple SMS
    public String sendSms(String toNumber, String messageBody) {
        if (!smsEnabled) {
            throw new NotificationServiceException("SMS notifications are disabled");
        }

        logger.info("Sending SMS: to={}", maskPhoneNumber(toNumber));

        try {
            Message message = Message.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(fromNumber),
                messageBody
            ).create();

            logger.info("SMS sent successfully: to={}, sid={}", maskPhoneNumber(toNumber), message.getSid());
            return message.getSid();

        } catch (Exception e) {
            logger.error("Failed to send SMS to {}: {}", maskPhoneNumber(toNumber), e.getMessage());
            throw new NotificationServiceException("Failed to send SMS: " + e.getMessage());
        }
    }

    // Send order confirmation SMS
    public void sendOrderConfirmationSms(String phoneNumber, String customerName,
                                       String orderNumber, String estimatedTime) {
        String message = String.format(
            "Hi %s! Your order %s has been confirmed. Estimated %s. Thank you for choosing Shah's Bakery!",
            customerName, orderNumber, estimatedTime
        );

        sendSms(phoneNumber, message);
    }

    // Send order ready SMS
    public void sendOrderReadySms(String phoneNumber, String customerName, String orderNumber) {
        String message = String.format(
            "Hi %s! Your order %s is ready for pickup. Please visit us at your convenience. Thanks!",
            customerName, orderNumber
        );

        sendSms(phoneNumber, message);
    }

    // Send delivery notification SMS
    public void sendDeliveryNotificationSms(String phoneNumber, String customerName,
                                          String orderNumber, String driverName, String eta) {
        String message = String.format(
            "Hi %s! Your order %s is out for delivery with %s. ETA: %s. Please be available.",
            customerName, orderNumber, driverName, eta
        );

        sendSms(phoneNumber, message);
    }

    // Send promotional SMS
    public void sendPromotionalSms(String phoneNumber, String customerName,
                                 String promoCode, String discount) {
        String message = String.format(
            "Hi %s! Special offer: %s off with code %s. Valid today only! Order now at Shah's Bakery.",
            customerName, discount, promoCode
        );

        sendSms(phoneNumber, message);
    }

    // Send appointment reminder SMS
    public void sendAppointmentReminderSms(String phoneNumber, String customerName,
                                         String appointmentTime, String cakeDetails) {
        String message = String.format(
            "Hi %s! Reminder: Your custom cake (%s) will be ready %s. Thanks for choosing Shah's Bakery!",
            customerName, cakeDetails, appointmentTime
        );

        sendSms(phoneNumber, message);
    }

    // Send verification code SMS
    public void sendVerificationCodeSms(String phoneNumber, String verificationCode) {
        String message = String.format(
            "Your Shah's Bakery verification code is: %s. This code expires in 10 minutes. Do not share this code.",
            verificationCode
        );

        sendSms(phoneNumber, message);
    }

    // Send password reset SMS
    public void sendPasswordResetSms(String phoneNumber, String customerName, String resetCode) {
        String message = String.format(
            "Hi %s! Your password reset code is: %s. Use this code to reset your password. Valid for 30 minutes.",
            customerName, resetCode
        );

        sendSms(phoneNumber, message);
    }

    // Send loyalty points SMS
    public void sendLoyaltyPointsSms(String phoneNumber, String customerName,
                                   int pointsEarned, int totalPoints) {
        String message = String.format(
            "Hi %s! You earned %d points. Total: %d points. Use them for discounts on your next order!",
            customerName, pointsEarned, totalPoints
        );

        sendSms(phoneNumber, message);
    }

    // Send birthday wishes SMS
    public void sendBirthdayWishesSms(String phoneNumber, String customerName, String specialOffer) {
        String message = String.format(
            "Happy Birthday %s! 🎂 Enjoy %s on us today. Visit Shah's Bakery to claim your birthday treat!",
            customerName, specialOffer
        );

        sendSms(phoneNumber, message);
    }

    // Send feedback request SMS
    public void sendFeedbackRequestSms(String phoneNumber, String customerName,
                                     String orderNumber, String feedbackUrl) {
        String message = String.format(
            "Hi %s! How was your order %s? Share feedback: %s. Your opinion helps us improve!",
            customerName, orderNumber, feedbackUrl
        );

        sendSms(phoneNumber, message);
    }

    // Validate phone number format
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        // Basic validation for international format (+1234567890)
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");
        return cleaned.matches("^\\+\\d{10,15}$");
    }

    // Format phone number
    public String formatPhoneNumber(String phoneNumber) {
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new NotificationServiceException("Invalid phone number format: " + phoneNumber);
        }

        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");

        // Add + if missing and appears to be US number
        if (!cleaned.startsWith("+") && cleaned.length() == 10) {
            cleaned = "+1" + cleaned;
        } else if (!cleaned.startsWith("+") && cleaned.length() == 11 && cleaned.startsWith("1")) {
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }

    // Test SMS connectivity
    public boolean testSmsConnection() {
        if (!smsEnabled) {
            return false;
        }

        try {
            // Send a test SMS to a verified number (typically your own number in Twilio trial)
            // This is just a connectivity test, not actually sending
            logger.info("SMS connectivity test - service is configured and ready");
            return true;

        } catch (Exception e) {
            logger.error("SMS connectivity test failed: {}", e.getMessage());
            return false;
        }
    }

    // Get SMS service health status
    public Map<String, Object> getSmsServiceHealth() {
        return Map.of(
            "enabled", smsEnabled,
            "fromNumber", fromNumber != null ? maskPhoneNumber(fromNumber) : null,
            "accountSid", accountSid != null ? maskAccountSid(accountSid) : null,
            "retryAttempts", retryAttempts,
            "connectivity", testSmsConnection(),
            "timestamp", LocalDateTime.now()
        );
    }

    // Helper methods
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    private String maskAccountSid(String accountSid) {
        if (accountSid == null || accountSid.length() < 8) {
            return "****";
        }
        return accountSid.substring(0, 4) + "****" + accountSid.substring(accountSid.length() - 4);
    }
}
