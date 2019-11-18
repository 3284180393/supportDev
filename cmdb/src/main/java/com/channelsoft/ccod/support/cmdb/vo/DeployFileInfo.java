package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: DeployFileInfo
 * @Author: lanhb
 * @Description: 部署的文件信息
 * @Date: 2019/11/18 10:18
 * @Version: 1.0
 */
public class DeployFileInfo {
    private String basePath; //文件的base path

    private String deployPath; //文件的部署路径

    private String fileName; //文件名

    private long fileSize; //文件大小

    private String fileMd5; //文件的md5

    private String localSavePath; //该文件被传输过来后的本地存储路径

    public String getLocalSavePath() {
        return localSavePath;
    }

    public void setLocalSavePath(String localSavePath) {
        this.localSavePath = localSavePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }
}
