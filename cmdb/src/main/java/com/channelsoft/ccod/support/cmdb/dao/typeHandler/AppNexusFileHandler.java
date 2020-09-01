package com.channelsoft.ccod.support.cmdb.dao.typeHandler;

import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;

/**
 * @ClassName: AppNexusFileHandler
 * @Author: lanhb
 * @Description: 用来定义AppFileNexusInfo对象和json之间转换
 * @Date: 2020/9/1 16:12
 * @Version: 1.0
 */
public class AppNexusFileHandler extends BaseTypeHandler<AppFileNexusInfo> {

    Gson gson = new Gson();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AppFileNexusInfo fileInfo, JdbcType jdbcType) throws SQLException {
        if(fileInfo == null) {
            ps.setNull(i, Types.VARCHAR);
        }
        else {
            ps.setString(i, gson.toJson(fileInfo));
        }
    }

    @Override
    public AppFileNexusInfo getNullableResult(ResultSet rs, String s) throws SQLException {
        String value = rs.getString(s);
        if(StringUtils.isBlank(value))
            return null;
        return gson.fromJson(value, AppFileNexusInfo.class);
    }

    @Override
    public AppFileNexusInfo getNullableResult(ResultSet rs, int i) throws SQLException {
        String value = rs.getString(i);
        if(StringUtils.isBlank(value))
            return null;
        return gson.fromJson(value, AppFileNexusInfo.class);
    }

    @Override
    public AppFileNexusInfo getNullableResult(CallableStatement cs, int i) throws SQLException {
        String value = cs.getString(i);
        if(StringUtils.isBlank(value))
            return null;
        return gson.fromJson(value, AppFileNexusInfo.class);
    }
}
