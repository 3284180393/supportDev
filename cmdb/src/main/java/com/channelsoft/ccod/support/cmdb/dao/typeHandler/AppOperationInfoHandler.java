package com.channelsoft.ccod.support.cmdb.dao.typeHandler;

import com.channelsoft.ccod.support.cmdb.vo.AppUpdateOperationInfo;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;

/**
 * @ClassName: AppOperationInfoHandler
 * @Author: lanhb
 * @Description: 实现AppOperationInfo类在数据库读取时类和json的自动转换
 * @Date: 2020/10/29 16:21
 * @Version: 1.0
 */
public class AppOperationInfoHandler extends BaseTypeHandler<AppUpdateOperationInfo> {

    Gson gson = new Gson();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AppUpdateOperationInfo detail, JdbcType jdbcType) throws SQLException {
        if(detail == null) {
            ps.setNull(i, Types.VARCHAR);
        }
        else {
            ps.setString(i, gson.toJson(detail));
        }
    }

    @Override
    public AppUpdateOperationInfo getNullableResult(ResultSet rs, String s) throws SQLException {
        String value = rs.getString(s);
        if(StringUtils.isBlank(value))
            return null;
        return gson.fromJson(value, AppUpdateOperationInfo.class);
    }

    @Override
    public AppUpdateOperationInfo getNullableResult(ResultSet rs, int i) throws SQLException {
        String value = rs.getString(i);
        if(StringUtils.isBlank(value))
            return null;
        return gson.fromJson(value, AppUpdateOperationInfo.class);
    }

    @Override
    public AppUpdateOperationInfo getNullableResult(CallableStatement cs, int i) throws SQLException {
        String value = cs.getString(i);
        if(StringUtils.isBlank(value))
            return null;
        return gson.fromJson(value, AppUpdateOperationInfo.class);
    }
    
}
