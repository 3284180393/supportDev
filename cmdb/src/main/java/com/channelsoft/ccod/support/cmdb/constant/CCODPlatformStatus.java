package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: CCODPlatformStatus
 * @Author: lanhb
 * @Description: 用来定义ccod平台状态的枚举
 * @Date: 2019/12/2 16:34
 * @Version: 1.0
 */
public enum CCODPlatformStatus {

    UNKNOWN(0, "UNKNOWN", "未知"),

    RUNNING(1, "RUNNING", "该平台为运营状态,且cmdb和paas数据同步"),

    SUSPEND(2, "SUSPEND", "暂停使用"),

    STOPPING(3, "STOPPING", "下架"),

    NEW_CREATE(4, "NEW_CREATE", "新创建平台,paas已经创建biz,set并分配空闲主机,cmdb无相关记录"),

    WAIT_SYNC_TO_PAAS(5, "WAIT_SYNC_TO_PAAS", "需要将平台应用部署详情从cmdb同步到paas,cmdb有平台应用部署记录,paas已经创建biz有且仅有idle pool set并且没有分配任何空闲主机"),

    NEED_SYNC_FROM_PAAS(6, "NEED_SYNC_FROM_PAAS", "需要将平台应用部署详情从paas同步到cmdb,paas有对应biz,biz下有同ccod相关的所有set并且有app部署信息,cmdb无该平台任何信息"),;

    public int id;

    public String name;

    public String desc;

    private CCODPlatformStatus(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static CCODPlatformStatus getEnum(String name)
    {
        if(name == null)
            return null;
        for (CCODPlatformStatus type : CCODPlatformStatus.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }
}
