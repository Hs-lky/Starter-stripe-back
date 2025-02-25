package com.example.springsaas.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;
    private final ResourceLoader resourceLoader;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name}")
    private String appName;

    @Async
    public void sendEmail(String to, String subject, String templateName, Context context, Map<String, Resource> inlineResources) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Add inline resources (images)
            if (inlineResources != null) {
                for (Map.Entry<String, Resource> entry : inlineResources.entrySet()) {
                    helper.addInline(entry.getKey(), entry.getValue());
                }
            }
            
            emailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendEmailWithAttachment(String to, String subject, String templateName, 
            Context context, String attachmentFilename, byte[] attachmentData) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Add the attachment
            helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentData));
            
            emailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email with attachment", e);
        }
    }

    public void sendVerificationEmail(String to, String name, String verificationLink) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("verificationLink", verificationLink);
        
        Map<String, Resource> inlineResources = new HashMap<>();
        try {
            inlineResources.put("logo", resourceLoader.getResource("classpath:static/images/logo.png"));
        } catch (Exception e) {
            log.warn("Could not load logo image for email", e);
        }
        
        sendEmail(
            to,
            "Verify your email address",
            "email/verification",
            context,
            inlineResources
        );
    }

    public void sendWelcomeEmail(String to, String name) {
        Context context = new Context();
        context.setVariable("name", name);
        
        Map<String, Resource> inlineResources = new HashMap<>();
        try {
            inlineResources.put("logo", resourceLoader.getResource("classpath:static/images/logo.png"));
            inlineResources.put("welcome-image", resourceLoader.getResource("classpath:static/images/welcome.png"));
        } catch (Exception e) {
            log.warn("Could not load images for welcome email", e);
        }
        
        sendEmail(
            to,
            "Welcome to " + appName + "!",
            "email/welcome",
            context,
            inlineResources
        );
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        Context context = new Context();
        context.setVariable("resetLink", resetLink);
        Map<String, Resource> inlineResources = new HashMap<>();
        try {
            inlineResources.put("logo", resourceLoader.getResource("classpath:static/images/logo.png"));
        } catch (Exception e) {
            log.warn("Could not load logo image for password reset email", e);
        }
        sendEmail(to, "Password Reset Request", "email/password-reset", context, inlineResources);
    }

    public void sendSubscriptionConfirmationEmail(String to, String name, String plan) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("plan", plan);
        sendEmail(to, "Subscription Confirmation", "subscription-confirmation", context, null);
    }

    public void sendInvoiceEmail(String to, String name, String invoiceNumber, byte[] pdfData) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("invoiceNumber", invoiceNumber);
        
        sendEmailWithAttachment(
            to,
            "Your Invoice #" + invoiceNumber,
            "invoice-email",
            context,
            "invoice-" + invoiceNumber + ".pdf",
            pdfData
        );
    }
} 