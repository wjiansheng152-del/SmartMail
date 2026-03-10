package com.smartmail.delivery.channel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "app.channel.smtp.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    public SendResult send(SendRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(request.getHtmlBody(), true);
            try {
                helper.setFrom(request.getFrom() != null ? request.getFrom() : "noreply@smartmail.local",
                        request.getFromName() != null ? request.getFromName() : "SmartMail");
            } catch (java.io.UnsupportedEncodingException e) {
                helper.setFrom(request.getFrom() != null ? request.getFrom() : "noreply@smartmail.local");
            }
            if (request.getReplyTo() != null) {
                helper.setReplyTo(request.getReplyTo());
            }
            mailSender.send(message);
            return SendResult.builder().success(true).messageId(message.getMessageID()).build();
        } catch (MessagingException e) {
            return SendResult.builder().success(false).errorMessage(e.getMessage()).build();
        }
    }

    @Override
    public String channelType() {
        return "smtp";
    }
}
