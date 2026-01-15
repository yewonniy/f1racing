package com.f1racing.f1_racing.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/*
자바: RaceData, LocalDateTime 같은 객체(Object)를 씀.
Redis: 그냥 문자열(String)이나 0101(Byte) 밖에 모름.
그래서 스프링이 Redis랑 대화하려면 "통역사"가 필요함

RedisConfig 파일은 스프링 부트가 실행될 때:
"야 스프링! Redis랑 대화할 때는 이 통역사(Serializer)를 써서 대화해! 그리고 연결은 이렇게 하고!"
라고 미리 설정(Configuration)을 잡아주는 파일이다.
*/
@Configuration
public class RedisConfig {

	/*
	redisTemplate 메서드 = "만능 리모컨"을 만드는 공장
	우리가 나중에 서비스 코드(RaceService)에서 Redis에 데이터를 넣거나 뺄 때, 
	Redis 명령어를 직접 칠 수는 없음 (Java 코드 안에서 SET key value 이렇게 칠 순 없으니까)
	
	대신 자바에서 Redis를 아주 쉽게 조종할 수 있게 해주는 도구(Bean)가 바로 RedisTemplate이다.
	이 메서드는 "우리가 원하는 옵션이 장착된 리모컨(redisTemplate)을 만들어서 스프링 창고(Bean Container)에 넣어두는 역할"을 함
	*/
	@Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>(); // 리모컨 몸체 만들기
        template.setConnectionFactory(connectionFactory); // 리모컨과 Redis 서버를 연결

		// 1. key 는 String으로 저장한다.
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		objectMapper.activateDefaultTyping(
			LaissezFaireSubTypeValidator.instance,
			ObjectMapper.DefaultTyping.NON_FINAL,
			JsonTypeInfo.As.PROPERTY
		);

		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
		// -> value를 저장할 땐 자바 객체를 JSON으로 변환해서 저장해라
        // -> 우리가 redisTemplate.opsForValue().set("key", raceData객체)라고만 해도 알아서 { "speed": 300, "driver": "Verstappen" } 이렇게 JSON으로 바뀌어서 저장됨
		
		template.afterPropertiesSet();
        return template;
    }
}

