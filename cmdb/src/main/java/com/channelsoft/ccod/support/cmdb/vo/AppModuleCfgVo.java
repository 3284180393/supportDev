package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: AppModuleCfgVo
 * @Author: lanhb
 * @Description: 应用模块配置文件信息
 * @Date: 2019/11/15 10:19
 * @Version: 1.0
 */
public class AppModuleCfgVo {
    private String fileName;  //配置文件名

    private String filePath; //配置文件存放路径

    private long fileSize; //配置文件大小

    private String md5; //配置文件md5

    private String localSavePath; //cfg文件传输过来后本地保存路径

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getLocalSavePath() {
        return localSavePath;
    }

    public void setLocalSavePath(String localSavePath) {
        this.localSavePath = localSavePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
