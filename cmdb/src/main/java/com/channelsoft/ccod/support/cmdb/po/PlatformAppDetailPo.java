package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: PlatformAppDetailPo
 * @Author: lanhb
 * @Description: 用来定义平台应用详情, 这里的详情可以是任意信息, 用键值对表示
 * @Date: 2019/11/12 17:40
 * @Version: 1.0
 */
public class PlatformAppDetailPo {
    private int id; //数据库唯一主键

    private int platformAppId; //平台应用id,外键platform_app表主键

    private String key; //应用详情中的key

    private String value; //应用详情中的value

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
    }
}
