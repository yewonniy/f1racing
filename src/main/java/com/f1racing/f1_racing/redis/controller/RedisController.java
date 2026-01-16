package com.f1racing.f1_racing.redis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.f1racing.f1_racing.redis.service.RedisCacheService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class RedisController {

    private final RedisCacheService redisCacheService; 

    // sessionKey (어떤 그랑프리인지), year (몇년도 경기인지) 파라미터 필요
    @Operation(summary = "Redis에 랩 정보 저장, 프론트가 직접 호출할 일 X", description = "타임 슬라이더를 움직일 때마다 DB를 긁는 게 아니라, Redis에서 순식간에 데이터를 꺼내오도록 하기 위해, Redis에 데이터를 저장하는 api")
    @PostMapping("/{sessionKey}/cache")
    public ResponseEntity<String> cacheSession(
        @PathVariable int sessionKey,
        @RequestParam(defaultValue = "2025") int year) {

        redisCacheService.cacheRaceDataAsync(sessionKey, year);
        return ResponseEntity.ok("캐싱 완료! Redis를 확인해보세요.");
    }
}
