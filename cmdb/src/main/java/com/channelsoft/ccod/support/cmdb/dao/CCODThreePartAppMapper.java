package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.CCODThreePartAppPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: CCODThreePartAppMapper
 * @Author: lanhb
 * @Description: 用来定义CCODThreePartAppPo类的dao接口
 * @Date: 2020/11/18 17:23
 * @Version: 1.0
 */
@Component
public interface CCODThreePartAppMapper {

    /**
     * 向数据库添加一条特定的第三方应用依赖记录
     * @param appPo 需要添加的第三方应用依赖记录
     */
    void insert(CCODThreePartAppPo appPo);

    /**
     * 查询特定的平台第三方依赖记录
     * @param ccodVersion ccod大版本号
     * @param tag 指定大版本号的特定标签
     * @param appName 第三方应用名
     * @return 查询结果
     */
    List<CCODThreePartAppPo> select(@Param("ccodVersion")String ccodVersion, @Param("tag")String tag, @Param("appName")String appName);

    /**
     * 删除特定的平台第三方依赖应用记录
     * @param ccodVersion ccod大版本号
     * @param tag 指定大版本号的特定标签
     * @param appName 第三方应用名
     * @param alias 第三方应用在平台的唯一别名
     */
    void delete(@Param("ccodVersion")String ccodVersion, @Param("tag")String tag, @Param("appName")String appName, @Param("alias")String alias);
}
