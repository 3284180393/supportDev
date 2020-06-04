package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppBkModulePo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;
import com.channelsoft.ccod.support.cmdb.vo.*;

import java.util.List;

/**
 * @ClassName: ILJPaasService
 * @Author: lanhb
 * @Description: 用来定义蓝鲸paas的服务接口
 * @Date: 2019/12/2 14:26
 * @Version: 1.0
 */
public interface ILJPaasService {
    /**
     * 根据指定的biz id查询蓝鲸biz信息
     * @param bkBizId 需要查询的biz的id
     * @return 查询结果
     * @throws Exception
     */
    LJBizInfo queryBizInfoById(int bkBizId) throws InterfaceCallException, LJPaasException;

    /**
     * 查询蓝鲸所有的biz信息
     * @return 查询结果
     * @throws Exception
     */
    List<LJBizInfo> queryAllBiz() throws InterfaceCallException, LJPaasException;

    /**
     * 将一个已经存在的biz重置,并给它创建指定的set
     * @param bkBizId 需要重置的set名
     * @param setNames 需要创建的set名
     * @throws InterfaceCallException 调用蓝鲸api接口失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果失败
     */
    List<LJSetInfo> resetExistBiz(int bkBizId, List<String> setNames) throws InterfaceCallException, LJPaasException;

    /**
     * 在蓝鲸paas创建一个新的set
     * @param bkBizId set所属的biz id
     * @param bkSetName 需要创建的set名字
     * @param desc 该set的描述
     * @param capacity 描述
     * @return 创建的set信息
     * @throws ParamException 如果biz不存在,或是需要创建的setName已经存在将会抛出该异常
     * @throws LJPaasException 蓝鲸paas返回的失败信息
     */
    LJSetInfo createNewBizSet(int bkBizId, String bkSetName, String desc, int capacity) throws ParamException, LJPaasException;

    /**
     * 删除已有的set
     * @param bkBizId 需要删除的set 归属的biz的id
     * @param bkSetId 需要删除的set id
     * @throws ParamException 如果bkBizId不存在或是bkSetId不存在将会抛出此异常
     * @throws LJPaasException 蓝鲸返回失败信息
     */
    void deleteBizSet(int bkBizId, int bkSetId) throws ParamException, LJPaasException;

    /**
     * 将通过客户端收集的平台应用部署详情同步到蓝鲸paas
     * @param bkBizId 平台对应的蓝鲸paas biz id
     * @param platformId 平台id
     * @param hostCloudId 该biz的服务器所处的cloud id
     * @throws ParamException 接口函数的参数有误
     * @throws NotSupportAppException 客户端收集的应用中有蓝鲸paas无法处理的
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    void syncClientCollectResultToPaas(int bkBizId, String platformId, int hostCloudId) throws ParamException, NotSupportAppException, InterfaceCallException, LJPaasException;

    /**
     * 将指定的主机迁移到平台的空闲池
     * @param bkBizId 指定平台的biz id
     * @param hostList 需要迁移的主机列表
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    void transferHostToIdlePool(int bkBizId, Integer[] hostList) throws InterfaceCallException, LJPaasException;

    /**
     * 将指定平台的一组modules迁移到一组指定的主机上
     * @param bkBizId 平台的biz id
     * @param moduleIdList 需要迁移的模块
     * @param hostIdList 迁移的目标主机
     * @param isIncrement 追加还是覆盖,true追加，false覆盖
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    void transferModulesToHost(int bkBizId, Integer[] hostIdList, Integer[] moduleIdList, boolean isIncrement) throws InterfaceCallException, LJPaasException;

    /**
     * 查询指定条件下的所有host resource
     * @param bkBizId 蓝鲸paas的biz id
     * @param bkSetId 蓝鲸paas的set id
     * @param bkSetName 蓝鲸paas的set名
     * @param bkModuleName 蓝鲸paas上定义的模块名
     * @param bkHostInnerIp 主机的内部ip
     * @return 满足条件的host resource
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    List<LJHostResourceInfo> queryBKHostResource(Integer bkBizId, Integer bkSetId, String bkSetName, String bkModuleName, String bkHostInnerIp) throws InterfaceCallException, LJPaasException;

    /**
     * 查询指定set下的所有主机
     * @param bkBizId 蓝鲸paas的biz id
     * @param bkSetId 蓝鲸paas的set id
     * @param bkSetName 蓝鲸paas的set名
     * @param bkModuleName 蓝鲸paas上定义的模块名
     * @param bkHostInnerIp 主机的内部ip
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    List<LJHostInfo> queryBKHost(Integer bkBizId, Integer bkSetId, String bkSetName, String bkModuleName, String bkHostInnerIp) throws InterfaceCallException, LJPaasException;

    /**
     * 将一组新的主机添加到idle pool去
     * @param bkBizId 需要添加新主机的biz的id
     * @param bkIdlePoolSetId 空闲资源池set的id
     * @param newHostIps 被添加的主机ip
     * @param bkCloudId 该服务器所处的云id
     * @return 添加后空闲池所有空闲服务器
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    List<LJHostInfo> addNewHostToIdlePool(int bkBizId, int bkIdlePoolSetId, List<String> newHostIps, int bkCloudId) throws InterfaceCallException, LJPaasException;

    /**
     * 在蓝鲸paas查询指定条件的module信息
     * @param bkBizId 需要查询的biz id,必填
     * @param bkSetId 需要查询的set id,必填
     * @param moduleId 需要查询的module id,可以为空，为空则忽略此条件
     * @param moduleName 需要查询的module name,可以为空,为空则忽略此条件
     * @return 查询结果
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    List<LJModuleInfo> queryBkModule(int bkBizId, int bkSetId, Integer moduleId, String moduleName) throws InterfaceCallException, LJPaasException;

    /**
     * 查询指定ip上部署了哪些应用模块
     * @param bkBizId biz的id
     * @param bkSetId set的id
     * @param hostIp 主机ip
     * @return 主机上部署了哪些模块
     * @throws ParamException 指定host的ip不存在
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    List<LJModuleInfo> queryBkModuleAtHost(int bkBizId, int bkSetId, String hostIp) throws ParamException, InterfaceCallException, LJPaasException;
    /**
     * 向平台的指定set添加一个新的module
     * @param bkBizId 平台的biz id
     * @param bkSetId  指定的set的id
     * @param moduleName 需要添加的module名
     * @return 添加后的模块信息
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    LJModuleInfo addNewBkModule(int bkBizId, int bkSetId, String moduleName) throws InterfaceCallException, LJPaasException;

    /**
     * 从指定平台删除一个已经存在的set
     * @param bkBizId 指定平台的biz id
     * @param bkSetId 需要删除的set id
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    void deleteExistBizSet(int bkBizId, int bkSetId) throws InterfaceCallException, LJPaasException;

    /**
     * 向蓝鲸paas指定biz添加一个新的set
     * @param bkBizId set所属的biz id
     * @param bkSetName 需要创建的set名字
     * @param desc 该set的描述
     * @param capacity 描述
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    LJSetInfo addNewBizSet(int bkBizId, String bkSetName, String desc, int capacity) throws InterfaceCallException, LJPaasException;

    /**
     * 将一组已经部署好的应用绑定到paas平台的指定set下
     * @param bkBizId 指定的平台biz id
     * @param deployAppList 需要绑定的应用列表
     * @throws InterfaceCallException 调用蓝鲸api异常
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api结果异常
     */
    void bindDeployAppsToBizSet(int bkBizId, int bkSetId, String bkSetName, List<PlatformAppPo> deployAppList) throws InterfaceCallException, LJPaasException;

    /**
     * 将一组已经部署好的应用从paas平台的指定set下解绑
     * @param bkBizId 指定的平台biz id
     * @param bkSetId 需要解绑应用的set id
     * @param deployAppList 需要解绑的应用列表
     * @throws ParamException
     * @throws InterfaceCallException 调用蓝鲸api异常
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api结果异常
     */
    void disBindDeployAppsToBizSet(int bkBizId, int bkSetId, List<PlatformAppBkModulePo> deployAppList) throws ParamException, InterfaceCallException, LJPaasException;

    /**
     * 查询指定biz的所有set
     * @param bkBizId 指定的biz的id
     * @return 该biz的所有set
     */
    List<LJSetInfo> queryBkBizSet(int bkBizId) throws InterfaceCallException, LJPaasException;

    /**
     * 查询指定id的biz的空闲服务器
     * @param bkBizId 指定的biz的id
     * @return 该biz的空闲服务器
     * @throws InterfaceCallException 调用蓝鲸api异常
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api结果异常
     */
    List<LJHostInfo> queryBizIdleHost(int bkBizId) throws InterfaceCallException, LJPaasException;

    /**
     * 创建新的biz
     * @param bkBizName 被创建的biz名
     * @param setNames 该biz下的set名称
     * @return 该biz的id
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    int createNewBiz(String bkBizName, List<String> setNames) throws ParamException, InterfaceCallException, LJPaasException;

    /**
     * 查询可能的ccod biz
     * @return 可能的新ccod biz
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    List<LJBizInfo> queryNewCCODBiz() throws InterfaceCallException, LJPaasException;

}
