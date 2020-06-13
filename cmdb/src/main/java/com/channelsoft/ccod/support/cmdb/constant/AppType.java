package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: AppType
 * @Author: lanhb
 * @Description: 用来定义app类型
 * @Date: 2019/11/12 14:15
 * @Version: 1.0
 */
public enum AppType
{
    CCOD_KERNEL_MODULE(1, "CCOD_KERNEL_MODULE", "ccod核心模块"),

    THREE_PART_APP(2, "THREE_PART_APP", "第三方应用,例如oracle、mysql等"),

    CCOD_WEBAPPS_MODULE(3, "CCOD_WEBAPPS_MODULE", "ccod门户web应用"),

    OTHER(99, "OTHER", "其它"),;

    public int id;

    public String name;

    public String desc;

    private AppType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static AppType getEnum(String name)
    {
        if(name == null)
            return null;
        for (AppType type : AppType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }
}
