package com.channelsoft.ccod.support.cmdb.vo;

import java.util.List;

/**
 * @ClassName: PlatformRollbackParamVo
 * @Author: lanhb
 * @Description: 平台回滚参数
 * @Date: 2020/7/28 16:26
 * @Version: 1.0
 */
public class PlatformRollbackParamVo {

    private List<String> domainIds;

    public List<String> getDomainIds() {
        return domainIds;
    }

    public void setDomainIds(List<String> domainIds) {
        this.domainIds = domainIds;
    }
}
