package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: ServerUserPo
 * @Author: lanhb
 * @Description: 用来定义服务器用户
 * @Date: 2019/11/12 16:17
 * @Version: 1.0
 */
public class ServerUserPo {

    private int userId; //用户id,数据库

    private String userName; //用户名

    private String password; //登录密码

    private int serverId; //用户所在服务器的id

    private int loginMethod; //登录方式, 1:ssh

    private String comment; //备注

    private int sshPort; //ssh端口

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getServerId() {
        return serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public int getLoginMethod() {
        return loginMethod;
    }

    public void setLoginMethod(int loginMethod) {
        this.loginMethod = loginMethod;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
}
