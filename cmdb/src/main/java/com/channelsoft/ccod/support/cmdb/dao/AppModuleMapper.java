package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
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
public interface AppModuleMapper {

    /**
     * 查询指定条件的AppModule，如果某个参数为空,则忽略该参数
     * @param appName 应用名
     * @param appAlias 应用别名
     * @param version 版本号
     * @return 查询结果
     * @throws DataAccessException
     */
    List<AppModuleVo> select(@Param("platformId")String appName, @Param("appAlias")String appAlias, @Param("version")String version) throws DataAccessException;
}
