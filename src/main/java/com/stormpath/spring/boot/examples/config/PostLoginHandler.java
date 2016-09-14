package com.stormpath.spring.boot.examples.config;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.directory.CustomData;
import com.stormpath.sdk.servlet.mvc.WebHandler;
import com.stormpath.spring.boot.examples.util.TwilioLoginMessageBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class PostLoginHandler{

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PostLoginHandler.class);

    @Value("#{ @environment['twilio.account.sid'] }")
    protected String twilioAccountSid;

    @Value("#{ @environment['twilio.auth.token'] }")
    protected String twilioAuthToken;

    @Value("#{ @environment['twilio.from.number'] }")
    protected String twilioFromNumber;

    @Value("#{ @environment['stormpath.customData.phoneNumber.identifier'] ?: 'phoneNumber' }")
    protected String phoneNumberIdentifier;

    @Value("#{ @environment['stormpath.customData.loginIPs.identifier'] ?: 'loginIPs' }")
    protected String loginIPsIdentifier;

    @Bean
    @ConditionalOnProperty(name = "twilio.enabled", havingValue = "false", matchIfMissing = true)
    @Qualifier("loginPostHandler")
    public WebHandler defaultLoginPostHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Account account) -> {
            log.info("Hit default loginPostHandler with account: {}", account.getEmail());
            return true;
        };
    }

    @Bean
    @ConditionalOnProperty(name = "twilio.enabled", havingValue = "true")
    public WebHandler loginPostHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Account account) -> {
            log.info("Account Full Name: " + account.getFullName());

            CustomData customData = account.getCustomData();
            String toNumber = (String) customData.get(phoneNumberIdentifier);
            List<String> loginIPs = getLoginIPs(customData);

            String ipAddress = getIPAddress(request);

            if (loginIPs.contains(ipAddress)) {
                // they've already logged in from this location
                log.info("{} has already logged in from: {}. No message sent.", account.getEmail(), ipAddress);
            } else {
                saveLoginIPs(ipAddress, loginIPs, customData);

                TwilioLoginMessageBuilder
                    .builder()
                    .setAccountSid(twilioAccountSid)
                    .setAuthToken(twilioAuthToken)
                    .setFromNumber(twilioFromNumber)
                    .setToNumber(toNumber)
                    .send("New login for: " + account.getEmail() + ", from: " + ipAddress);
            }

            return true;
        };
    }

    private String getIPAddress(HttpServletRequest request) {
        // get user IP and account for proxies
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }

        return ipAddress;
    }

    private List<String> getLoginIPs(CustomData customData) {
        // check to see if the user has accessed the app from this location before
        List<String> loginIPs = (List<String>) customData.get(loginIPsIdentifier);
        if (loginIPs == null) {
            loginIPs = new ArrayList<>();
        }

        return loginIPs;
    }

    private void saveLoginIPs(String ipAddress, List<String> loginIPs, CustomData customData) {
        loginIPs.add(ipAddress);
        customData.put(loginIPsIdentifier, loginIPs);
        customData.save();
    }
}
