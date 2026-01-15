package com.f1racing.f1_racing.domain.pastGrandprix.controller;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.RaceSession24;
import com.f1racing.f1_racing.domain.pastGrandprix.service.RaceService;
import com.f1racing.f1_racing.redis.dto.RaceDataRedisDto;
import com.f1racing.f1_racing.redis.service.RedisCacheService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/race")
@RequiredArgsConstructor
public class RaceController {

    private final RaceService raceService;
    private final RedisCacheService redisCacheService;

    // 1. 경기 목록 가져오기
    @Operation(summary = "파라미터 : 연도. -> 해당 연도에 있었던 모든 그랑프리 정보 조회용 api")
    @GetMapping("/sessions")
    public ResponseEntity<Object> getSessions(
        @RequestParam int year
    ) {
        if (year == 2024) {
            return ResponseEntity.ok(raceService.getAllSessionsIn2024());
        }
        return ResponseEntity.ok(raceService.getAllSessionsIn2025());
    }

    // 2. 경기 변경 요청 (재생 시작)
    @Operation(summary = "모르겠다.. 그랑프리 재생 api인데 손봐야함")
    @PostMapping("/play/{sessionKey}")
    public ResponseEntity<String> playRace(
            @PathVariable int sessionKey,
            @RequestParam(required = false) String startTime
    ) {
        System.out.println("▶️ 재생 요청: 세션 " + sessionKey + (startTime != null ? " (점프: " + startTime + ")" : ""));
        
        raceService.playRaceSession(sessionKey, startTime); 
        
        return ResponseEntity.ok("재생 시작됨");
    }

    	/** 
	 * 프론트엔드가 타임 슬라이더를 움직일 때마다 호출할 <조회용 API>
	 */
	@Operation(summary = "프론트엔드가 타임 슬라이더를 움직일 때마다 호출할 <조회용 API>")
	@GetMapping("/skip/{sessionKey}") // http://localhost:8080/api/race/skip/9472?timestamp=1709393400000
	public ResponseEntity<List<RaceDataRedisDto>> getRaceData(
		@PathVariable int sessionKey, 
        @RequestParam int year, // 몇년도 경기?
		@RequestParam double timestamp, // 현재 슬라이더가 위치한 시간 (밀리초!! 16:12:34 = 1709305200123)
		@RequestParam(defaultValue = "3000") int windowSize // 앞뒤로 얼만큼 가져올지 (기본 = 3초)
	) {
		// ex: 만약 15시 00분 00초 데이터를 요청하면 -> 15:00:00 ~ 15:00:03 사이 데이터를 줌
		double startTs = timestamp;
		double endTs = timestamp + windowSize;

		List<RaceDataRedisDto> result = redisCacheService.getRaceDataByTimeRange(sessionKey, year, startTs, endTs);

		return ResponseEntity.ok(result);
	}
}
