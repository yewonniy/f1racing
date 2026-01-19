package com.f1racing.f1_racing.domain.pastGrandprix.controller;
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.f1racing.f1_racing.domain.pastGrandprix.dto.IntegratedRaceDataDto;
import com.f1racing.f1_racing.domain.pastGrandprix.dto.RaceDataRequestDto;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.RaceSession24;
import com.f1racing.f1_racing.domain.pastGrandprix.service.PlayerService;
import com.f1racing.f1_racing.domain.pastGrandprix.service.RaceService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequiredArgsConstructor
public class RaceController {

    private final RaceService raceService;
    private final PlayerService playerService;  // 재생용 서비스
    private final SimpMessagingTemplate messagingTemplate;

    // 1. 경기 목록 가져오기
    @Operation(summary = "파라미터 : 연도. -> 해당 연도에 있었던 모든 그랑프리 조회용 api")
    @GetMapping("/api/race/sessions")
    public ResponseEntity<Object> getSessions(
        @RequestParam int year
    ) {
        if (year == 2024) {
            return ResponseEntity.ok(raceService.getAllSessionsIn2024());
        }
        return ResponseEntity.ok(raceService.getAllSessionsIn2025());
    }

    // 2. 경기 변경 요청 (재생 시작)
    /* 
    @Operation(summary = "모르겠다.. 그랑프리 재생 api인데 손봐야함")
    @PostMapping("/play/{sessionKey}")
    public ResponseEntity<String> playRace(
            @PathVariable int sessionKey,
            @RequestParam(required = false) String startTime
    ) {
        System.out.println("▶️ 재생 요청: 세션 " + sessionKey + (startTime != null ? " (점프: " + startTime + ")" : ""));
        
        raceService.playRaceSession(sessionKey, startTime); 
        
        return ResponseEntity.ok("재생 시작됨");
    } */


    /**
     * 📩 클라이언트 요청: /app/race/data (/app을 붙이면 websocket 통신))
     * { "year": 2024, "sessionKey": 9480, "startTime": 1709392000000 }
     * @param request
     * @param principal
     * @return
     */
    @Operation(summary = "sessionKey와 연도를 전달하면, 해당 그랑프리의 모든 RaceData 반환 <조회용 API>")
	@GetMapping("/race/data") 
	public ResponseEntity<List<IntegratedRaceDataDto>> getRaceData(
		RaceDataRequestDto request,
        Principal principal
	) {
		// 1. 서비스 호출 (DB에서 60초 치 데이터 가져오기)
        List<IntegratedRaceDataDto> dataChunk = playerService.getRaceData(
            request.getYear(), 
            request.getSessionKey(), 
            request.getStartTime()
        );

        // 2. 데이터 전송 (경로: /topic/race/{sessionKey})
        // 주의: 이 경로는 '해당 세션을 보고 있는 모든 유저'에게 갑니다.
        // 만약 '요청한 나한테만' 오게 하려면 @SendToUser를 쓰거나 경로에 userId를 포함해야 합니다.
        // 여기서는 간단하게 세션별 토픽으로 구현했습니다.
        messagingTemplate.convertAndSend(
                "/topic/race/" + request.getSessionKey(), 
                dataChunk
        );
        
        // 💡 팁: 응답 데이터에 '다음 요청을 위한 정보(nextStartTime)'를 같이 주면 프론트가 편합니다.
        // 예: { "data": [...], "nextStartTime": 1709392060000 }
	}

    /** 
	 * 프론트엔드가 타임 슬라이더를 움직일 때마다 호출할 <조회용 API>
	 */
    /* 
	@Operation(summary = "프론트엔드가 타임 슬라이더를 움직일 때마다 호출할 <조회용 API> -> 수정 필요")
	@GetMapping("/skip/{sessionKey}") // http://localhost:8080/api/race/skip/9472?timestamp=1709393400000
	public ResponseEntity<List<IntegratedRaceDataDto>> skippingRaceData(
		@PathVariable int sessionKey, 
        @RequestParam int year, // 몇년도 경기?
		@RequestParam double timestamp, // 현재 슬라이더가 위치한 시간 (밀리초!! 16:12:34 = 1709305200123) -> 즉, 시작 시간!
		@RequestParam(defaultValue = "3000") int windowSize // 앞뒤로 얼만큼 가져올지 (기본 = 3초)
	) {
		// ex: 만약 15시 00분 00초 데이터를 요청하면 -> 15:00:00 ~ 15:00:03 사이 데이터를 줌
		double startTs = timestamp;
		double endTs = timestamp + windowSize;

		return ResponseEntity.ok(playerService.getRaceData(sessionKey, year));
	}
    */
}
