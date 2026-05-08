package com.smartblog.service.impl;

import com.smartblog.service.MailService;
import com.smartblog.util.AuthConstants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationCode(String to, String scene, String code, int expireMinutes) {
        String subject = "SmartBlog " + sceneName(scene) + "验证码";
        String text = "您的" + sceneName(scene) + "验证码为：" + code + "，有效期" + expireMinutes + "分钟。";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(from, "SmartBlog", "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    private String sceneName(String scene) {
        return switch (scene) {
            case AuthConstants.SCENE_REGISTER -> "注册";
            case AuthConstants.SCENE_LOGIN -> "登录";
            case AuthConstants.SCENE_RESET_PASSWORD -> "重置密码";
            case AuthConstants.SCENE_DELETE_ACCOUNT -> "注销账号";
            case AuthConstants.SCENE_CANCEL_DELETE_ACCOUNT -> "取消注销账号";
            default -> "操作";
        };
    }
}