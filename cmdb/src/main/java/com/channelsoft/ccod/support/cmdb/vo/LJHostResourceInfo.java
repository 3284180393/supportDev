package com.channelsoft.ccod.support.cmdb.vo;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * @ClassName: LJHostResourceInfo
 * @Author: lanhb
 * @Description: 用来定义蓝鲸paas服务器上的的资源信息
 * @Date: 2019/12/3 19:50
 * @Version: 1.0
 */
public class LJHostResourceInfo {

    LJBKHostInfo host;

    LJSetInfo[] set;

    LJBKInfo[] biz;

    LJModuleInfo[] module;

    @JSONField(name = "host")
    public LJBKHostInfo getHost() {
        return host;
    }

    public void setHost(LJBKHostInfo host) {
        this.host = host;
    }

    @JSONField(name = "set")
    public LJSetInfo[] getSet() {
        return set;
    }

    public void setSet(LJSetInfo[] set) {
        this.set = set;
    }

    @JSONField(name = "biz")
    public LJBKInfo[] getBiz() {
        return biz;
    }

    public void setBiz(LJBKInfo[] biz) {
        this.biz = biz;
    }

    @JSONField(name = "module")
    public LJModuleInfo[] getModule() {
        return module;
    }

    public void setModule(LJModuleInfo[] module) {
        this.module = module;
    }
}
