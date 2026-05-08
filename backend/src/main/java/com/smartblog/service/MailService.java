package com.smartblog.service;

public interface MailService {

    void sendVerificationCode(String to, String scene, String code, int expireMinutes);
}