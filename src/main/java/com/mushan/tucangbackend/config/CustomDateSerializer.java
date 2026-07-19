package com.mushan.tucangbackend.config;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Date;

/**
 * 自定义日期序列化器，将日期格式化为 yyyy-MM-dd HH:mm:ss 格式
 */
public class CustomDateSerializer extends JsonSerializer<Date> {
    
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    @Override
    public void serialize(Date date, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (date != null) {
            gen.writeString(DateUtil.format(date, DATE_PATTERN));
        } else {
            gen.writeNull();
        }
    }
}