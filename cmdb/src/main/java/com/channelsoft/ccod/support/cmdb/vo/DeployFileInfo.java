package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.po.AppFilePo;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;

/**
 * @ClassName: DeployFileInfo
 * @Author: lanhb
 * @Description: 部署的文件信息
 * @Date: 2019/11/18 10:18
 * @Version: 1.0
 */
public class DeployFileInfo {

    private String deployPath; //文件的部署路径

    private String fileName; //文件名

    private long fileSize; //文件大小

    private String fileMd5; //文件的md5

    private String localSavePath; //该文件被传输过来后的本地存储路径

    private String nexusRepository; //该文件被上传到nexus的repository

    private String nexusDirectory; //该文件被上传到nexus的directory

    private String nexusAssetId; //在nexus库中的assetId

    private String ext; //文件扩展类型

    private boolean transferSucc; //源文件是否传输成功

    private String transferFailReason; //源文件传输失败原因

//    public DeployFileInfo(String fileName, String savePath)
//    {
//        this.fileName = fileName;
//        this.localSavePath = savePath;
//    }

    public DeployFileInfo(AppFilePo filePo, String savePath)
    {
        this.fileMd5 = filePo.getMd5();
        this.localSavePath = savePath;
        this.deployPath = filePo.getDeployPath();
        this.fileName = filePo.getFileName();
        this.ext = filePo.getExt();
    }

    public DeployFileInfo(AppFileNexusInfo filePo, String savePath)
    {
        this.fileMd5 = filePo.getMd5();
        this.localSavePath = savePath;
        this.deployPath = filePo.getDeployPath();
        this.fileName = filePo.getFileName();
        this.ext = filePo.getExt();
    }

    public DeployFileInfo(NexusAssetInfo assetInfo, String savePath)
    {
        this.fileMd5 = assetInfo.getMd5();
        this.localSavePath = savePath;
        this.fileName = assetInfo.getNexusAssetFileName();
        String[] arr = this.fileName.split("\\.");
        this.ext = arr.length > 1 ? arr[arr.length -1] : null;
    }

    public DeployFileInfo()
    {

    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getLocalSavePath() {
        return localSavePath;
    }

    public void setLocalSavePath(String localSavePath) {
        this.localSavePath = localSavePath;
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

    public String getNexusAssetId() {
        return nexusAssetId;
    }

    public void setNexusAssetId(String nexusAssetId) {
        this.nexusAssetId = nexusAssetId;
    }

    public String getNexusRepository() {
        return nexusRepository;
    }

    public void setNexusRepository(String nexusRepository) {
        this.nexusRepository = nexusRepository;
    }

    public String getNexusDirectory() {
        return nexusDirectory;
    }

    public void setNexusDirectory(String nexusDirectory) {
        this.nexusDirectory = nexusDirectory;
    }

    public boolean isTransferSucc() {
        return transferSucc;
    }

    public void setTransferSucc(boolean transferSucc) {
        this.transferSucc = transferSucc;
    }

    public String getTransferFailReason() {
        return transferFailReason;
    }

    public void setTransferFailReason(String transferFailReason) {
        this.transferFailReason = transferFailReason;
    }
}
