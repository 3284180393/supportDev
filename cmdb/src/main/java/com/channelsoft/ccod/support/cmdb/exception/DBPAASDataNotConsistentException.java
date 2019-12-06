package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: DBPAASDataNotConsistentException
 * @Author: lanhb
 * @Description: 用来定义蓝鲸paas数据和本地数据库存储数据不一致的异常
 * @Date: 2019/12/6 9:26
 * @Version: 1.0
 */
public class DBPAASDataNotConsistentException extends Exception {
    private static final long serialVersionUID = -1L;

    public DBPAASDataNotConsistentException(String msg)
    {
        super(msg);
    }
}
