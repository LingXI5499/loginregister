package com.smartblog.service.impl;

import com.smartblog.exception.BusinessException;
import com.smartblog.service.MailService;
import com.smartblog.util.AuthConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationCode(String to, String scene, String code, int expireMinutes) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("SmartBlog 邮箱验证码");
            message.setText(buildContent(scene, code, expireMinutes));
            mailSender.send(message);
        } catch (MailException e) {
            throw new BusinessException("邮件发送失败，请稍后再试");
        }
    }

    private String buildContent(String scene, String code, int expireMinutes) {
        return """
                你正在进行：%s

                你的验证码是：%s

                该验证码将在 %d 分钟后失效。
                如果不是你本人操作，请忽略本邮件。

                SmartBlog
                """.formatted(sceneName(scene), code, expireMinutes);
    }

    private String sceneName(String scene) {
        if (AuthConstants.SCENE_REGISTER_EMAIL.equals(scene)) {
            return "注册账号";
        }
        if (AuthConstants.SCENE_LOGIN_EMAIL.equals(scene)) {
            return "邮箱验证码登录";
        }
        if (AuthConstants.SCENE_RESET_PASSWORD.equals(scene)) {
            return "重置密码";
        }
        if (AuthConstants.SCENE_DELETE_ACCOUNT.equals(scene)) {
            return "注销账号";
        }
        return "邮箱验证";
    }
}