package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.vo.LJBKInfo;

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
    LJBKInfo queryBizInfoById(int bizId) throws Exception;

    /**
     * 查询蓝鲸所有的biz信息
     * @return 查询结果
     * @throws Exception
     */
    LJBKInfo[] queryBizInfo() throws Exception;
}
