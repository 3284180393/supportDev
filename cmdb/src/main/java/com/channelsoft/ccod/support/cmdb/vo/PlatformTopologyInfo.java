package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: PlatformTopologyInfo
 * @Author: lanhb
 * @Description: 用来定义平台拓扑信息
 * @Date: 2019/12/25 17:52
 * @Version: 1.0
 */
public class PlatformTopologyInfo {

    private String platformId; //平台id

    private String platformName; //平台名,同时对应蓝鲸paas的biz name

    private int bkBizId; //该平台在蓝鲸paas的biz id

    private int bkCloudId; //该平台下面所有host所在的cloud id

    private int status; //该平台的状态


}
