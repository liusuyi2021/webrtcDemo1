package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.util.WebAppRootListener;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Set;

/**
 * @ClassName: WebSocketConfig
 * @Description:
 * @Author: 刘苏义
 * @Date: 2023年05月11日8:25
 * @Version: 1.0
 **/
@Configuration
public class WebSocketConfig implements ServletContainerInitializer {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }


    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext) throws ServletException {
        servletContext.addListener(WebAppRootListener.class);
        servletContext.setInitParameter("org.apache.tomcat.websocket.textBufferSize", "12");
        servletContext.setInitParameter("org.apache.tomcat.websocket.binaryBufferSize", "12");
    }
}