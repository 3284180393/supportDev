package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.ci.po.BuildDetailPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: BuildDetailMapper
 * @Author: lanhb
 * @Description: 用来定义应用构建结果的dao接口类
 * @Date: 2020/9/28 19:35
 * @Version: 1.0
 */
@Component
public interface BuildDetailMapper {

    /**
     * 向数据库添加一条新的构建结果记录
     * @param detail 构建结果记录
     */
    void insert(BuildDetailPo detail);

    /**
     * 查询指定条件的应用构建记录，如果某个条件为空则忽略该条件
     * @param jobName job名
     * @param appName 应用名
     * @param version 应用版本
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 查询结果
     */
    List<BuildDetailPo> select(@Param("jobName")String jobName,
                               @Param("appName")String appName,
                               @Param("version")String version,
                               @Param("startTime") Date startTime,
                               @Param("endTime") Date endTime);
}
