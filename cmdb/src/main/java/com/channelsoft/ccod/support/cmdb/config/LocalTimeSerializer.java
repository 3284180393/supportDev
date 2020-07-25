package com.channelsoft.ccod.support.cmdb.config;

import com.google.gson.*;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;

/**
 * @ClassName: LocalTimeSerializer
 * @Author: lanhb
 * @Description: Converter for org.joda.time.LocalTime
 * @Date: 2020/7/25 17:31
 * @Version: 1.0
 */
public class LocalTimeSerializer implements JsonDeserializer<LocalTime>, JsonSerializer<LocalTime> {

    private static final DateTimeFormatter TIME_FORMAT = ISODateTimeFormat.timeNoMillis();

    @Override
    public LocalTime deserialize(final JsonElement je, final Type type,
                                 final JsonDeserializationContext jdc) throws JsonParseException
    {
        final String dateAsString = je.getAsString();
        if (dateAsString.length() == 0)
        {
            return null;
        }
        else
        {
            return TIME_FORMAT.parseLocalTime(dateAsString);
        }
    }

    @Override
    public JsonElement serialize(final LocalTime src, final Type typeOfSrc,
                                 final JsonSerializationContext context)
    {
        String retVal;
        if (src == null)
        {
            retVal = "";
        }
        else
        {
            retVal = TIME_FORMAT.print(src);
        }
        return new JsonPrimitive(retVal);
    }
}
