package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformThreePartAppPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformThreePartAppMapper
 * @Author: lanhb
 * @Description: 用来定义PlatformThreePartAppPo类的dao接口
 * @Date: 2020/6/4 15:45
 * @Version: 1.0
 */
@Component
public interface PlatformThreePartAppMapper {

    /**
     * 根据id查找指定条件的平台第三方应用
     * @param threePartAppId 平台第三方应用id
     * @return 查询结果
     */
    PlatformThreePartAppPo selectByPrimaryKey(int threePartAppId);

    /**
     * 查询指定平台下所有第三方应用
     * @param platformId 平台id
     * @return 查询结果
     */
    List<PlatformThreePartAppPo> select(String platformId);

    /**
     * 向数据库添加一条平台第三方应用
     * @param threePartAppPo 需要添加的平台第三方应用
     */
    void insert(PlatformThreePartAppPo threePartAppPo);

    /**
     * 修改已有的平台第三方应用信息
     * @param threePartAppPo
     */
    void update(PlatformThreePartAppPo threePartAppPo);

    /**
     * 删除指定条件的平台第三方应用
     * @param platformId 平台id
     * @param threePartAppId 平台第三方id
     */
    void delete(@Param("platformId")String platformId,  @Param("threePartAppId")Integer threePartAppId);
}
