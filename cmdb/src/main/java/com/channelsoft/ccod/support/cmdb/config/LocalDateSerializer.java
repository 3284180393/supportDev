package com.channelsoft.ccod.support.cmdb.config;

import com.google.gson.*;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;

import org.joda.time.LocalDate;

/**
 * @ClassName: LocalDateSerializer
 * @Author: lanhb
 * @Description: 用来解析LocalDate
 * @Date: 2020/7/25 17:26
 * @Version: 1.0
 */
public class LocalDateSerializer implements JsonDeserializer<LocalDate>, JsonSerializer<LocalDate>{

    private static final DateTimeFormatter DATE_FORMAT = ISODateTimeFormat.date();

    @Override
    public LocalDate deserialize(final JsonElement je, final Type type,
                                 final JsonDeserializationContext jdc) throws JsonParseException
    {
        final String dateAsString = je.getAsString();
        if (dateAsString.length() == 0)
        {
            return null;
        }
        else
        {
            return DATE_FORMAT.parseLocalDate(dateAsString);
        }
    }

    @Override
    public JsonElement serialize(final LocalDate src, final Type typeOfSrc,
                                 final JsonSerializationContext context)
    {
        String retVal;
        if (src == null)
        {
            retVal = "";
        }
        else
        {
            retVal = DATE_FORMAT.print(src);
        }
        return new JsonPrimitive(retVal);
    }
}