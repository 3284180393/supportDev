package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: AppModuleMapper
 * @Author: lanhb
 * @Description: 用来查询AppModuleVo的dao接口
 * @Date: 2019/11/23 13:31
 * @Version: 1.0
 */
@Component
@Mapper
public interface AppModuleMapper {

    /**
     * 查询指定条件的AppModule，如果某个参数为空,则忽略该参数
     * @param appType 应用类型
     * @param appName 应用名
     * @param appAlias 应用别名
     * @param version 版本号
     * @return 查询结果
     * @throws DataAccessException
     */
    List<AppModuleVo> select(@Param("appType")String appType, @Param("appName")String appName, @Param("appAlias")String appAlias, @Param("version")String version) throws DataAccessException;

    /**
     * 查询指定条件的AppModule
     * @param appName 应用名
     * @param version 版本号
     * @return 查询结果
     * @throws DataAccessException
     */
    AppModuleVo selectByNameAndVersion(@Param("appName")String appName, @Param("version")String version) throws DataAccessException;
}
