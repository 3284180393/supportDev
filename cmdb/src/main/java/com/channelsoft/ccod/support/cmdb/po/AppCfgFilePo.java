package com.channelsoft.ccod.support.cmdb.po;

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
@Table(name = "app_cfg_file")
public class AppCfgFilePo extends BaseModel {

    @Column(name = "cfg_file_id",type = MySqlTypeConstant.INT,length = 11,isKey = true,isAutoIncrement = true)
    private int cfgFileId; //配置文件id,数据库唯一生成

    @Column(name = "app_id",type = MySqlTypeConstant.INT,length = 11, isNull = false)
    private int appId; //该配置文件是哪个app的配置文件,外键app表的app_id

    @Column(name = "file_name",type = MySqlTypeConstant.VARCHAR,length = 40,isNull = false)
    private String fileName; //配置文件名

    @Column(name = "save_path",type = MySqlTypeConstant.VARCHAR,length = 255,isNull = false)
    private String savePath; //文件存放路径,可以是相对app的base path的相对路径,也可以是绝对路径

    @Column(name = "nexus_name",type = MySqlTypeConstant.VARCHAR,length = 128,isNull = false)
    private String nexusName; //该配置文件在nexus中的name

    @Column(name = "create_time",type = MySqlTypeConstant.DATETIME, isNull = false)
    private Date createTime; //该文件在nexus的创建时间

    public int getCfgFileId() {
        return cfgFileId;
    }

    public void setCfgFileId(int cfgFileId) {
        this.cfgFileId = cfgFileId;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public String getNexusName() {
        return nexusName;
    }

    public void setNexusName(String nexusName) {
        this.nexusName = nexusName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
