package com.stormpath.spring.boot.examples;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.directory.CustomData;
import com.stormpath.sdk.lang.Strings;
import com.stormpath.sdk.servlet.mvc.WebHandler;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
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

    @Bean
    public WebHandler loginPostHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Account account) -> {
            log.info("Account Full Name: " + account.getFullName());

            if (!isTwilioConfigured()) {
                log.warn(
                    "Twilio not configured. Please set the following properties: " +
                    "twilio.account.sid, twilio.auth.token, twilio.from.number"
                );
                return true;
            }

            CustomData customData = account.getCustomData();
            String toNumber = (String) customData.get("phoneNumber");

            if (toNumber == null) {
                log.warn("No 'phoneNumber' in CustomData for: {}", account.getEmail());
                return true;
            }

            // get user IP and account for proxies
            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }

            // check to see if the user has accessed the app from this location before
            List<String> loginIPs = (List<String>) customData.get("loginIPs");
            if (loginIPs == null) {
                loginIPs = new ArrayList<>();
            }

            if (loginIPs.contains(ipAddress)) {
                // they've already logged in from this location
                log.info("{} has already logged in from: {}. No message sent.", account.getEmail(), ipAddress);
                return true;
            }

            loginIPs.add(ipAddress);
            customData.put("loginIPs", loginIPs);
            customData.save();

            sendTwilioMessage(toNumber, "New login for: " + account.getEmail() + ", from: " + ipAddress);

            return true;
        };
    }

    private boolean isTwilioConfigured() {
        return
            Strings.hasText(twilioAccountSid) && Strings.hasText(twilioAuthToken) && Strings.hasText(twilioFromNumber);
    }

    private void sendTwilioMessage(String toNumber, String msg) {
        TwilioRestClient client = new TwilioRestClient(twilioAccountSid, twilioAuthToken);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("To", toNumber));
        params.add(new BasicNameValuePair("From", twilioFromNumber));
        params.add(new BasicNameValuePair("Body", msg));

        MessageFactory messageFactory = client.getAccount().getMessageFactory();
        Message message = null;
        try {
            message = messageFactory.create(params);
            log.info("Message successfuly sent via Twilio. Sid: {}", message.getSid());
        } catch (TwilioRestException e) {
            log.error("Error communicating with Twilio: {}", e.getErrorMessage(), e);
        }
    }
}
