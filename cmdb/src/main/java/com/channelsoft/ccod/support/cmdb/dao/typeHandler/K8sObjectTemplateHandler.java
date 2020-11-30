package com.channelsoft.ccod.support.cmdb.dao.typeHandler;

import com.channelsoft.ccod.support.cmdb.po.K8sObjectTemplatePo;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;

/**
 * @ClassName: K8sObjectTemplateHandler
 * @Author: lanhb
 * @Description: 用来实现mybatis的K8sObjectTemplatePo和字符串之间的转换
 * @Date: 2020/11/30 19:45
 * @Version: 1.0
 */
public class K8sObjectTemplateHandler extends BaseTypeHandler<K8sObjectTemplatePo> {

    Gson gson = new Gson();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, K8sObjectTemplatePo template, JdbcType jdbcType) throws SQLException {
        if(template == null) {
            ps.setNull(i, Types.VARCHAR);
        }
        else {
            ps.setString(i, gson.toJson(template));
        }
    }

    @Override
    public K8sObjectTemplatePo getNullableResult(ResultSet rs, String s) throws SQLException {
        String value = rs.getString(s);
        if(StringUtils.isBlank(value))
            return null;
        return gson.fromJson(value, K8sObjectTemplatePo.class);
    }

    @Override
    public K8sObjectTemplatePo getNullableResult(ResultSet rs, int i) throws SQLException {
        String value = rs.getString(i);
        if(StringUtils.isBlank(value))
            return null;
        return gson.fromJson(value, K8sObjectTemplatePo.class);
    }

    @Override
    public K8sObjectTemplatePo getNullableResult(CallableStatement cs, int i) throws SQLException {
        String value = cs.getString(i);
        if(StringUtils.isBlank(value))
            return null;
        return gson.fromJson(value, K8sObjectTemplatePo.class);
    }

}
