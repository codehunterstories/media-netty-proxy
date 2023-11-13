package com.aaron.stream.config;

import lombok.Data;

@Data
public class ProxyProperties {
    private String appKey;
    private String appSecret;
    private String hikServicePrefix;
    private String username;
    private String password;
    private boolean test;
}
