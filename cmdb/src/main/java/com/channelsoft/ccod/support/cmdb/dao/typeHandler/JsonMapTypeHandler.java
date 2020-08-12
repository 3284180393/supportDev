package com.channelsoft.ccod.support.cmdb.dao.typeHandler;

import com.google.gson.Gson;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.Map;

/**
 * @ClassName: JsonMapTypeHandler
 * @Author: lanhb
 * @Description: 用来处理json和map之间的转换
 * @Date: 2020/8/12 15:45
 * @Version: 1.0
 */
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, Object>> {

    Gson gson = new Gson();

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) {
        try {
            String value = rs.getString(columnName);
            return gson.fromJson(value, Map.class);
        } catch (Exception e) {

        }
        return null;
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            String value = rs.getString(columnIndex);
            return gson.fromJson(value, Map.class);
        } catch (Exception e) {

        }
        return null;
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            String value = cs.getString(columnIndex);
            return gson.fromJson(value, Map.class);
        } catch (Exception e) {

        }
        return null;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, gson.toJson(parameter));
        }

    }
}
