package com.f1racing.f1_racing.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

// 이 config 파일 : "스프링아, 우리 집(서버)에 실시간 통신을 위한 '전용 우체국'을 하나 차려줘"라고 명령하는 파일
@Configuration
@EnableWebSocketMessageBroker // 이걸 붙여야 스프링이 "아 이제부터 웹소켓 서버 기능을 켜고 STOMP 프로토콜로 메세지를 주고 받겠군" 인식함
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 이 메서드는 메시지가 오고 가는 주소 규칙을 정함. (우체국이 주소를 보고 분류하는 것과 같음)
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. 서버 -> 클라이언트 (broadcast)
        config.enableSimpleBroker("/topic");  // 유저가 /topic/race라는 채널을 '구독'하고 있으면 서버가 그리로 데이터를 쐈을 때 다 같이 받는다. (브로드캐스트 통신)
        // 2. 클라이언트 -> 서버 (send 요청)
        config.setApplicationDestinationPrefixes("/app");  // 클라이언트가 /app/race라는 주소로 데이터를 보내면 서버가 그걸 받아서 처리한다.
    }

    // 웹소켓 전송 용량 제한 늘리기
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        // 기본값: 512KB (512 * 1024 = 524288)
        // 수정값: 10MB (10 * 1024 * 1024 = 10485760)
        
        registry.setMessageSizeLimit(10 * 1024 * 1024);      // 메시지 하나의 최대 크기
        registry.setSendBufferSizeLimit(10 * 1024 * 1024);   // 보내는 버퍼 사이즈 제한
        registry.setSendTimeLimit(20 * 1000);                // 보내는 데 걸리는 시간 제한 (20초)
    }

    // 웹소켓에 들어오는 문 (endpoint)
    /**
     * "스프링아!
        ws://localhost:8080/ws-f1 으로 들어오는 연결을 받아줘! (누구든 상관없어)
        혹시 웹소켓 안 되는 환경이면 SockJS로 어떻게든 연결해줘.
        손님이 /app 으로 시작하는 주소로 보내면 나(서버)한테 가져오고,
        내가 /topic 으로 시작하는 주소로 보내면 구독하고 있는 손님들한테 다 뿌려줘!"
    */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 스프링아, ws://localhost:8080/ws-f1 로 들어오는 연결을 받도록 해!
        registry.addEndpoint("/ws-f1") // 웹소켓 연결 주소를 만들겠다! /ws-f1으로 요청이 들어오면 웹소켓인것.
                .setAllowedOriginPatterns("*") // 모든 도메인 허용 (CORS 해결)
                .withSockJS(); // 안전장치. 웹소켓 미지원 브라우저를 위한 설정
    }
}
