package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: NexusComponentPo
 * @Author: lanhb
 * @Description: 用来定义nexus的component信息
 * @Date: 2019/11/14 9:56
 * @Version: 1.0
 */
public class NexusComponentPo {

    private String id;

    private String repository;

    private String format;

    private String group;

    private String name;

    private String version;

    private NexusAssetInfo[] assets;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public NexusAssetInfo[] getAssets() {
        return assets;
    }

    public void setAssets(NexusAssetInfo[] assets) {
        this.assets = assets;
    }
}
