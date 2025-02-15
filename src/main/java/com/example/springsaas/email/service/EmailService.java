package com.example.springsaas.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
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

    public void sendWelcomeEmail(String to, String name) {
        Context context = new Context();
        context.setVariable("name", name);
        sendEmail(to, "Welcome to Our Service", "welcome", context);
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        Context context = new Context();
        context.setVariable("resetLink", resetLink);
        sendEmail(to, "Password Reset Request", "password-reset", context);
    }

    public void sendVerificationEmail(String to, String verificationLink) {
        Context context = new Context();
        context.setVariable("verificationLink", verificationLink);
        sendEmail(to, "Verify Your Email", "email-verification", context);
    }

    public void sendSubscriptionConfirmationEmail(String to, String name, String plan) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("plan", plan);
        sendEmail(to, "Subscription Confirmation", "subscription-confirmation", context);
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