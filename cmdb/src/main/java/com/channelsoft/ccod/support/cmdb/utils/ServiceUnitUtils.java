package com.channelsoft.ccod.support.cmdb.utils;

import com.channelsoft.ccod.support.cmdb.constant.DatabaseType;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ClassName: ServiceUnitUtils
 * @Author: lanhb
 * @Description: 提供服务单元管理相关的工具类，例如获取特定服务单元的insert和update sql
 * @Date: 2020/3/11 11:35
 * @Version: 1.0
 */
public class ServiceUnitUtils {

    private final static Logger logger = LoggerFactory.getLogger(ServiceUnitUtils.class);

    public static String getCMSServerInsertSql(DatabaseType dbType, String databaseName, String serviceUnitTableName,
                                              String nextSeq, String domainId, String alias, int heartBreathTime,
                                              int serviceMode, String areaId, String operator,
                                               String dcmsId) throws ParamException
    {
        switch (dbType){
            case MYSQL:
            case ORACLE:
                break;
            default:
                logger.error(String.format("not support gls server with dbType=%s", dbType.name));
                throw new ParamException(String.format("not support gls server with dbType=%s", dbType.name));
        }
        int nodePort = 17119;
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String insertSql = String.format("INSERT INTO \"%s\".\"%s\" VALUES ('%s', '%s-%s', '6', '%d', null, '%s', '%d', 'CMS4GLS:tcp -h %s-%s -p %d', '%s', null, null, null, null, null, null, null, null, '%s', null, null, null, null, null, '%s', null, null);",
                databaseName, serviceUnitTableName, nextSeq, alias, domainId, heartBreathTime, sf.format(now),
                serviceMode, alias, domainId, nodePort, areaId, operator, dcmsId);
        return insertSql;
    }

    public static String getDefaultCMSServerInsertSql(DatabaseType dbType, String areaId,
                                              String domainId, String alias) throws ParamException
    {
        String databaseName = "CCOD";
        String serviceUnitTableName = "GLS_SERVICE_UNIT";
        String nextSeq = "TEST_SEQ.nextval";
        int heartBreathTime = 10;
        int serviceMode = 1;
        String operator = "system";
        String dcmsId = "DCMS-SH-002";
        String sql = getCMSServerInsertSql(dbType, databaseName, serviceUnitTableName, nextSeq, domainId, alias,
                heartBreathTime, serviceMode,  areaId, operator, dcmsId);
        return sql;
    }

    public static String getUCDSServerInsertSql(DatabaseType dbType, String databaseName, String serviceUnitTableName,
                                               String nextSeq, String domainId, String alias, int heartBreathTime,
                                               int serviceMode, String areaId, String dbName, String dbUser,
                                                String dbPwd, String dbIp, String ucdsIp, int ucdsPort,
                                                String operator, String ucdsDataKeeperIp, String ucdsInnerIp) throws ParamException
    {
        switch (dbType){
            case MYSQL:
            case ORACLE:
                break;
            default:
                logger.error(String.format("not support gls server with dbType=%s", dbType.name));
                throw new ParamException(String.format("not support gls server with dbType=%s", dbType.name));
        }
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String insertSql = String.format("INSERT INTO \"%s\".\"%s\" VALUES ('%s', '%s-%s', '2', '%d', null, '%s', '%d', 'ucxserver:tcp -h %s-%s -p 11000', '%s', '%s', '%s', '%s', '%s', null, null, '%s', '%d', '%s', null, 'UMTFactory:tcp -h %s-%s -p 17004', 'UCDSDataKeeper:tcp -h %s -p 17006', 'HBService:tcp -h %s-%s -p 17007', null, null, null, '%s');",
                databaseName, serviceUnitTableName, nextSeq, alias, domainId, heartBreathTime, sf.format(now), serviceMode,
                alias.replaceAll("ucds", "ucx"), domainId, areaId, dbName, dbUser,
                dbPwd, dbIp, ucdsIp, ucdsPort, operator, alias, domainId, ucdsDataKeeperIp, alias, domainId,
                ucdsInnerIp);
        return insertSql;
    }

    public static String getDefaultUCDSServerInsertSql(DatabaseType dbType, String areaId,
                                                       String domainId, String alias, String dbName, String dbUser,
                                                       String dbPwd, String ucdsIp, int ucdsPort,
                                                       String ucdsDataKeeperIp, String ucdsInnerIp) throws ParamException
    {
        String databaseName = "CCOD";
        String serviceUnitTableName = "GLS_SERVICE_UNIT";
        String nextSeq = "TEST_SEQ.nextval";
        int heartBreathTime = 15;
        int serviceMode = 1;
        String operator = "system";
        String dbIp = "mysql";
        String sql = getUCDSServerInsertSql(dbType, databaseName, serviceUnitTableName, nextSeq, domainId, alias,
                heartBreathTime, serviceMode, areaId, dbName, dbUser, dbPwd, dbIp, ucdsIp, ucdsPort, operator, ucdsDataKeeperIp,
                ucdsInnerIp);
        return sql;
    }

    public static String getUCDSServerUpdateSql(DatabaseType dbType, String databaseName, String serviceUnitTableName,
                                                String domainId, String alias, int ucdsPort) throws ParamException
    {
        switch (dbType){
            case MYSQL:
            case ORACLE:
                break;
            default:
                logger.error(String.format("not support gls server with dbType=%s", dbType.name));
                throw new ParamException(String.format("not support gls server with dbType=%s", dbType.name));
        }
        String updateSql = String.format("update \"%s\".\"%s\" set PARAM_UCDS_PORT='%d' where NAME='%s-%s';",
                databaseName, serviceUnitTableName, ucdsPort, alias, domainId);
        return updateSql;
    }

    public static String getDefaultUCDSServerUpdateSql(DatabaseType dbType, String domainId, String alias, int ucdsPort) throws ParamException
    {
        String databaseName = "CCOD";
        String serviceUnitTableName = "GLS_SERVICE_UNIT";
        switch (dbType){
            case MYSQL:
            case ORACLE:
                break;
            default:
                logger.error(String.format("not support gls server with dbType=%s", dbType.name));
                throw new ParamException(String.format("not support gls server with dbType=%s", dbType.name));
        }
        String updateSql = getUCDSServerUpdateSql(dbType, databaseName, serviceUnitTableName, domainId, alias, ucdsPort);
        return updateSql;
    }

    public static String getDCSServerInsertSql(DatabaseType dbType, String databaseName, String serviceUnitTableName,
                                               String nextSeq, String domainId, String alias, int heartBreathTime,
                                               int serviceMode, String areaId, String operator) throws ParamException
    {
        switch (dbType){
            case MYSQL:
            case ORACLE:
                break;
            default:
                logger.error(String.format("not support gls server with dbType=%s", dbType.name));
                throw new ParamException(String.format("not support gls server with dbType=%s", dbType.name));
        }
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String insertSql = String.format("INSERT INTO \"%s\".\"%s\" VALUES ('%s', '%s-%s', '9', '%d', null, '%s', '%d', 'EMCServer:default -h %s-%s -p 12009', '%s', null, null, null, null, null, null, null, null, '%s', null, null, null, null, null, null, 'DCInterfaceI:default -h %s-%s -p 18070', null);",
                databaseName, serviceUnitTableName, nextSeq, alias, domainId, heartBreathTime, sf.format(now),
                serviceMode, alias.replaceAll("dcs", "dcproxy"), domainId, areaId, operator, alias, domainId);
        return insertSql;
    }

    public static String getDefaultDCSServerInsertSql(DatabaseType dbType, String areaId,
                                                      String domainId, String alias) throws ParamException
    {
        String databaseName = "CCOD";
        String serviceUnitTableName = "GLS_SERVICE_UNIT";
        String nextSeq = "TEST_SEQ.nextval";
        int heartBreathTime = 10;
        int serviceMode = 1;
        String operator = "system";
        String sql = getDCSServerInsertSql(dbType, databaseName, serviceUnitTableName, nextSeq, domainId, alias,
                heartBreathTime, serviceMode,  areaId, operator);
        return sql;
    }

    public static String getDDSServerInsertSql(DatabaseType dbType, String databaseName, String serviceUnitTableName,
                                               String nextSeq, String domainId, String alias, int heartBreathTime,
                                               int serviceMode, String areaId, String operator) throws ParamException
    {
        switch (dbType){
            case MYSQL:
            case ORACLE:
                break;
            default:
                logger.error(String.format("not support gls server with dbType=%s", dbType.name));
                throw new ParamException(String.format("not support gls server with dbType=%s", dbType.name));
        }
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String insertSql = String.format("INSERT INTO \"%s\".\"%s\" VALUES ('%s', '%s-%s', '4', '%d', null, '%s', '%d', 'DDSServer:tcp -h %s-%s -p 17088', '%s', null, null, null, null, null, null, null, null, '%s', null, null, null, null, null, null, null, null);",
                databaseName, serviceUnitTableName, nextSeq, alias, domainId, heartBreathTime, sf.format(now),
                serviceMode, alias, domainId, areaId, operator);
        return insertSql;
    }

    public static String getDefaultDDSServerInsertSql(DatabaseType dbType, String areaId,
                                                      String domainId, String alias) throws ParamException
    {
        String databaseName = "CCOD";
        String serviceUnitTableName = "GLS_SERVICE_UNIT";
        String nextSeq = "TEST_SEQ.nextval";
        int heartBreathTime = 10;
        int serviceMode = 1;
        String operator = "system";
        String sql = getDDSServerInsertSql(dbType, databaseName, serviceUnitTableName, nextSeq, domainId, alias,
                heartBreathTime, serviceMode,  areaId, operator);
        return sql;
    }

    public static String getDAEInsertSql(DatabaseType dbType, String databaseName, String serviceUnitTableName,
                                               String nextSeq, String domainId, String alias, int heartBreathTime,
                                               int serviceMode, String areaId, String operator) throws ParamException
    {
        switch (dbType){
            case MYSQL:
            case ORACLE:
                break;
            default:
                logger.error(String.format("not support gls server with dbType=%s", dbType.name));
                throw new ParamException(String.format("not support gls server with dbType=%s", dbType.name));
        }
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String insertSql = String.format("INSERT INTO \"%s\".\"%s\" VALUES ('%s', '%s-%s', '12', '%d', null, '%s', '%d', 'DAEInterface:tcp -h %s-%s -p 10101', '%s', null, null, null, null, null, null, null, null, '%s', null, null, null, null, null, null, null, null);",
                databaseName, serviceUnitTableName, nextSeq, alias, domainId, heartBreathTime, sf.format(now),
                serviceMode, alias, domainId, areaId, operator);
        return insertSql;
    }

    public static String getDefaultDAEInsertSql(DatabaseType dbType, String areaId,
                                                      String domainId, String alias) throws ParamException
    {
        String databaseName = "CCOD";
        String serviceUnitTableName = "GLS_SERVICE_UNIT";
        String nextSeq = "TEST_SEQ.nextval";
        int heartBreathTime = 10;
        int serviceMode = 1;
        String operator = "system";
        String sql = getDAEInsertSql(dbType, databaseName, serviceUnitTableName, nextSeq, domainId, alias,
                heartBreathTime, serviceMode,  areaId, operator);
        return sql;
    }

    public static String getSSInsertSql(DatabaseType dbType, String databaseName, String serviceUnitTableName,
                                               String nextSeq, String domainId, String alias, int heartBreathTime,
                                               int serviceMode, String areaId, String operator) throws ParamException
    {
        switch (dbType){
            case MYSQL:
            case ORACLE:
                break;
            default:
                logger.error(String.format("not support gls server with dbType=%s", dbType.name));
                throw new ParamException(String.format("not support gls server with dbType=%s", dbType.name));
        }
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String insertSql = String.format("INSERT INTO \"%s\".\"%s\" VALUES ('%s', '%s-%s', '10', '%d', null, '%s', '%d', 'Schedule:tcp -h %s-%s -p 18888', '%s', null, null, null, null, null, null, null, null, 'system', null, null, null, null, null, null, null, null);",
                databaseName, serviceUnitTableName, nextSeq, alias, domainId, heartBreathTime, sf.format(now),
                serviceMode, alias, domainId, areaId, operator);
        return insertSql;
    }

    public static String getDefaultSSInsertSql(DatabaseType dbType, String areaId,
                                                      String domainId, String alias) throws ParamException
    {
        String databaseName = "CCOD";
        String serviceUnitTableName = "GLS_SERVICE_UNIT";
        String nextSeq = "TEST_SEQ.nextval";
        int heartBreathTime = 10;
        int serviceMode = 1;
        String operator = "system";
        String sql = getSSInsertSql(dbType, databaseName, serviceUnitTableName, nextSeq, domainId, alias,
                heartBreathTime, serviceMode,  areaId, operator);
        return sql;
    }
}
