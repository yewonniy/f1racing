package com.f1racing.f1_racing.domain.pastGrandprix.service;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.f1racing.f1_racing.domain.pastGrandprix.dto.F1LocationDto;
import com.f1racing.f1_racing.domain.pastGrandprix.dto.RaceDataDto;
import com.f1racing.f1_racing.domain.pastGrandprix.dto.SessionDto;
import com.f1racing.f1_racing.domain.pastGrandprix.dto.SpeedInfoDto;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.RaceData;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.RaceSession;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.RaceDataRepository;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.RaceSessionRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaceService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RaceSessionRepository raceSessionRepository;
    private final RaceDataRepository raceDataRepository;
    
    private ThreadPoolTaskExecutor taskExecutor; 
    private Future<?> currentPlayTask;

    @PostConstruct
    public void initExecutor() {
        this.taskExecutor = new ThreadPoolTaskExecutor();
        this.taskExecutor.setCorePoolSize(1); 
        this.taskExecutor.setThreadNamePrefix("Race-Replay-");
        this.taskExecutor.initialize();
        
        new Thread(this::crawlingAllRaces).start();
    }

    private void crawlingAllRaces() {
        log.info("🌍 2024년 모든 그랑프리 정보 조회 시작...");
        RestTemplate rt = createRestTemplate();

        try {
            String sessionsUrl = "https://api.openf1.org/v1/sessions?year=2024&session_name=Race";
            SessionDto[] sessions = rt.getForObject(sessionsUrl, SessionDto[].class);

            if (sessions == null) return;
            log.info("총 {}개의 그랑프리 일정을 찾았습니다.", sessions.length);

            for (SessionDto session : sessions) {

                if (raceDataRepository.existsBySessionKey(session.getSessionKey())) {
                    log.info("⏭️ [{}] 데이터는 이미 DB에 있습니다.", session.getCountryName());
                    if (!raceSessionRepository.existsById(session.getSessionKey())) {
                         saveSessionInfo(session);
                    }
                    continue;
                }

                log.info("📥 [{}] 데이터 수집 시작 (Key: {}, Start: {})", 
                        session.getCountryName(), session.getSessionKey(), session.getDateStart());
                
                saveSessionInfo(session); 
                
                // [수정 1] 수집하러 갈 때는 날짜 계산이 필요하니 변환(parseTime)해서 보냄
                crawlRaceData(session.getSessionKey(), parseTime(session.getDateStart()));
            }
            log.info("🎉 모든 그랑프리 데이터 DB 저장 및 확인 완료!");

        } catch (Exception e) {
            log.error("초기화 중 오류 발생", e);
        }
    }

    private RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    private void saveSessionInfo(SessionDto dto) {
        // [수정 2] Entity는 String을 원하므로 변환 없이 그대로 넣음!
        RaceSession session = RaceSession.builder()
                .sessionKey(dto.getSessionKey())
                .countryName(dto.getCountryName())
                .circuitShortName(dto.getCircuitShortName())
                .dateStart(dto.getDateStart()) // String 그대로!
                .build();
        raceSessionRepository.save(session);
    }

    private void crawlRaceData(int sessionKey, LocalDateTime officialStartTime) {
        log.info("🔎 세션 {} ({} 이후 데이터) 드라이버 목록 조회...", sessionKey, officialStartTime);
        List<Integer> driverNumbers = fetchDriverNumbers(sessionKey);
        
        ExecutorService executor = Executors.newFixedThreadPool(2); 
        List<CompletableFuture<Void>> futures = driverNumbers.stream()
                .map(driverNum -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(700); 
                        List<RaceDataDto> dataList = loadDataForDriver(sessionKey, driverNum, officialStartTime);
                        
                        if (!dataList.isEmpty()) {
                            List<RaceData> entities = dataList.stream().map(d -> RaceData.builder()
                                    .sessionKey(sessionKey)
                                    .driverNumber(d.getDriverNumber())
                                    .timestamp(d.getDate())
                                    .x(d.getX())
                                    .y(d.getY())
                                    .speed(d.getSpeed())
                                    .build()).collect(Collectors.toList());

                            raceDataRepository.saveAll(entities);
                            log.info("💾 [Session {}] Driver {} 저장 ({} 건)", sessionKey, driverNum, entities.size());
                        }
                    } catch (Exception e) {
                        log.error("💥 Driver {} 저장 실패", driverNum, e);
                    }
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }
    
    private List<RaceDataDto> loadDataForDriver(int sessionKey, int driverNumber, LocalDateTime startTime) {
        RestTemplate restTemplate = createRestTemplate();
        List<RaceDataDto> result = new ArrayList<>();

        try {
            String timeStr = startTime.toString() + "Z";
            String commonQuery = String.format("session_key=%d&driver_number=%d&date%%3E=%s", 
                                                sessionKey, driverNumber, timeStr);
            
            String locUrl = "https://api.openf1.org/v1/location?" + commonQuery;
            String speedUrl = "https://api.openf1.org/v1/car_data?" + commonQuery;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("User-Agent", "Mozilla/5.0 (Compatible)");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            F1LocationDto[] locs = restTemplate.exchange(URI.create(locUrl), org.springframework.http.HttpMethod.GET, entity, F1LocationDto[].class).getBody();
            SpeedInfoDto[] speeds = restTemplate.exchange(URI.create(speedUrl), org.springframework.http.HttpMethod.GET, entity, SpeedInfoDto[].class).getBody();

            if (locs != null && speeds != null && locs.length > 0) {
                result = mergeData(locs, speeds);
            } 
        } catch (Exception e) {
            // ignore
        }
        return result;
    }

    private List<RaceDataDto> mergeData(F1LocationDto[] locs, SpeedInfoDto[] speeds) {
        List<RaceDataDto> merged = new ArrayList<>();
        if (speeds.length == 0) return merged;

        int speedIndex = 0;
        for (F1LocationDto loc : locs) {
            LocalDateTime locTime = parseTime(loc.getTimestamp());
            while (speedIndex < speeds.length - 1) {
                LocalDateTime currentSpeedTime = parseTime(speeds[speedIndex].getDate());
                LocalDateTime nextSpeedTime = parseTime(speeds[speedIndex + 1].getDate());
                long diffCurrent = Math.abs(Duration.between(locTime, currentSpeedTime).toMillis());
                long diffNext = Math.abs(Duration.between(locTime, nextSpeedTime).toMillis());
                if (diffNext < diffCurrent) speedIndex++;
                else break;
            }
            SpeedInfoDto closestSpeed = speeds[speedIndex];
            
            merged.add(RaceDataDto.builder()
                    .date(locTime)
                    .driverNumber(loc.getDriverNumber())
                    .x(loc.getX()).y(loc.getY()).speed(closestSpeed.getSpeed())
                    .build());
        }
        return merged;
    }

    private List<Integer> fetchDriverNumbers(int sessionKey) {
        try {
            RestTemplate rt = createRestTemplate();
            String url = "https://api.openf1.org/v1/drivers?session_key=" + sessionKey;
            SpeedInfoDto[] response = rt.getForObject(url, SpeedInfoDto[].class);
            if (response != null) {
                return Arrays.stream(response).map(SpeedInfoDto::getDriverNumber).distinct().toList();
            }
        } catch (Exception e) {
            log.error("드라이버 목록 조회 실패: Session {}", sessionKey);
        }
        return Collections.emptyList();
    }

    private LocalDateTime parseTime(String timeStr) {
        try { return OffsetDateTime.parse(timeStr).toLocalDateTime(); } 
        catch (Exception e) { return LocalDateTime.parse(timeStr); }
    }

    public List<RaceSession> getAllSessions() {
        return raceSessionRepository.findAll();
    }

    public void playRaceSession(int sessionKey, String startTimeStr) {
        if (currentPlayTask != null && !currentPlayTask.isDone()) {
            currentPlayTask.cancel(true);
        }

        currentPlayTask = taskExecutor.submit(() -> {
            try {
                log.info("📂 DB에서 세션 {} 데이터 로딩 중...", sessionKey);
                RaceSession session = raceSessionRepository.findById(sessionKey).orElse(null);
                if (session == null) {
                    log.error("세션 정보 없음: {}", sessionKey);
                    return;
                }

                List<RaceData> raceData = raceDataRepository.findBySessionKeyOrderByTimestampAsc(sessionKey);
                
                if (raceData.isEmpty()) {
                    log.warn("⚠️ 데이터가 없습니다.");
                    return;
                }

                // [수정 3] DB에서 꺼낼 때 String -> LocalDateTime 변환 필요
                LocalDateTime targetTime;
                if (startTimeStr != null && !startTimeStr.isEmpty()) {
                    String fixedTimeStr = startTimeStr.replace(" ", "+");
                    targetTime = OffsetDateTime.parse(fixedTimeStr).toLocalDateTime();
                } else {
                    // 세션의 dateStart가 String이므로 parseTime 사용!
                    targetTime = parseTime(session.getDateStart());
                }

                List<RaceData> playList = raceData.stream()
                        .filter(d -> !d.getTimestamp().isBefore(targetTime))
                        .toList();

                log.info("⏩ {} 부터 재생 시작! (남은 프레임: {})", targetTime, playList.size());

                for (RaceData entity : playList) {
                    if (Thread.currentThread().isInterrupted()) break; 

                    RaceDataDto data = RaceDataDto.builder()
                            .date(entity.getTimestamp())
                            .driverNumber(entity.getDriverNumber())
                            .x(entity.getX()).y(entity.getY()).speed(entity.getSpeed())
                            .build();

                    if (data.getSpeed() == 0) Thread.sleep(5); 
                    else Thread.sleep(100); 

                    messagingTemplate.convertAndSend("/topic/race", data);
                }
                log.info("🛑 레이스 종료");

            } catch (InterruptedException e) {
                log.info("⛔ 재생 중단");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("재생 중 에러", e);
            }
        });
    }
}