package com.stormpath.spring.boot.examples.controller;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.servlet.account.AccountResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class PhoneController {

    @Value("#{ @environment['stormpath.web.login.uri'] ?: '/login' }")
    protected String loginUri;

    @Value("#{ @environment['stormpath.customData.phoneNumber.identifier'] ?: 'phoneNumber' }")
    protected String phoneNumberIdentifier;

    @RequestMapping(path = "/phone", method = GET)
    public String getPhoneForm(HttpServletRequest req, Model model) {
        Account account = AccountResolver.INSTANCE.getAccount(req);
        if (account == null) {
            return "redirect:" + loginUri + "?next=/phone";
        }

        model.addAttribute("phoneNumberIdentifier", phoneNumberIdentifier);

        return "phone";
    }

    @RequestMapping(path = "/phone", method = POST)
    public String savePhoneNumber(HttpServletRequest req, @RequestParam String phoneNumber) {
        Account account = AccountResolver.INSTANCE.getAccount(req);
        if (account == null) {
            return "redirect:" + loginUri + "?next=/phone";
        }

        account.getCustomData().put(phoneNumberIdentifier, phoneNumber);
        account.getCustomData().save();

        return "redirect:/?phoneSaved";
    }
}
