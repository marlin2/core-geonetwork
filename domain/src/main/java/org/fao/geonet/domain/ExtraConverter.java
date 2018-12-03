package org.fao.geonet.domain;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import org.fao.geonet.Util;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply=true)
public class ExtraConverter implements AttributeConverter<Map, String> {

 @Override
 public String convertToDatabaseColumn(Map map) {
        String result = Util.mapToQueryStr(map);
        return result;
 }

 @Override
 public Map<String,String> convertToEntityAttribute(String extra) {
        Map<String,String> result = new HashMap<String,String>();

        if (extra != null) {
          List<NameValuePair> extras = URLEncodedUtils.parse(extra, StandardCharsets.UTF_8);

          for (NameValuePair nvPair : extras) {
             result.put(nvPair.getName(), nvPair.getValue());
          }
        }
        return result; 
 }
}

