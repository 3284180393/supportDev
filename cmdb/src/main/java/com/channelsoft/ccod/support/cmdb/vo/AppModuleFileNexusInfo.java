package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: AppModuleFileNexusInfo
 * @Author: lanhb
 * @Description: 用来定义应用文件的相关信息
 * @Date: 2019/11/29 15:13
 * @Version: 1.0
 */
public class AppModuleFileNexusInfo {

    private String repository; //文件在nexus的仓库名

    private String nexusName; //文件在nexus asset信息中的name

    private String md5; //文件的md5

    private String deployPath; //文件的目标存放路径,相对应用的base path

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getNexusName() {
        return nexusName;
    }

    public void setNexusName(String nexusName) {
        this.nexusName = nexusName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getDeployPath() {
        return deployPath;
    }

    public void setDeployPath(String deployPath) {
        this.deployPath = deployPath;
    }
}
