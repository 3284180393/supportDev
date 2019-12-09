package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.vo.CCODPlatformInfo;
import com.channelsoft.ccod.support.cmdb.vo.LJBizInfo;

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
    List<CCODPlatformInfo> queryCCODBiz(Integer bizId, Integer setId, String domainId) throws Exception;
}
