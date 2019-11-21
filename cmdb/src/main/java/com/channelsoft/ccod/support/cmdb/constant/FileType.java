package com.channelsoft.ccod.support.cmdb.constant;

/**
 * @ClassName: FileType
 * @Author: lanhb
 * @Description: 用来定义文件类型的枚举
 * @Date: 2019/11/21 13:55
 * @Version: 1.0
 */
public enum  FileType {
    binary(1, "binary", "可直接运行的二进制文件"),

    zip(2, "zip", "zip压缩包"),

    tar(3, "tar", "tar压缩包"),

    rar(4, "rar", "rar压缩包"),

    war(5, "war", "war包"),

    ini(6, "ini", "键值对类型的配置文件"),

    yml(7, "yml", "yml类型的配置文件"),

    xml(8, "xml", "xml格式的配置文件"),

    text(5, "text", "纯文本文件"),;

    public int id;

    public String name;

    public String desc;

    private FileType(int id, String name, String desc)
    {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public static FileType getEnum(String name)
    {
        if(name == null)
            return null;
        for (FileType type : FileType.values())
        {
            if (type.name.equals(name))
            {
                return type;
            }
        }
        return null;
    }
}
