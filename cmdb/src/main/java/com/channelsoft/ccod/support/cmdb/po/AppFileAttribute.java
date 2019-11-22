package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: AppFileAttribute
 * @Author: lanhb
 * @Description: 用来定义应用关联文件属性
 * @Date: 2019/11/22 13:56
 * @Version: 1.0
 */
public class AppFileAttribute {
    public String fileType;
    public String platformId;
    public String domainId;
    public String hostIp;
    public String appName;
    public String appAlias;
    public String version;
    public String basePath;
    public String deployPath;
    public String fileName;
    public String ext;
    public long fileSize;
    public String md5;

    public AppFileAttribute(String fileType, String platformId, String domainId, String hostIp, String appName,
                            String appAlias, String version, String basePath, String deployPath, String fileName,
                            String ext, long fileSize, String md5)
    {
        this.fileType = fileType;
        this.platformId = platformId;
        this.domainId = domainId;
        this.hostIp = hostIp;
        this.appName = appName;
        this.appAlias = appAlias;
        this.version = version;
        this.basePath = basePath;
        this.deployPath = deployPath;
        this.fileName = fileName;
        this.ext = ext;
        this.fileSize = fileSize;
        this.md5 = md5;
    }

    public AppFileAttribute()
    {

    }

}
