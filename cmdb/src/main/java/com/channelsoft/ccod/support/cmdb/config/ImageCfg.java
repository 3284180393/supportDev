package com.channelsoft.ccod.support.cmdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @ClassName: ImageCfg
 * @Author: lanhb
 * @Description: 用来定义同镜像相关的配置类
 * @Date: 2020/6/13 9:50
 * @Version: 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "image")
public class ImageCfg {

    private String nexusUrl;

    private String nexusUser;

    private String nexusPwd;

    private List<String> ccodModuleRepository;

    private List<String> threeAppRepository;

    public String getNexusUrl() {
        return nexusUrl;
    }

    public void setNexusUrl(String nexusUrl) {
        this.nexusUrl = nexusUrl;
    }

    public String getNexusUser() {
        return nexusUser;
    }

    public void setNexusUser(String nexusUser) {
        this.nexusUser = nexusUser;
    }

    public String getNexusPwd() {
        return nexusPwd;
    }

    public void setNexusPwd(String nexusPwd) {
        this.nexusPwd = nexusPwd;
    }

    public List<String> getCcodModuleRepository() {
        return ccodModuleRepository;
    }

    public void setCcodModuleRepository(List<String> ccodModuleRepository) {
        this.ccodModuleRepository = ccodModuleRepository;
    }

    public List<String> getThreeAppRepository() {
        return threeAppRepository;
    }

    public void setThreeAppRepository(List<String> threeAppRepository) {
        this.threeAppRepository = threeAppRepository;
    }
}
