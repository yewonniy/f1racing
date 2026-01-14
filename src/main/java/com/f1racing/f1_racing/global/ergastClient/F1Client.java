package com.f1racing.f1_racing.global.ergastClient;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.f1racing.f1_racing.global.ergastClient.dto.ErgastResponseDto;

// Ergast F1 API 를 이용해서 드라이버 정보를 가져오는 클라이언트
@Component
@RequiredArgsConstructor
public class F1Client {

    private final RestTemplate restTemplate = new RestTemplate(); // chrome처럼 행동! API_URL 주소로 가서 GET 요청을 함!
    private final String API_URL = "https://api.jolpi.ca/ergast/f1/current/driverStandings.json";

    public ErgastResponseDto getDriverStandings() {
 
        // 헤더 만들기 (나 모질라(chrome) 브라우저야!)
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        // 요청 엔티티에 헤더 담기
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // exchange로 요청 보내기 (Url, get 방식, 헤더 포함, 받을 타입)
        ResponseEntity<ErgastResponseDto> response = restTemplate.exchange(
            API_URL,
            HttpMethod.GET,
            entity,
            ErgastResponseDto.class);

        // f1 api로부터 받은 내용 body만 꺼내서 리턴
        return response.getBody(); 
    }

}