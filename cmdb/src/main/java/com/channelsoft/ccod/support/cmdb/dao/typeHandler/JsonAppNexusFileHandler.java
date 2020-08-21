package com.channelsoft.ccod.support.cmdb.dao.typeHandler;

import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.List;

/**
 * @ClassName: JsonAppNexusFileHandler
 * @Author: lanhb
 * @Description: 用来处理List<AppFileNexusInfo>和json之间的转换
 * @Date: 2020/8/21 14:02
 * @Version: 1.0
 */
public class JsonAppNexusFileHandler extends BaseTypeHandler<List<AppFileNexusInfo>> {

    Gson gson = new Gson();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<AppFileNexusInfo> appFileNexusInfos, JdbcType jdbcType) throws SQLException {
        if (appFileNexusInfos == null || appFileNexusInfos.size() == 0) {
            ps.setNull(i, Types.VARCHAR);
        } else {
            ps.setString(i, gson.toJson(appFileNexusInfos));
        }
    }

    @Override
    public List<AppFileNexusInfo> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        if(StringUtils.isBlank(value))
            value = "[]";
        return gson.fromJson(value, new TypeToken<List<AppFileNexusInfo>>() {}.getType());
    }

    @Override
    public List<AppFileNexusInfo> getNullableResult(ResultSet rs, int i) throws SQLException {
        String value = rs.getString(i);
        if(StringUtils.isBlank(value))
            value = "[]";
        return gson.fromJson(value, new TypeToken<List<AppFileNexusInfo>>() {}.getType());
    }

    @Override
    public List<AppFileNexusInfo> getNullableResult(CallableStatement cs, int i) throws SQLException {
        String value = cs.getString(i);
        if(StringUtils.isBlank(value))
            value = "[]";
        return gson.fromJson(value, new TypeToken<List<AppFileNexusInfo>>() {}.getType());
    }
}
