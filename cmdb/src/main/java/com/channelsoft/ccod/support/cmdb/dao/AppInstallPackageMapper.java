package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppInstallPackagePo;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: AppInstallPackageMapper
 * @Author: lanhb
 * @Description: 用来定义AppInstallPackagePo操作的dao接口
 * @Date: 2019/11/21 14:07
 * @Version: 1.0
 */
@Component
public interface AppInstallPackageMapper {
    /**
     * 向数据库添加一条新的应用安装包记录
     * @param installPackage 需要添加的配置文件信息
     * @throws DataAccessException
     */
    void insert(AppInstallPackagePo installPackage) throws DataAccessException;

    /**
     * 根据主键查询配置文件信息
     * @param packageId 应用安装包主键
     * @return 查询结果
     * @throws DataAccessException
     */
    AppInstallPackagePo selectByPrimaryKey(int packageId) throws DataAccessException;

    /**
     * 查询所有安装包记录
     * @return 查询结果
     * @throws DataAccessException
     */
    List<AppInstallPackagePo> select() throws DataAccessException;

    /**
     * 更新已有的应用安装包信息
     * @param installPackage 需要更新的安装包信息
     * @throws DataAccessException
     */
    void update(AppInstallPackagePo installPackage) throws DataAccessException;

    /**
     * 删除指定条件的应用配置文件记录,如果某个参数为空则忽略该参数
     * @param packageId 应用安装包id
     * @param appId 应用id
     */
    void delete(@Param("packageId")Integer packageId, @Param("appId")Integer appId);
}
