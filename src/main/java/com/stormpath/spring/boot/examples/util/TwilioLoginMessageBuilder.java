package com.stormpath.spring.boot.examples.util;

import com.stormpath.sdk.lang.Strings;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TwilioLoginMessageBuilder {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TwilioLoginMessageBuilder.class);

    private String accountSid;
    private String authToken;
    private String fromNumber;
    private String toNumber;

    private TwilioLoginMessageBuilder() {}

    public static TwilioLoginMessageBuilder builder() {
        return new TwilioLoginMessageBuilder();
    }

    public TwilioLoginMessageBuilder setAccountSid(String accountSid) {
        this.accountSid = accountSid;
        return this;
    }

    public TwilioLoginMessageBuilder setAuthToken(String authToken) {
        this.authToken = authToken;
        return this;
    }

    public TwilioLoginMessageBuilder setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
        return this;
    }

    public TwilioLoginMessageBuilder setToNumber(String toNumber) {
        this.toNumber = toNumber;
        return this;
    }

    public void send(String msg) {
        if (!isTwilioConfigured()) {
            log.warn(
                "Twilio not configured. Please set the following properties: " +
                "twilio.account.sid, twilio.auth.token, twilio.from.number"
            );
            return;
        }

        if (toNumber == null) {
            log.warn("No toNumber set. Cannot proceed.");
            return;
        }

        if (!Strings.hasText(msg)) {
            log.warn("The message to send via twilio is either null or emtpy. Cannot proceed.");
            return;
        }

        TwilioRestClient client = new TwilioRestClient(accountSid, authToken);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("To", toNumber));
        params.add(new BasicNameValuePair("From", fromNumber));
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

    private boolean isTwilioConfigured() {
        return Strings.hasText(accountSid) && Strings.hasText(authToken) && Strings.hasText(fromNumber);
    }
}
