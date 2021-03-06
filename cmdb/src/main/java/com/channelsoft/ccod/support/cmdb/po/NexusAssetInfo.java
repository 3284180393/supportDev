package com.channelsoft.ccod.support.cmdb.po;

/**
 * @ClassName: NexusAssetInfo
 * @Author: lanhb
 * @Description: 定义nexus的asset信息
 * @Date: 2019/11/13 20:21
 * @Version: 1.0
 */
public class NexusAssetInfo {
    private String downloadUrl;  //nexus api返回的asset信息中的downloadUrl

    private String path; //nexus api返回的asset信息中的path

    private String id; //nexus api返回的asset信息中的id

    private String repository; //nexus api返回的asset信息中的repository

    private String format; //nexus api返回的asset信息中的format

    private Checksum checksum; //文件特征值

    public Checksum getChecksum() {
        return checksum;
    }

    public void setChecksum(Checksum checksum) {
        this.checksum = checksum;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

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

    public String getMd5() {
        return this.checksum != null ? this.checksum.md5 : null;
    }

    public String getNexusAssetFileName()
    {
        String[] arr = this.path.split("/");
        return arr[arr.length - 1];
    }

}
