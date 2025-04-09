package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.entity.vo.response.WeatherVO;
import com.example.service.WeatherService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@Service
public class WeatherServiceImpl implements WeatherService {

    @Resource
    RestTemplate restTemplate;
    @Resource
    StringRedisTemplate template;
    @Value("${spring.weather.key}")
    String key;

    @Override
    public WeatherVO fetchWeather(double longitude, double latitude) {
        return fetchFromCache(longitude, latitude);
    }

//    private WeatherVO fetchFromCache(double longitude, double latitude) {
//        // 从缓存获取天气信息，如果缓存中没有，则从API获取并存入缓存
//        // 和风天气官方文档说明，返回的数据进行了GZIP压缩
//        // 根据经纬度获取城市信息，并进行解压缩
//        JSONObject geo = this.decompressStringToJson(
//                restTemplate.getForObject("https://geoapi.qweather.com/v2/city/lookup?location=" + longitude + "," + latitude + "&key=" + key, byte[].class)
//        );
//        // 如果geo数据为空，返回null
//        if (geo == null) return null;
//        // 获取城市信息的第一个结果
//        JSONObject location = geo.getJSONArray("location").getJSONObject(0);
//        // 获取城市的id
//        int id = location.getInteger("id");
//        // 构建缓存的key
//        String key = Const.FORUM_WEATHER_CACHE + id;
//        // 尝试从缓存中获取天气数据
//        String cache = template.opsForValue().get(key);
//        // 如果缓存中存在数据，直接解析并返回
//        if (cache != null) {
//            return JSONObject.parseObject(cache).to(WeatherVO.class);
//        }
//        // 如果缓存中没有数据，则从API获取天气信息
//        WeatherVO vo = this.fetchFromAPI(id, location);
//        // 如果API获取失败，返回null
//        if (vo == null) return null;
//        // 将获取到的天气信息存入缓存，设置缓存时间为1小时
//        template.opsForValue().set(key, JSONObject.from(vo).toJSONString(), 1, TimeUnit.HOURS);
//        // 返回获取到的天气信息
//        return vo;
//    }
private WeatherVO fetchFromCache(double longitude, double latitude) {
    // 从缓存获取天气信息，如果缓存中没有，则从API获取并存入缓存
    // 和风天气官方文档说明，返回的数据进行了GZIP压缩
    // 根据经纬度获取城市信息，并进行解压缩
    JSONObject geo = this.decompressStringToJson(
            restTemplate.getForObject("https://geoapi.qweather.com/v2/city/lookup?location=" + longitude + "," + latitude + "&key=" + key, byte[].class)
    );
    // 如果geo数据为空，返回null
    if (geo == null) return null;

    JSONArray locationArray = geo.getJSONArray("location");
    if (locationArray == null || locationArray.isEmpty()) return null;

    // 获取城市信息的第一个结果
    JSONObject location = locationArray.getJSONObject(0);
    // 获取城市的id
    int id = location.getInteger("id");
    // 构建缓存的key
    String key = Const.FORUM_WEATHER_CACHE + id;
    // 尝试从缓存中获取天气数据
    String cache = template.opsForValue().get(key);
    // 如果缓存中存在数据，直接解析并返回
    if (cache != null) {
        return JSONObject.parseObject(cache).to(WeatherVO.class);
    }
    // 如果缓存中没有数据，则从API获取天气信息
    WeatherVO vo = this.fetchFromAPI(id, location);
    // 如果API获取失败，返回null
    if (vo == null) return null;
    // 将获取到的天气信息存入缓存，设置缓存时间为1小时
    template.opsForValue().set(key, JSONObject.from(vo).toJSONString(), 1, TimeUnit.HOURS);
    // 返回获取到的天气信息
    return vo;
}

    private WeatherVO fetchFromAPI(int id, JSONObject location) {
        WeatherVO vo = new WeatherVO();
        vo.setLocation(location);
        JSONObject now = this.decompressStringToJson(restTemplate.getForObject("https://devapi.qweather.com/v7/weather/now?location=" + id + "&key=" + key, byte[].class));
        if (now == null) return null;
        vo.setNow(now.getJSONObject("now"));//获取现在的天气
        JSONObject hourly = this.decompressStringToJson(restTemplate.getForObject("https://devapi.qweather.com/v7/weather/24h?location=" + id + "&key=" + key, byte[].class));
        if (hourly == null) return null;
        vo.setHourly(new JSONArray(hourly.getJSONArray("hourly").stream().limit(5).toList()));//获取5个小时的天气
        return vo;

    }

    private JSONObject decompressStringToJson(byte[] data) {//在和风天气传回来的数据GZIP进行解压缩并转换为JSON
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(data));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzipInputStream.read(buffer)) != -1)
                byteArrayOutputStream.write(buffer, 0, read);
            gzipInputStream.close();
            byteArrayOutputStream.close();
            return JSONObject.parseObject(byteArrayOutputStream.toString());
        } catch (IOException e) {
            throw null;
        }
    }

}
