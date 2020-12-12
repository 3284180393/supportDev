package com.channelsoft.ccod.support.cmdb.config;

import com.channelsoft.ccod.support.cmdb.vo.PlatformUpdateSchemaInfo;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * @ClassName: SchemaTypeAdapter
 * @Author: lanhb
 * @Description: 用来顶定义schema输出排序
 * @Date: 2020/12/11 15:30
 * @Version: 1.0
 */
public class SchemaTypeAdapter extends TypeAdapter<PlatformUpdateSchemaInfo> {

    @Override
    public void write(JsonWriter out, PlatformUpdateSchemaInfo value) throws IOException {
        out.beginObject();
        out.name("platformName").value(value.getPlatformName());
        out.name("platformId").value(value.getPlatformId());
        out.name("ccodVersion").value(value.getCcodVersion());
        out.name("hostUrl").value(value.getHostUrl());
//        out.name("hosts").value(value.getHostJson());
//        out.name("depend").value(value.getDependJson());
//        out.value("nginx").value(value.getNginxJson());
//        out.value("domains").value(value.getDomainJson());
    }

    @Override
    public PlatformUpdateSchemaInfo read(JsonReader in) throws IOException {
        return null;
    }
}
