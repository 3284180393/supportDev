package com.channelsoft.ccod.support.cmdb.vo;

import java.util.List;

/**
 * @ClassName: NginxConfig
 * @Author: lanhb
 * @Description: 用来定义nginx配置
 * @Date: 2020/12/11 16:18
 * @Version: 1.0
 */
public class NginxConfig {

    private String ip; //nginx部署服务器的ip

    private String conf; //nginx配置文件路径

    private String http; //nginx监听的http端口

    private String https; //nginx定义的https端口

    private List<String> headers; //用来定义一组proxy-set-header

    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    public String getHttp() {
        return http;
    }

    public void setHttp(String http) {
        this.http = http;
    }

    public String getHttps() {
        return https;
    }

    public void setHttps(String https) {
        this.https = https;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
