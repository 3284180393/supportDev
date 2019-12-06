package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: DownloadFileInfo
 * @Author: lanhb
 * @Description: 定义下载文件信息
 * @Date: 2019/12/4 10:35
 * @Version: 1.0
 */
public class DownloadFileInfo {
    private String fileName; //文件名

    private String downloadUrl; //文件下载地址

    private String md5; //文件md5

    public DownloadFileInfo(String fileName, String downloadUrl, String md5)
    {
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.md5 = md5;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
