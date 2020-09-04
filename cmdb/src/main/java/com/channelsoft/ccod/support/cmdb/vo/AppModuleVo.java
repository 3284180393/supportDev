package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.po.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: AppModuleVo
 * @Author: lanhb
 * @Description: 用来定义应用模块信息的类
 * @Date: 2019/11/14 14:34
 * @Version: 1.0
 */
public class AppModuleVo extends AppBase{

    private int appId; //部署应用的id

    private Date createTime; //创建时间

    private String createReason; //创建原因

    private Date updateTime; //最后一次修改时间

    private VersionControl versionControl; //版本控制方式

    private String versionControlUrl; //版本控制的连接url

    private Boolean kernal; //该模块是否是核心模块，如果是核心模块则CREATE或是REPLACE对应的deployment时必须返回执行成功后才能执行后面操作

    private Integer timeout; //启动超时

    private String comment; //备注

    private Boolean hasImage; //是否有镜像

    public AppModuleVo()
    {

    }

    public AppModuleVo(AppPo app)
    {
        super(app);
        this.appId = app.getAppId();
        this.createTime = app.getCreateTime();
        this.createReason = app.getCreateReason();
        this.updateTime = app.getUpdateTime();
        this.versionControl = app.getVersionControl();
        this.versionControlUrl = app.getVersionControlUrl();
        this.kernal = app.isKernal();
        this.timeout = app.getTimeout();
        this.comment = app.getComment();
        this.hasImage = app.isHasImage();
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public VersionControl getVersionControl() {
        return versionControl;
    }

    public void setVersionControl(VersionControl versionControl) {
        this.versionControl = versionControl;
    }

    public String getVersionControlUrl() {
        return versionControlUrl;
    }

    public void setVersionControlUrl(String versionControlUrl) {
        this.versionControlUrl = versionControlUrl;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateReason() {
        return createReason;
    }

    public void setCreateReason(String createReason) {
        this.createReason = createReason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAppNexusUploadUrl(String nexusHostUrl, String repository) {
        String url = String.format("%s/service/rest/v1/components?repository=%s", nexusHostUrl, repository);
        return url;
    }

    public Boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(Boolean hasImage) {
        this.hasImage = hasImage;
    }

    public Boolean isKernal() {
        return kernal;
    }

    public void setKernal(Boolean kernal) {
        this.kernal = kernal;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public AppPo getApp()
    {
        AppPo po = new AppPo(this);
        po.setAppId(this.appId);
        po.setCreateTime(this.createTime);
        po.setCreateReason(this.createReason);
        po.setUpdateTime(this.updateTime);
        po.setVersionControl(this.versionControl);
        po.setVersionControlUrl(this.versionControlUrl);
        po.setKernal(this.kernal);
        po.setTimeout(this.timeout);
        po.setHasImage(this.hasImage);
        po.setComment(this.comment);
        return po;
    }

    public AppPo getApp(int appId)
    {
        AppPo po = getApp();
        po.setAppId(appId);
        return po;
    }

    public void update(AppModuleVo module)
    {
        this.changeTo(module);
        this.updateTime = new Date();
        this.versionControl = module.getVersionControl() != null ? module.getVersionControl() : this.versionControl;
        this.versionControlUrl = StringUtils.isNotBlank(module.getVersionControlUrl()) ? module.getVersionControlUrl() : this.versionControlUrl;
        this.kernal = module.isKernal() != null ? module.isKernal() : this.kernal;
        this.timeout = module.getTimeout() != null ? module.getTimeout() : this.timeout;
        this.comment = StringUtils.isNotBlank(module.getComment()) ? module.getComment() : this.comment;
        this.hasImage = module.isHasImage() != null ? module.isHasImage() : this.hasImage;
    }

    @Override
    public String toString()
    {
        return String.format("%s(%s)", this.appName, this.getVersion());
    }
}
