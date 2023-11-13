package com.aaron.stream;

import com.aaron.stream.server.ProxyServer;

public class ProxyApplication {
    private final static int DEFAULT_PORT = 9999;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("proxy server starting");

        String portProperty = System.getProperty("server.port");
        int port;
        if (portProperty == null) {
            System.out.println("Can't found the server port[using -Dserver.port=<port>], using default port 9999");
            port = DEFAULT_PORT;
        } else {
            port = Integer.parseInt(portProperty);
        }

        new ProxyServer(port).start();
    }
}
