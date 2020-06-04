package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformThreePartServicePo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformThreePartServiceMServiceer
 * @Author: lanhb
 * @Description: PlatformThreePartServicePo类的dao接口
 * @Date: 2020/6/4 16:15
 * @Version: 1.0
 */
@Component
public interface PlatformThreePartServiceMapper {

    /**
     * 根据id查找指定条件的平台第三方服务
     * @param threePartServiceId 平台第三方服务id
     * @return 查询结果
     */
    PlatformThreePartServicePo selectByPrimaryKey(int threePartServiceId);

    /**
     * 查询指定平台下所有第三方服务
     * @param platformId 平台id
     * @return 查询结果
     */
    List<PlatformThreePartServicePo> select(String platformId);

    /**
     * 向数据库添加一条平台第三方服务
     * @param threePartServicePo 需要添加的平台第三方服务
     */
    void insert(PlatformThreePartServicePo threePartServicePo);

    /**
     * 修改已有的平台第三方服务信息
     * @param threePartServicePo
     */
    void update(PlatformThreePartServicePo threePartServicePo);

    /**
     * 删除指定条件的平台第三方服务
     * @param platformId 平台id
     * @param threePartServiceId 平台第三方id
     */
    void delete(@Param("platformId")String platformId, @Param("threePartServiceId")Integer threePartServiceId);
}
