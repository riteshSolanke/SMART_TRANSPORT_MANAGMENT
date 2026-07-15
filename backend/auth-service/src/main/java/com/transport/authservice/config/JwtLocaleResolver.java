package com.transport.authservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

public class JwtLocaleResolver extends AcceptHeaderLocaleResolver {

    public static final String LOCALE_ATTRIBUTE = "RESOLVED_LOCALE";

    @Override
    public Locale resolveLocale(HttpServletRequest req){

        Object attributeLocale = req.getAttribute(LOCALE_ATTRIBUTE);

        if(attributeLocale instanceof  Locale locale){
            return locale;
        }
    return super.resolveLocale(req);

    }
}
