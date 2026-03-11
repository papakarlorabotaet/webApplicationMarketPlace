package ru.urfu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Включаем простой брокер для очередей сообщений
        config.enableSimpleBroker("/topic", "/queue");
        // Префикс для сообщений, которые идут на контроллер (@MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
        // Префикс для персональных сообщений
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws").withSockJS();// Точка подключения к WebSocket
    }
}