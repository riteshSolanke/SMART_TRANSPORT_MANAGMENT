package com.transport.authservice.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
public class ConsoleOtpSender implements OtpSender{

    @Value("${app.sms.mode}")
    private String smsMode;

    @Value("${app.otp.expose-in-logs:false}")
    private boolean exposeOtpInLogs;

    @Override
    public void send(String mobileNumber, String otp){
        if (exposeOtpInLogs) {
            log.warn("[{} MODE] OTP delivery for number ending in {}: {}",
                    smsMode.toUpperCase(), lastFour(mobileNumber), otp);
            return;
        }
        log.info("[{} MODE] OTP delivery requested for number ending in {}",
                smsMode.toUpperCase(), lastFour(mobileNumber));
    }

    private String lastFour(String value) {
        return value == null || value.length() < 4
                ? "****"
                : value.substring(value.length() - 4);
    }
}
