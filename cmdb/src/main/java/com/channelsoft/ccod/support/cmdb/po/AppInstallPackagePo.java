package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;

import java.util.Date;

/**
 * @ClassName: AppInstallPackagePo
 * @Author: lanhb
 * @Description: 用来定义应用安装包的类
 * @Date: 2019/11/21 13:31
 * @Version: 1.0
 */
public class AppInstallPackagePo extends AppFilePo {

    private int packageId; //发布包id,数据库唯一生成主键

    public AppInstallPackagePo()
    {}

    public AppInstallPackagePo(int appId, String deployPath, NexusAssetInfo assetInfo)
    {
        super(appId, deployPath, assetInfo);
    }

    public AppInstallPackagePo(int appId, DeployFileInfo packageInfo)
    {
        super(appId, packageInfo);
    }

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

}
