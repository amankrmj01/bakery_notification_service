package com.shah_s.bakery_notification_service.service;

import com.shah_s.bakery_notification_service.entity.Notification;
import com.shah_s.bakery_notification_service.exception.NotificationServiceException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Value("${notification.email.reply-to}")
    private String replyToEmail;

    @Value("${notification.email.enabled:true}")
    private Boolean emailEnabled;

    @Value("${notification.email.retry-attempts:3}")
    private Integer retryAttempts;

    // Send email notification
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendEmail(Notification notification) {
        if (!emailEnabled) {
            logger.warn("Email notifications are disabled");
            throw new NotificationServiceException("Email notifications are disabled");
        }

        logger.info("Sending email notification: id={}, recipient={}",
                   notification.getId(), notification.getRecipientEmail());

        try {
            if (notification.getHtmlContent() != null && !notification.getHtmlContent().trim().isEmpty()) {
                sendHtmlEmail(notification);
            } else {
                sendTextEmail(notification);
            }

            notification.markAsSent(UUID.randomUUID().toString());
            logger.info("Email sent successfully: {}", notification.getId());

        } catch (Exception e) {
            logger.error("Failed to send email notification {}: {}", notification.getId(), e.getMessage());
            throw new NotificationServiceException("Failed to send email: " + e.getMessage());
        }
    }

    // Send simple text email
    private void sendTextEmail(Notification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(notification.getRecipientEmail());
            message.setSubject(notification.getSubject());
            message.setText(notification.getContent());
            message.setReplyTo(replyToEmail);

            mailSender.send(message);

        } catch (MailException e) {
            logger.error("Failed to send text email: {}", e.getMessage());
            throw new NotificationServiceException("Failed to send text email: " + e.getMessage());
        }
    }

    // Send HTML email
    private void sendHtmlEmail(Notification notification) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getContent(), notification.getHtmlContent());
            helper.setReplyTo(replyToEmail);

            // Add recipient name if available
            if (notification.getRecipientName() != null) {
                helper.setTo(notification.getRecipientEmail(), notification.getRecipientName());
            }

            mailSender.send(mimeMessage);

        } catch (MessagingException | MailException e) {
            logger.error("Failed to send HTML email: {}", e.getMessage());
            throw new NotificationServiceException("Failed to send HTML email: " + e.getMessage());
        }
    }

    // Send templated email
    public void sendTemplatedEmail(String to, String toName, String subject,
                                  String templateName, Map<String, Object> variables) {
        logger.info("Sending templated email: to={}, template={}", to, templateName);

        try {
            // Process template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            // Create and send email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to, toName);
            helper.setSubject(subject);
            helper.setText("", htmlContent);
            helper.setReplyTo(replyToEmail);

            mailSender.send(mimeMessage);

            logger.info("Templated email sent successfully: to={}, template={}", to, templateName);

        } catch (Exception e) {
            logger.error("Failed to send templated email: {}", e.getMessage());
            throw new NotificationServiceException("Failed to send templated email: " + e.getMessage());
        }
    }

    // Send welcome email
    public void sendWelcomeEmail(String email, String name, String activationLink) {
        Map<String, Object> variables = Map.of(
            "name", name,
            "activationLink", activationLink,
            "supportEmail", replyToEmail
        );

        sendTemplatedEmail(email, name, "Welcome to Shah's Bakery!", "welcome-email", variables);
    }

    // Send password reset email
    public void sendPasswordResetEmail(String email, String name, String resetLink) {
        Map<String, Object> variables = Map.of(
            "name", name,
            "resetLink", resetLink,
            "supportEmail", replyToEmail,
            "expiryTime", "24 hours"
        );

        sendTemplatedEmail(email, name, "Password Reset Request", "password-reset-email", variables);
    }

    // Send order confirmation email
    public void sendOrderConfirmationEmail(String email, String name, String orderNumber,
                                         String orderTotal, String deliveryType) {
        Map<String, Object> variables = Map.of(
            "name", name,
            "orderNumber", orderNumber,
            "orderTotal", orderTotal,
            "deliveryType", deliveryType,
            "supportEmail", replyToEmail,
            "orderDate", LocalDateTime.now()
        );

        sendTemplatedEmail(email, name, "Order Confirmation - " + orderNumber, "order-confirmation-email", variables);
    }

    // Send cart abandonment email
    public void sendCartAbandonmentEmail(String email, String name, String cartUrl,
                                       String cartTotal, int itemCount) {
        Map<String, Object> variables = Map.of(
            "name", name,
            "cartUrl", cartUrl,
            "cartTotal", cartTotal,
            "itemCount", itemCount,
            "supportEmail", replyToEmail
        );

        sendTemplatedEmail(email, name, "Complete Your Order - Items Waiting!", "cart-abandonment-email", variables);
    }

    // Send promotional email
    public void sendPromotionalEmail(String email, String name, String promoCode,
                                   String discount, String expiryDate) {
        Map<String, Object> variables = Map.of(
            "name", name,
            "promoCode", promoCode,
            "discount", discount,
            "expiryDate", expiryDate,
            "supportEmail", replyToEmail
        );

        sendTemplatedEmail(email, name, "Special Offer Just for You!", "promotional-email", variables);
    }

    // Send feedback request email
    public void sendFeedbackRequestEmail(String email, String name, String orderNumber,
                                       String feedbackUrl) {
        Map<String, Object> variables = Map.of(
            "name", name,
            "orderNumber", orderNumber,
            "feedbackUrl", feedbackUrl,
            "supportEmail", replyToEmail
        );

        sendTemplatedEmail(email, name, "How was your experience?", "feedback-request-email", variables);
    }

    // Test email connectivity
    public boolean testEmailConnection() {
        try {
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setFrom(fromEmail);
            testMessage.setTo(fromEmail); // Send to self
            testMessage.setSubject("Email Service Test");
            testMessage.setText("This is a test email to verify email service connectivity.");

            mailSender.send(testMessage);
            logger.info("Email connectivity test successful");
            return true;

        } catch (Exception e) {
            logger.error("Email connectivity test failed: {}", e.getMessage());
            return false;
        }
    }

    // Get email service health status
    public Map<String, Object> getEmailServiceHealth() {
        Map<String, Object> health = Map.of(
            "enabled", emailEnabled,
            "fromEmail", fromEmail,
            "fromName", fromName,
            "replyTo", replyToEmail,
            "retryAttempts", retryAttempts,
            "connectivity", testEmailConnection(),
            "timestamp", LocalDateTime.now()
        );

        return health;
    }
}
