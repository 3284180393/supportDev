package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: ConfigKey
 * @Author: lanhb
 * @Description: 用来定义配置中的k, v以及tag
 * @Date: 2020/12/11 16:30
 * @Version: 1.0
 */
public class ConfigKey {
    private String key;

    private String value;

    private String tag;

    public ConfigKey(String key, String value, String tag){
        this.key = key;
        this.value = value;
        this.tag = tag;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
