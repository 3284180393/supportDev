package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: CMDBCfgException
 * @Author: lanhb
 * @Description: 用来定义cmdb配置错误的异常
 * @Date: 2019/12/16 14:19
 * @Version: 1.0
 */
public class CMDBCfgException extends Exception {

    private static final long serialVersionUID = -1L;

    public CMDBCfgException(String msg)
    {
        super(msg);
    }
}
