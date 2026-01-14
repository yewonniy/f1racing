package com.f1racing.f1_racing.domain.pastGrandprix.controller;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.RaceSession;
import com.f1racing.f1_racing.domain.pastGrandprix.service.RaceService;

@RestController
@RequestMapping("/api/race")
@RequiredArgsConstructor
public class RaceController {
    private final RaceService raceService;

    // 1. 경기 목록 가져오기
    @GetMapping("/sessions")
    public ResponseEntity<Object> getSessions() {
        return ResponseEntity.ok(raceService.getAllSessions());
    }

    // 2. 경기 변경 요청 (재생 시작)
    @PostMapping("/play/{sessionKey}")
    public ResponseEntity<String> playRace(
            @PathVariable int sessionKey,
            @RequestParam(required = false) String startTime
    ) {
        System.out.println("▶️ 재생 요청: 세션 " + sessionKey + (startTime != null ? " (점프: " + startTime + ")" : ""));
        
        raceService.playRaceSession(sessionKey, startTime); 
        
        return ResponseEntity.ok("재생 시작됨");
    }
}
