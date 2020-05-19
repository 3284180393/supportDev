package com.channelsoft.ccod.support.cmdb.config;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.filter.HttpPutFormContentFilter;

/**
 * @ClassName: PutFilter
 * @Author: lanhb
 * @Description: 用来配置springboot允许PUT
 * @Date: 2020/5/19 9:32
 * @Version: 1.0
 */
@Component
public class PutFilter extends HttpPutFormContentFilter {
}
