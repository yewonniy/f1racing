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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.f1racing.f1_racing.domain.driver.entity.Driver24;
import com.f1racing.f1_racing.domain.driver.repository.DriverRepository24;
import com.f1racing.f1_racing.domain.driver.repository.DriverRepository25;
import com.f1racing.f1_racing.domain.pastGrandprix.dto.F1LocationDto;
import com.f1racing.f1_racing.domain.pastGrandprix.dto.IntegratedRaceDataDto;
import com.f1racing.f1_racing.domain.pastGrandprix.dto.SessionDto;
import com.f1racing.f1_racing.domain.pastGrandprix.dto.SpeedInfoDto;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025.LapData25;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025.RaceData25;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.LapData24;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.RaceData24;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.RaceSession24;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025.RaceSession25;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2024.LapData24Repository;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2024.RaceData24Repository;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2024.RaceSessionRepository;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2025.LapData25Repository;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2025.RaceData25Repository;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2025.RaceSession25Repository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaceService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RaceSessionRepository raceSession24Repository;
    private final RaceData24Repository raceData24Repository;
    private final RaceData25Repository raceData25Repository;
    private final RaceSession25Repository raceSession25Repository;
    // private final LapData25Repository lapData25Repository;
    // private final DriverRepository25 driverRepository25;
    private final LapData24Repository lapData24Repository;
    private final DriverRepository24 driverRepository24;
    
    private ThreadPoolTaskExecutor taskExecutor; 

    // @PostConstruct
    public void initExecutor() {
        this.taskExecutor = new ThreadPoolTaskExecutor();
        this.taskExecutor.setCorePoolSize(1); 
        this.taskExecutor.setThreadNamePrefix("Race-Replay-");
        this.taskExecutor.initialize();
        
        new Thread(this::crawlingAllRaces).start();
    }

    private void crawlingAllRaces() {
        log.info("🌍 24/25년 모든 그랑프리 정보 조회 시작...");
        RestTemplate rt = createRestTemplate();

        try {
            String sessionsUrl = "https://api.openf1.org/v1/sessions?year=2024&session_name=Race";
            SessionDto[] sessions = rt.getForObject(sessionsUrl, SessionDto[].class);

            if (sessions == null) return;
            log.info("총 {}개의 그랑프리 일정을 찾았습니다.", sessions.length);

            for (SessionDto session : sessions) {

                /* if (raceData25Repository.existsBySessionKey(session.getSessionKey())) {
                    log.info("⏭️ [{}] 데이터는 이미 DB에 있습니다.", session.getCountryName());
                    if (!raceSession25Repository.existsById(session.getSessionKey())) {
                         saveSessionInfo(session);
                    }
                    continue;
                }

                log.info("📥 [{}] 데이터 수집 시작 (Key: {}, Start: {})", 
                        session.getCountryName(), session.getSessionKey(), session.getDateStart());
                
                saveSessionInfo(session); */
                
                // crawlLapData(session.getSessionKey(), session.getDateStart());
                
                // 아래는 그랑프리 0.2초단위 위치 저장하는 메서드
                // crawlRaceData(session.getSessionKey(), parseTime(session.getDateStart()));
            }
            log.info("🎉 모든 그랑프리 데이터 DB 저장 및 확인 완료!");

        } catch (Exception e) {
            log.error("초기화 중 오류 발생", e);
        }
    }

    private void crawlLapData(int sessionKey, String officialStartTime) {
        if (lapData24Repository.existsBySessionKey(sessionKey)) return;

        log.info("🏁 [Session {}] 랩(Lap) 타임테이블 수집 시작 (DB 드라이버 정보 활용)", sessionKey);
        RestTemplate rt = createRestTemplate();

        try {
            // 1. 내 DB(Driver25)에서 정보 조회
            List<Driver24> allDrivers = driverRepository24.findAll();
            // List -> Map<Integer, String> 변환 (Key: 번호, Value: 이름)
            // 예: { 1: "Verstappen", 44: "Hamilton", ... }
            Map<Integer, String> driverNameMap = allDrivers.stream()
                    .collect(Collectors.toMap(
                            Driver24::getDriverNumber, 
                            Driver24::getLastName,
                            (existing, replacement) -> existing // 혹시 모를 중복 방지
                    ));

            // 2. 랩 데이터 수집 (여기는 그대로 OpenF1 API 사용)
            String url = "https://api.openf1.org/v1/laps?session_key=" + sessionKey;
            Map<String, Object>[] laps = rt.getForObject(url, Map[].class);
            
            Map<Integer, String> apiDriverMap = new HashMap<>(); 
            boolean apiCalled = false; // API 호출 여부 플래그

            if (laps == null) return;

            LocalDateTime realStartTime = calculateRealStartTime(sessionKey, officialStartTime);
            Map<Integer, LapData24> fastestLapMap = new HashMap<>();

            for (Map<String, Object> lap : laps) {
                Integer driverNum = (Integer) lap.get("driver_number");
                Integer lapNumber = (Integer) lap.get("lap_number");
                String dateStartStr = (String) lap.get("date_start");
                Object durationObj = lap.get("lap_duration");
                Double duration = (durationObj != null) ? ((Number) durationObj).doubleValue() : null;
  
                String driverName = driverNameMap.get(driverNum); // 1차: DB 확인

                LocalDateTime currentLapTime = null;

                if (driverName == null) {
                    // 🚨 DB에 없다! -> API 드라이버 목록 확인
                    if (!apiCalled) {
                        try {
                            String driverUrl = "https://api.openf1.org/v1/drivers?session_key=" + sessionKey;
                            Map<String, Object>[] apiDrivers = rt.getForObject(driverUrl, Map[].class);
                            if (apiDrivers != null) {
                                for (Map<String, Object> d : apiDrivers) {
                                    Integer num = (Integer) d.get("driver_number");
                                    String name = (String) d.get("last_name");
                                    if (name == null) name = (String) d.get("name_acronym");
                                    if (num != null && name != null) apiDriverMap.put(num, name);
                                }
                            }
                            apiCalled = true; // API는 한 번만 호출
                            log.info("📢 [Session {}] DB에 없는 드라이버가 있어 API 명단을 추가 로딩했습니다.", sessionKey);
                        } catch (Exception e) {
                            log.warn("API 드라이버 로딩 실패");
                        }
                    }
                    // 2차: API 맵에서 재확인
                    driverName = apiDriverMap.getOrDefault(driverNum, "Unknown");
                }

                // (시간 파싱 로직 동일)
                if (lapNumber == 1) {
                    currentLapTime = realStartTime; 
                } else if (dateStartStr != null) {
                    currentLapTime = parseTime(dateStartStr);
                }

                if (currentLapTime != null) {
                    if (!fastestLapMap.containsKey(lapNumber)) {
                        fastestLapMap.put(lapNumber, LapData24.builder()
                                .sessionKey(sessionKey)
                                .lapNumber(lapNumber)
                                .dateStart(currentLapTime)
                                .lapDuration(duration)
                                .driverName(driverName) // 👈 DB에서 가져온 "Verstappen" 등이 들어감
                                .build());
                    } else {
                        LapData24 existing = fastestLapMap.get(lapNumber);
                        if (currentLapTime.isBefore(existing.getDateStart())) {
                            existing.setDateStart(currentLapTime);
                            existing.setLapDuration(duration);
                            existing.setDriverName(driverName); // 👈 선두 바뀔 때 이름도 업데이트
                        }
                    }
                }
            }

            // 5. 저장
            List<LapData24> optimizedEntities = new ArrayList<>(fastestLapMap.values());
            if (!optimizedEntities.isEmpty()) {
                optimizedEntities.sort(Comparator.comparingInt(LapData24::getLapNumber));
                lapData24Repository.saveAll(optimizedEntities);
                log.info("💾 [Session {}] 선두 랩 정보 DB 저장 완료 (총 {}개)", sessionKey, optimizedEntities.size());
            }

        } catch (Exception e) {
            log.error("Lap 데이터 수집 중 오류", e);
        }
    }

    // sesisonKey를 넣으면, 해당 그랑프리가 시작한 "진짜 시간"을 반환함
    // 3. [핵심 로직] 
    // Lap 1은 정지 출발이므로 Lap 2보다 보통 12~13초 정도 더 걸린다 치면
    // Lap 2 시작 시점에서 (Lap 2 시간 + 12.5초)를 뺀다
    // => 포메이션 랩을 제외한 'Lights Out' 시점이 나옴.
    private LocalDateTime calculateRealStartTime(int sessionKey, String officialStartTime) {
        RestTemplate rt = createRestTemplate(); // 기존에 만들어둔 메서드 활용
        String url = String.format("https://api.openf1.org/v1/laps?session_key=%d&lap_number=2&driver_number=1", sessionKey);
    
        try {
            Map<String, Object>[] laps = rt.getForObject(url, Map[].class);
    
            if (laps != null && laps.length > 0) {
                Map<String, Object> lap2 = laps[0];
                String lap2StartStr = (String) lap2.get("date_start");
                Object durationObj = lap2.get("lap_duration");
    
                if (lap2StartStr != null && durationObj != null) {
                    double lap2Duration = ((Number) durationObj).doubleValue();
                    OffsetDateTime lap2StartTime = OffsetDateTime.parse(lap2StartStr);
    
                    // 공식: Lap 2 시작 시간 - (Lap 2 주행 시간 + 보정치 12.5초)
                    double standingStartOffset = 12.5;
                    
                    return lap2StartTime
                            .minusNanos((long)((lap2Duration + standingStartOffset) * 1_000_000_000L))
                            .toLocalDateTime();
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [Session {}] 찐 시작 시간 계산 중 오류 (공식 시간 사용 권장): {}", sessionKey, e.getMessage());
        }
        
        return parseTime(officialStartTime); // 계산 실패 시 null 반환
    }

    private RestTemplate createRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    private void saveSessionInfo(SessionDto dto) {
        // [수정 2] Entity는 String을 원하므로 변환 없이 그대로 넣음!
        RaceSession25 session = RaceSession25.builder()
                .sessionKey(dto.getSessionKey())
                .countryName(dto.getCountryName())
                .circuitShortName(dto.getCircuitShortName())
                .dateStart(dto.getDateStart()) // String 그대로!
                .build();
            raceSession25Repository.save(session);
    }

    /**
     * api 서버에 요청 보내서 sessionkey 그랑프리 데이터 크롤링
     * 2025 데이터임.
     * @param sessionKey
     * @param officialStartTime
     */
    private void crawlRaceData(int sessionKey, LocalDateTime officialStartTime) {
        log.info("🔎 세션 {} ({} 이후 데이터) 드라이버 목록 조회...", sessionKey, officialStartTime);
        List<Integer> driverNumbers = fetchDriverNumbers(sessionKey);
        
        ExecutorService executor = Executors.newFixedThreadPool(2); 
        List<CompletableFuture<Void>> futures = driverNumbers.stream()
                .map(driverNum -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(700);
                        List<IntegratedRaceDataDto> dataList = loadDataForDriver(sessionKey, driverNum, officialStartTime);
                        
                        if (!dataList.isEmpty()) {
                            List<RaceData25> entities = dataList.stream()
                                    .map(d -> RaceData25.builder()
                                            .sessionKey(sessionKey)
                                            .driverNumber(d.getDriverNumber())
                                            .timestamp(d.getTimestamp())
                                            .x(d.getX())
                                            .y(d.getY())
                                            .speed(d.getSpeed())
                                            .build())
                                    .collect(Collectors.toList());

                            raceData25Repository.saveAll(entities);
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
    
    private List<IntegratedRaceDataDto> loadDataForDriver(int sessionKey, int driverNumber, LocalDateTime startTime) {
        RestTemplate restTemplate = createRestTemplate();
        List<IntegratedRaceDataDto> result = new ArrayList<>();

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

    private List<IntegratedRaceDataDto> mergeData(F1LocationDto[] locs, SpeedInfoDto[] speeds) {
        List<IntegratedRaceDataDto> merged = new ArrayList<>();
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
            
            merged.add(IntegratedRaceDataDto.builder()
                    .timestamp(locTime)
                    .driverNumber(loc.getDriverNumber())
                    .x(loc.getX())
                    .y(loc.getY())
                    .speed(closestSpeed.getSpeed())
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

    public List<RaceSession25> getAllSessionsIn2025() {
        return raceSession25Repository.findAll();
    }

    public List<RaceSession24> getAllSessionsIn2024() {
        return raceSession24Repository.findAll();
    }

}