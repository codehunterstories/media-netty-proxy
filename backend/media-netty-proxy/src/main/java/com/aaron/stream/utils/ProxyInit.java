package com.aaron.stream.utils;

import com.aaron.stream.config.ProxyProperties;
import com.aaron.stream.consts.ProxyConst;

import java.util.Map;

public class ProxyInit {
    private static ProxyProperties PROXY_PROPERTIES = null;

    public static ProxyProperties getProxyProperties() {
        if (PROXY_PROPERTIES == null) {
            Map<String, String> sysMap = System.getenv();
            PROXY_PROPERTIES = new ProxyProperties();
            PROXY_PROPERTIES.setAppKey(sysMap.get(ProxyConst.APP_KEY));
            PROXY_PROPERTIES.setAppSecret(sysMap.get(ProxyConst.APP_SECRET));
            PROXY_PROPERTIES.setHikServicePrefix(sysMap.get(ProxyConst.HIK_SERVICE_PREFIX));
            PROXY_PROPERTIES.setUsername(sysMap.get(ProxyConst.USERNAME));
            PROXY_PROPERTIES.setPassword(sysMap.get(ProxyConst.PASSWORD));
            PROXY_PROPERTIES.setTest(Boolean.parseBoolean(sysMap.get(ProxyConst.TEST)));
        }

        return PROXY_PROPERTIES;
    }
}
