package org.example.backend.service;

import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private MailService mailService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private JwtProvider jwtProvider;

    private String generateOtp() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    public void sendMail(String email) {
        String otp = generateOtp();
        redisService.save(
                "otp:" + email,
                otp,
                5 * 60 * 1000 // 5 phút
        );
        mailService.sendOtpMail(email, otp);
    }

    public String verifyOtp(String email, String otp) {
        String key = "otp:" + email;

        String savedOtp = redisService.get(key);

        if (savedOtp == null) {
            throw new CustomBusinessException(ErrorCode.OTP_EXPIRED);
        }

        if (!savedOtp.equals(otp)) {
            throw new CustomBusinessException(ErrorCode.INVALID_OTP);
        }

        redisService.delete(key);

        return jwtProvider.generateOtpVerificationToken(email);
    }
}
