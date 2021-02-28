package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.SystemFamily;

import java.util.Map;

/**
 * @ClassName: HostConfig
 * @Author: lanhb
 * @Description: 用来定义主机配置
 * @Date: 2020/12/11 16:36
 * @Version: 1.0
 */
public class HostConfig {

    private String ip; //主机ip

    private String user; //用户名，缺省root

    private String password; //用户的登录密码

    private int sshPort; //服务器的ssh登陆端口

    private SystemFamily systemFamily; //服务器的系统类型

    private String initCmd; //用来执行服务器初始化脚本

    private Map<String, String> cfg; //用来定义服务器相关配置，例如jdk7,jkd8,resin4,tomcat6路径

    private Map<String, String> env; //用来定义服务器的环境变量

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getInitCmd() {
        return initCmd;
    }

    public void setInitCmd(String initCmd) {
        this.initCmd = initCmd;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public Map<String, String> getCfg() {
        return cfg;
    }

    public void setCfg(Map<String, String> cfg) {
        this.cfg = cfg;
    }

    public SystemFamily getSystemFamily() {
        return systemFamily;
    }

    public void setSystemFamily(SystemFamily systemFamily) {
        this.systemFamily = systemFamily;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
}
