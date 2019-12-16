package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.LJPaasException;
import com.channelsoft.ccod.support.cmdb.exception.NotSupportAppException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.vo.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.vo.CCODPlatformInfo;
import com.channelsoft.ccod.support.cmdb.vo.LJBizInfo;
import com.channelsoft.ccod.support.cmdb.vo.LJSetInfo;

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
     * @param bizId
     * @return 查询结果
     * @throws Exception
     */
    LJBizInfo queryBizInfoById(int bizId) throws Exception;

    /**
     * 查询蓝鲸所有的biz信息
     * @return 查询结果
     * @throws Exception
     */
    LJBizInfo[] queryBizInfo() throws Exception;

    /**
     * 查询所有的ccod biz平台
     * @return 查询结果
     * @throws Exception
     */
    List<CCODPlatformInfo> queryAllCCODBiz() throws Exception;

    /**
     * 根据指定条件查询相关biz信息
     * @param bizId biz id
     * @param setId set id
     * @param domainId 域id
     * @return 查询结果
     * @throws Exception
     */
    List<CCODPlatformInfo> queryCCODBiz(Integer bizId, String setId, String domainId) throws Exception;

    /**
     * 查询ccod biz下面set信息
     * @return
     */
    List<BizSetDefine> queryCCODBizSet();

    /**
     * 查询指定set下面关联的应用
     * @param setId 指定的set的id
     * @return 查询结果
     * @throws ParamException 指定的setId不存在
     */
    List<String> queryAppsInSet(String setId) throws ParamException;

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
     * @param platformName 平台名
     * @param hostCloudId 该biz的服务器所处的cloud id
     * @throws ParamException 接口函数的参数有误
     * @throws NotSupportAppException 客户端收集的应用中有蓝鲸paas无法处理的
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    void syncClientCollectResultToPaas(int bkBizId, String platformName, int hostCloudId) throws ParamException, NotSupportAppException, InterfaceCallException, LJPaasException;
}
