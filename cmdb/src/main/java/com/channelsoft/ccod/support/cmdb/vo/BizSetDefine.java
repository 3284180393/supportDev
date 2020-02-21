package com.channelsoft.ccod.support.cmdb.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: BizSetDefine
 * @Author: lanhb
 * @Description: 用来定义蓝鲸set对应ccod域以及应用的关系
 * @Date: 2019/12/10 10:03
 * @Version: 1.0
 */
public class BizSetDefine {

    private String name; //蓝鲸paas中的set名

    private String id; //set的id,方便查询

    private int isBasic; //是否是basic set,如果为1则任何不包含该set的biz都不是ccod biz

    private String fixedDomainName; //如果非空,将用它作为set下面域的固定名字

    private String fixedDomainId; //如果非空,将用它作为set下面域的固定id

    private int domainType; //该set下域域类型

    private String[] apps; //该set关联的apps

    private Map<String, String> appAliasMap; //set下的app别名

    private Map<String, Integer> appAddDelayMap; //执行完ADD操作后需要延迟多少时间执行下一条ADD操作

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIsBasic() {
        return isBasic;
    }

    public void setIsBasic(int isBasic) {
        this.isBasic = isBasic;
    }

    public String getFixedDomainName() {
        return fixedDomainName;
    }

    public void setFixedDomainName(String fixedDomainName) {
        this.fixedDomainName = fixedDomainName;
    }

    public String getFixedDomainId() {
        return fixedDomainId;
    }

    public void setFixedDomainId(String fixedDomainId) {
        this.fixedDomainId = fixedDomainId;
    }

    public String[] getApps() {
        return apps;
    }

    public void setApps(String[] apps) {
        this.apps = apps;
    }

    public Map<String, String> getAppAliasMap() {
        return appAliasMap;
    }

    public void setAppAliasMap(Map<String, String> appAliasMap) {
        this.appAliasMap = appAliasMap;
    }

    public int getDomainType() {
        return domainType;
    }

    public void setDomainType(int domainType) {
        this.domainType = domainType;
    }

    public Map<String, Integer> getAppAddDelayMap() {
        return appAddDelayMap;
    }

    public void setAppAddDelayMap(Map<String, Integer> appAddDelayMap) {
        this.appAddDelayMap = appAddDelayMap;
    }

    @Override
    public BizSetDefine clone()
    {
        BizSetDefine setDefine = new BizSetDefine();
        setDefine.name = this.name;
        setDefine.id = this.id;
        setDefine.isBasic = this.isBasic;
        setDefine.fixedDomainName = this.fixedDomainName;
        setDefine.fixedDomainId = this.fixedDomainId;
        setDefine.apps = new String[this.apps.length];
        for(int i = 0; i < this.apps.length; i++)
        {
            setDefine.apps[i] = this.apps[i];
        }
        Map<String, String> aliasMap = new HashMap<>();
        Map<String, Integer> addDelayMap = new HashMap<>();
        for(String key : this.appAliasMap.keySet())
        {
            aliasMap.put(key, this.appAliasMap.get(key));
            addDelayMap.put(key, this.appAddDelayMap.get(key));
        }
        setDefine.setAppAliasMap(aliasMap);
        setDefine.setAppAddDelayMap(appAddDelayMap);
        setDefine.domainType = this.domainType;
        return setDefine;
    }
}
