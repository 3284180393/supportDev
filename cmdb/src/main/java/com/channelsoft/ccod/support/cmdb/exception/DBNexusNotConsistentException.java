package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: DBNexusNotConsistentException
 * @Author: lanhb
 * @Description: cmdb数据库的记录和nexus存储情况不一致
 * @Date: 2019/12/18 15:09
 * @Version: 1.0
 */
public class DBNexusNotConsistentException extends Exception{

    private static final long serialVersionUID = -1L;

    public DBNexusNotConsistentException(String msg)
    {
        super(msg);
    }
}
