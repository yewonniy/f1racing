package com.f1racing.f1_racing.domain.pastGrandprix.controller;
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
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
     * 📩 클라이언트 요청: /app/race/data (/app을 붙이면 websocket 통신)
     * { "year": 2024, "sessionKey": 9480, "startTime": 1709392000000 }
     * 웹소켓 구독할 때 경로 뒤에 고유 ID(UUID) 하나만 붙여줘.
     * 구독(SUB): /topic/race/9480/my-unique-id-123
     * 요청(PUB): /app/race/data 보낼 때 JSON 안에 clientId: "my-unique-id-123" 이거 꼭 넣어줘., 그래야 다른 유저 데이터랑 안 섞이고 니꺼만 받을 수 있어."
     * @param request
     * @param principal
     * @return
     */
    @Operation(summary = "<조회용 API> 그랑프리 다시 보기 기능", 
        description = "year, sessionKey, startTime, uuid를 보내주면 -> 1분치 데이터를 전송. 프론트에서 버퍼가 다 떨어진 걸 확인했거나 or 사용자가 타임슬라이더를 움직이면 다시 요청할 것")
	@MessageMapping("/race/data") 
	public void handleRaceDataRequest(
		RaceDataRequestDto request
        // Principal principal
	) {
		// 1. 서비스 호출 (DB에서 60초 치 데이터 가져오기)
        List<IntegratedRaceDataDto> dataChunk = playerService.getRaceData(
            request.getYear(), 
            request.getSessionKey(), 
            request.getStartTime()
        );

        // 2. 데이터 전송 (요청한 사람의 '전용 채널'로 쏴주기)
        // 변경 전: /topic/race/9480 (모두가 다 받음 ❌)
        // 변경 후: /topic/race/9480/user-1234 (요청한 나만 받음 ✅)
        messagingTemplate.convertAndSend(
            "/topic/race/" + request.getSessionKey() + "/" + request.getClientId(), 
            dataChunk
        );
	}
    /**
     * 프론트한테 설명할때:
     * "우리는 라이브가 아니라 다시보기라서 유저마다 보는 시간이 다 달라. 그래서 철수가 요청한 데이터를 영희가 받으면 안 돼.
     * 너네가 접속할 때 랜덤한 UUID(clientId) 하나 생성해줘.
     * 받을 때(Sub): /topic/race/{sessionKey}/{clientId} 로 구독해.
     * 요청할 때(Pub): clientId 필드에 그 UUID를 꼭 담아서 보내줘.
     * 그러면 내가 그 ID를 보고 너한테만 데이터를 쏴줄게!"
     */

    /** ----------------------------------------------------------------------------
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
