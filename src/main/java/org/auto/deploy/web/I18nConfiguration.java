package org.auto.deploy.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author xiangqian
 * @date 02:22 2022/09/16
 */
@Configuration
public class I18nConfiguration implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        return customLocaleResolver();
    }

    private LocaleResolver customLocaleResolver() {
        return new LocaleResolver() {

            private Function<String, Locale> function = language -> {
                if ("zh".equals(language)) {
                    return Locale.SIMPLIFIED_CHINESE;
                } else if ("en".equals(language)) {
                    return Locale.US;
                }
                return Locale.SIMPLIFIED_CHINESE;
            };

            @Override
            public Locale resolveLocale(HttpServletRequest request) {

                // 从URL获取lang值
                String lang = request.getParameter("lang");
                if (Objects.nonNull(lang)) {
                    return function.apply(lang);
                }

                // 从请求头获取 Accept-Language
                // request.getHeader("Accept-Language")
                Locale locale = request.getLocale();
                if (Objects.nonNull(locale)) {
                    return function.apply(locale.getLanguage());
                }

                return Locale.SIMPLIFIED_CHINESE;
            }

            @Override
            public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
            }
        };
    }

    private SessionLocaleResolver sessionLocaleResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();

        // 设置默认语言环境
        slr.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);

        return slr;
    }

    private CookieLocaleResolver cookieLocaleResolver() {
        CookieLocaleResolver clr = new CookieLocaleResolver();

        // 将语言信息添加到Cookie中
        clr.setCookieName("locale");

        // 设置默认语言环境
        clr.setDefaultLocale(Locale.SIMPLIFIED_CHINESE); // 默认简体中文
//        clr.setDefaultLocale(Locale.US);

        // 设置cookie有效期，单位：s
        clr.setCookieMaxAge(60 * 60 * 24 * 3); // 3天

        return clr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        // 前端传参名
        lci.setParamName("lang");
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

}
