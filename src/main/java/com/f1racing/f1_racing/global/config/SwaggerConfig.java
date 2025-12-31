package com.f1racing.f1_racing.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("F1 Racing API")
				.description("F1 실시간 경기 정보 및 드라이버/팀 정보 API")
				.version("v1.0")
				.contact(new Contact()
					.name("F1 Racing Team")
					.email("f1racing@example.com")));
	}
}

