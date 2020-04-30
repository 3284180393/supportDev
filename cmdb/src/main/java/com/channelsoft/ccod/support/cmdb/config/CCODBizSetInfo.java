package com.channelsoft.ccod.support.cmdb.config;

import java.util.List;

/**
 * @ClassName: CCODBizSetInfo
 * @Author: lanhb
 * @Description: 用来定义一个合法的ccod biz所必须有的以及可能有的set信息
 * @Date: 2019/12/18 15:39
 * @Version: 1.0
 */

public class CCODBizSetInfo {

    private List<BizSetDefine> set;

    public List<BizSetDefine> getSet() {
        return set;
    }

    public void setSet(List<BizSetDefine> set) {
        this.set = set;
    }
}
