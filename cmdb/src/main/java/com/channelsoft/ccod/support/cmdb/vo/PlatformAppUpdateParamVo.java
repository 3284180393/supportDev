package com.channelsoft.ccod.support.cmdb.vo;

import java.util.List;

/**
 * @ClassName: PlatformAppUpdateParamVo
 * @Author: lanhb
 * @Description: 用来定义平台应用更新的参数
 * @Date: 2020/5/19 9:02
 * @Version: 1.0
 */
public class PlatformAppUpdateParamVo {

    private String platformId;

    private String platformName;

    private List<AppUpdateOperationInfo> updateAppList;

    public String getPlatformId() {
        return platformId;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public List<AppUpdateOperationInfo> getUpdateAppList() {
        return updateAppList;
    }

    public void setUpdateAppList(List<AppUpdateOperationInfo> updateAppList) {
        this.updateAppList = updateAppList;
    }
}
