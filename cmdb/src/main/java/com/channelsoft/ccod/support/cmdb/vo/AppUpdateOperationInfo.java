package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.UpdateStatus;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: AppUpdateOperationInfo
 * @Author: lanhb
 * @Description: 用来定义应用升级操作信息
 * @Date: 2019/12/11 17:27
 * @Version: 1.0
 */
public class AppUpdateOperationInfo {

    private int platformAppId; //平台应用id,如果操作是ADD为0,否则是被操作的平台应用id

    private AppUpdateOperation operation; //应用升级类型,由AppUpdateType枚举定义

    private String appAlias; //应用别名

    private int originalAppId; //操作前应用id,该id由app表定义,如果是ADD操作，该属性为0

    private int targetAppId; //操作后应用id,该id由app表定义,如果是DELETE或是CFG_UPDATE、STOP、START操作则该参数为0

    private int bzHostId; //对该应用所在的服务器id,该id由蓝鲸paas平台唯一生成

    private String basePath; //该应用所在的base path

    private String appRunner; //该应用的执行用户

    private List<NexusAssetInfo> cfgs; //如果升级成功,需要返回升级后的应用配置在nexus中的存储信息

    public AppUpdateOperation getOperation() {
        return operation;
    }

    public void setOperation(AppUpdateOperation operation) {
        this.operation = operation;
    }

    public int getOriginalAppId() {
        return originalAppId;
    }

    public void setOriginalAppId(int originalAppId) {
        this.originalAppId = originalAppId;
    }

    public int getTargetAppId() {
        return targetAppId;
    }

    public void setTargetAppId(int targetAppId) {
        this.targetAppId = targetAppId;
    }

    public int getBzHostId() {
        return bzHostId;
    }

    public void setBzHostId(int bzHostId) {
        this.bzHostId = bzHostId;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getAppRunner() {
        return appRunner;
    }

    public void setAppRunner(String appRunner) {
        this.appRunner = appRunner;
    }

    public List<NexusAssetInfo> getCfgs() {
        return cfgs;
    }

    public void setCfgs(List<NexusAssetInfo> cfgs) {
        this.cfgs = cfgs;
    }

    public String getAppAlias() {
        return appAlias;
    }

    public void setAppAlias(String appAlias) {
        this.appAlias = appAlias;
    }

    public int getPlatformAppId() {
        return platformAppId;
    }

    public void setPlatformAppId(int platformAppId) {
        this.platformAppId = platformAppId;
    }
}
