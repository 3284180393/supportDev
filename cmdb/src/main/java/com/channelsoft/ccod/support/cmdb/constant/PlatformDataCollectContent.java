package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: PlatformDataCollectContent
 * @Author: lanhb
 * @Description: 用来定义平台收集内容的枚举
 * @Date: 2020/3/13 11:33
 * @Version: 1.0
 */
public enum PlatformDataCollectContent {

    APP_MODULE(1, "APP_MODULE", "收集CCOD应用模块的安装包、版本以及配置文件"),

    APP_VERSION(2, "APP_VERSION", "收集CCOD应用模块的版本"),

    APP_CFG(3, "APP_CFG", "收集CCOD应用的配置文件"),;

    public int id;

    public String name;

    public String desc;

    private PlatformDataCollectContent(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static PlatformDataCollectContent getEnumByName(String name)
    {
        if(name == null)
            return null;
        for (PlatformDataCollectContent type : PlatformDataCollectContent.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }

    public static PlatformDataCollectContent getEnumById(int id)
    {
        for (PlatformDataCollectContent type : PlatformDataCollectContent.values())
        {
            if (type.id == id)
            {
                return type;
            }
        }
        return null;
    }
}
