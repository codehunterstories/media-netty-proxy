package com.aaron.stream.utils;

import com.aaron.stream.config.ProxyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HikRest {
    private final static String HIK_RTSP_URL = "/artemis/api/video/v2/cameras/previewURs";
    private final static String URL_PREFIX_REGEX = "(https?)://(\\d{1,3}(\\.\\d{1,3}){3}:\\d+)";
    private final static String HIK_PROTOCOL;
    private final static String HIK_HOST;

    private final static ProxyProperties PROXY_PROPERTIES;
    private final static ObjectMapper OBJECT_MAPPER;
    private final static ArtemisConfig ARTEMIS_CONFIG;

    static {
        PROXY_PROPERTIES = ProxyInit.getProxyProperties();
        OBJECT_MAPPER = new ObjectMapper();
        Pattern pattern = Pattern.compile(URL_PREFIX_REGEX);
        Matcher matcher = pattern.matcher(PROXY_PROPERTIES.getHikServicePrefix());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Hik prefix url illegal, example: http://127.0.0.1:9999");
        }
        HIK_PROTOCOL = matcher.group(1);
        HIK_HOST = matcher.group(2);
        ARTEMIS_CONFIG = new ArtemisConfig(HIK_HOST, PROXY_PROPERTIES.getAppKey(), PROXY_PROPERTIES.getAppSecret());
    }

    public static String getRtspUrl(String cameraIndexCode) throws Exception {
        Map<String, String> path = buildPath(HIK_RTSP_URL);
        String body = buildBody(cameraIndexCode);
        // Read response body
        String responseBody = ArtemisHttpUtil.doPostStringArtemis(ARTEMIS_CONFIG, path, body, null, null, "application/json");
        log.info("【HikVision API】Get cameras stream on rtsp, result: {}", responseBody);
        Map respMap = OBJECT_MAPPER.readValue(responseBody, Map.class);

        String code = respMap.get("code").toString();
        if (!"0".equals(code)) {
            log.error("can't found device: {},", cameraIndexCode);
            throw new IllegalArgumentException("can't found device");
        }
        Map<String, Object> dataMap = (Map<String, Object>) respMap.get("data");
        String streamUrl = dataMap.getOrDefault("url", "").toString();
        log.info("【HikVision API】Get cameras stream on rtsp, address: {}", streamUrl);
        return streamUrl;
    }

    private static Map<String, String> buildPath(String requestUrl) {
        return new HashMap<>(1) {
            {
                put(HIK_PROTOCOL, requestUrl);
            }
        };
    }

    private static String buildBody(String cameraIndexCode) throws JsonProcessingException {
        Map<String, String> body = new HashMap<>();
        body.put("cameraIndexCode", cameraIndexCode);
        body.put("protocol", "rtsp");
        return OBJECT_MAPPER.writeValueAsString(body);
    }
}
