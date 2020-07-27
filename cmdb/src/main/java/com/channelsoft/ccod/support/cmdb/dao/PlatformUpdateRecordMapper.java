package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformUpdateRecordPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformUpdateRecordMapper
 * @Author: lanhb
 * @Description: PlatformUpdateRecordPo类的dao接口类
 * @Date: 2020/7/27 15:30
 * @Version: 1.0
 */
@Component
@Mapper
public interface PlatformUpdateRecordMapper {

    /**
     * 向数据库添加一条平台升级记录
     * @param updateRecord
     */
    void insert(PlatformUpdateRecordPo updateRecord);

    /**
     * 查询指定条件的平台升级记录
     * @param platformId 平台id
     * @param isLastUpdate 是否是最后一次更新
     * @return 查询结果
     */
    List<PlatformUpdateRecordPo> select(@Param("platformId")String platformId, @Param("isLastUpdate")Boolean isLastUpdate);


    /**
     * 查询指定job id的平台升级记录
     * @param jobId job id
     * @return 查询结果
     */
    PlatformUpdateRecordPo selectByJobId(String jobId);

    /**
     * 删除指定条件的平台升级记录
     * @param platformId 平台id
     * @param jobId job id
     */
    void delete(@Param("platformId")String platformId, @Param("jobId")String jobId);

    /**
     * 修改已有的平台升级记录
     * @param updateRecord 需要修改的平台升级记录
     */
    void update(PlatformUpdateRecordPo updateRecord);
}
