package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.command.BaseModel;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;

import java.util.Date;

/**
 * @ClassName: AppCfgFilePo
 * @Author: lanhb
 * @Description: 用来定义app配置文件的类
 * @Date: 2019/11/12 16:46
 * @Version: 1.0
 */
public class AppCfgFilePo extends AppFilePo {

    private int cfgFileId; //配置文件id,数据库唯一生成

    public AppCfgFilePo()
    {}

    public AppCfgFilePo(int appId, String deployPath, NexusAssetInfo assetInfo)
    {
        super(appId, deployPath, assetInfo);
    }

    public AppCfgFilePo(int appId, DeployFileInfo cfgFileInfo)
    {
        super(appId, cfgFileInfo);
    }

    public int getCfgFileId() {
        return cfgFileId;
    }

    public void setCfgFileId(int cfgFileId) {
        this.cfgFileId = cfgFileId;
    }

}
