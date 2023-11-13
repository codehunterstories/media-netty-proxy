# Media stream proxy (netty)

- netty
- ffmpeg
- hikvision

流媒体代理服务，支持动态获取海康威视 RTSP 流，支持 RTSP/RTMP 流等

## 安装依赖

调用海康威视开放平台API时，涉及了签名算法库，公共仓库没有最新的，需要下载最新的依赖可访问：[OpenAPI 安全认证库 （JAVA） V1.1.9](https://open.hikvision.com/download/5c67f1e2f05948198c909700?type=20)

``` bash
mvn install:install-file \
-Dfile=artemis-http-client-1.1.9.jar \
-DgroupId=com.hikvision.ga \
-DartifactId=artemis-http-client \
-Dversion=1.1.9 \
-Dpackaging=jar \
-DgeneratePom=true
```

## Compile

Java version: 11

``` bash
# 编译
mvn clean package # -DskipTests
```

## Build

``` bash
docker build -t media-netty-proxy .
```

## Run

### Sys Environment

``` environment
PROXY_APP_KEY=123;PROXY_APP_SECRET=123;PROXY_HIK_SERVICE_PREFIX=http://127.0.0.1:8090;PROXY_USERNAME=media;PROXY_PASSWORD=media!12345;PROXY_TEST=false
```

1. `PROXY_APP_KEY=123` # Hikvision app key
2. `PROXY_APP_SECRET=123` # Hikvision app secret
3. `PROXY_HIK_SERVICE_PREFIX=http://127.0.0.1:8090` # Hikvision interface prefix
4. `PROXY_USERNAME=media` # Basic access authentication
5. `PROXY_PASSWORD=media!12345` # Basic access authentication
6. `PROXY_TEST=false` # Just for test

### Docker

``` bash
docker run -itd / 
-e 'PROXY_APP_KEY=asd' /
-e 'PROXY_APP_SECRET=asd' /
-e 'PROXY_HIK_SERVICE_PREFIX=asd' /
-e 'PROXY_USERNAME=media' /
-e 'PROXY_PASSWORD=media!12345' /
-e 'PROXY_TEST=false' /
-p 9999:9999 /
--name media-netty-proxy /
media-netty-proxy
```

### Docker compose

``` yaml
version: '3'

services:

  media-netty-proxy:
    image: media-netty-proxy:latest
    container_name: media-netty-proxy
    restart: always
    environment:
      PROXY_APP_KEY: ${PROXY_APP_KEY}
      PROXY_APP_SECRET: ${PROXY_APP_SECRET}
      PROXY_HIK_SERVICE_PREFIX: ${PROXY_HIK_SERVICE_PREFIX}
      PROXY_USERNAME: media
      PROXY_PASSWORD: media!12345
      PROXY_TEST: "false"
    ports:
      - 9999:9999
#    volumes:
    networks:
      - media-service
#    privileged: "true"

networks:
  media-service:
    driver: bridge
```

## Test

``` shell
cd {project_path}/frontend
npm i
npm run serve
```
