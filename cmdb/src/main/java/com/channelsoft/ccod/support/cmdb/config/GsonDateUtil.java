package com.channelsoft.ccod.support.cmdb.config;

import com.google.gson.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @ClassName: GsonDateUtil
 * @Author: lanhb
 * @Description: 用来处理gson中的Date处理
 * @Date: 2020/6/23 18:51
 * @Version: 1.0
 */
public class GsonDateUtil implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {

    private static final DateTimeFormatter TIME_FORMAT = ISODateTimeFormat.dateTime();

    @Override
    public DateTime deserialize(final JsonElement je, final Type type,
                                final JsonDeserializationContext jdc) throws JsonParseException
    {
        final String dateAsString = je.getAsString();
        if (dateAsString.length() == 0)
        {
            return null;
        }
        else
        {
            return TIME_FORMAT.parseDateTime(dateAsString);
        }
    }

    @Override
    public JsonElement serialize(final DateTime src, final Type typeOfSrc,
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
