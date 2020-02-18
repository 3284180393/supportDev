package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: PlatformCreateParamVo
 * @Author: lanhb
 * @Description: 平台创建参数
 * @Date: 2020/2/18 22:02
 * @Version: 1.0
 */
public class PlatformCreateParamVo {

    public final static int MANUAL = 1;   //通过手动创建一个新的空平台升级计划

    public final static int CLONE = 2; //从已有的平台克隆而来

    public final static int PREDEFINE = 3; //从预定义脚本创建

    private int createMethod; //平台创建方式

    private String platformId; //新建平台id

    private String platformName; //新建平台名

    private int bkBizId; //新建平台的biz id

    private int bkCloudId; //新建平台服务器所在的cloud id

    private String params; //同平台创建相关的参数

    public static int getMANUAL() {
        return MANUAL;
    }

    public static int getCLONE() {
        return CLONE;
    }

    public static int getPREDEFINE() {
        return PREDEFINE;
    }

    public int getCreateMethod() {
        return createMethod;
    }

    public void setCreateMethod(int createMethod) {
        this.createMethod = createMethod;
    }

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

    public int getBkBizId() {
        return bkBizId;
    }

    public void setBkBizId(int bkBizId) {
        this.bkBizId = bkBizId;
    }

    public int getBkCloudId() {
        return bkCloudId;
    }

    public void setBkCloudId(int bkCloudId) {
        this.bkCloudId = bkCloudId;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}
