package com.channelsoft.ccod.support.cmdb.po;

import lombok.Getter;
import lombok.Setter;

/**
 * @ClassName: SubSystemPo
 * @Author: lanhb
 * @Description: 用来定义ccod子系统的pojo类
 * @Date: 2020/9/24 14:48
 * @Version: 1.0
 */
@Getter
@Setter
public class SubSystemPo {

    private String id; //子系统id,主键

    private String name; //子系统名称

    private String leader; //系统负载人

    private String setName; //归属的业务集群

    private String level; //系统级别

    private String comment; //说明

    @Override
    public String toString()
    {
        return String.format("%s(%s):%s,负责人%s,系统级别%s,归属业务集群%s", name, id, comment, leader, level, setName);
    }
}
