package com.channelsoft.ccod.support.cmdb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.joda.time.DateTime;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * @ClassName: CustomConfiguration
 * @Author: lanhb
 * @Description: 用来定义Gson作为json转换类
 * @Date: 2020/6/12 16:52
 * @Version: 1.0
 */
@Configuration
public class CustomConfiguration {
    @Bean
    public HttpMessageConverters customConverters() {
        Collection<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
        Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new GsonDateUtil()).create();
        gsonHttpMessageConverter.setGson(gson);
        messageConverters.add(gsonHttpMessageConverter);
        return new HttpMessageConverters(true, messageConverters);
    }
}
