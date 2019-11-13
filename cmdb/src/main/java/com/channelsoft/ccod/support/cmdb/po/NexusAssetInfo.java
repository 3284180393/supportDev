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

    private String md5; //nexus api返回的asset信息中的md5

    private String sha1; //nexus api返回的asset信息中的sha1

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
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }
}
