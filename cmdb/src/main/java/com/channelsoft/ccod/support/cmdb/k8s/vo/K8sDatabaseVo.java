package com.channelsoft.ccod.support.cmdb.k8s.vo;

import com.channelsoft.ccod.support.cmdb.constant.DatabaseType;

/**
 * @ClassName: K8sDatabaseVo
 * @Author: lanhb
 * @Description: 用来定义部署在k8s上的数据库(oracle, mysql)等
 * @Date: 2020/8/13 11:09
 * @Version: 1.0
 */
public class K8sDatabaseVo {

    private K8sThreePartAppVo k8sDB;

    private DatabaseType dbType;

    private String ip;

    private String user;

    private String pwd;

    private int port;

    private String sid;

    private K8sDatabaseVo() {}

    public static K8sDatabaseVo getOracleInstance(String ip, String user, String pwd, int port, String sid)
    {
        K8sDatabaseVo db = new K8sDatabaseVo();
        db.ip = ip;
        db.user = user;
        db.pwd = pwd;
        db.port = port;
        db.sid = sid;
        return db;
    }

    public K8sThreePartAppVo getK8sDB() {
        return k8sDB;
    }

    public void setK8sDB(K8sThreePartAppVo k8sDB) {
        this.k8sDB = k8sDB;
    }

    public DatabaseType getDbType() {
        return dbType;
    }

    public void setDbType(DatabaseType dbType) {
        this.dbType = dbType;
    }

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

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }
}
