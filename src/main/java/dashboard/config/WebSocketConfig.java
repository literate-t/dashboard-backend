package dashboard.config;

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
    // 프론트엔드가 구독(Subscribe)할 경로의 접두사
    config.enableSimpleBroker("/topic");
    // 프론트엔드에서 서버로 메시지를 보낼 때 사용할 접두사
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-dashboard")
        .setAllowedOriginPatterns("http://localhost:5173")
        .withSockJS(); // 호환성을 위한 fallback 옵션
  }
}
