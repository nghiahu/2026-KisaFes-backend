package org.example.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpMail(String to, String otp) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Xác thực tài khoản - Kisafres");

            String htmlContent = buildOtpEmail(otp, "mail/otp-email");
            helper.setText(htmlContent, true);
            helper.setFrom("nghia120425@gmai.com", "Kisafres");

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }
    public void sendOtpResetPass(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Xác thực đổi mật khẩu - Kisafres");

            String htmlContent = buildOtpEmail(otp, "mail/otp-reset-password");
            helper.setText(htmlContent, true);
            helper.setFrom("nghia120425@gmai.com", "Kisafres");

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email", e);
        }
    }

    // ================= TEMPLATE =================
    private String buildOtpEmail(String otp, String urlMail) {
        Context context = new Context();
        context.setVariable("otp", otp);

        return templateEngine.process(urlMail, context);
    }

    private String loadTemplate(String path) {
        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(path)) {

            assert inputStream != null;
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Không đọc được template email", e);
        }
    }
}