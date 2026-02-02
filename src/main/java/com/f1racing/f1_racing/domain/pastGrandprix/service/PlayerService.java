package com.f1racing.f1_racing.domain.pastGrandprix.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2025.RaceData25Repository;
import com.f1racing.f1_racing.domain.pastGrandprix.dto.IntegratedRaceDataDto;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2024.RaceData24Repository;


@Service
@RequiredArgsConstructor
public class PlayerService {
    private final RaceData24Repository raceData24Repository;
    private final RaceData25Repository raceData25Repository;
    // 📺 스트리밍 청크 사이즈 (60초)
    // 유튜브/넷플릭스는 보통 4~10초 청크를 쓰지만, F1 데이터는 텍스트라 60초도 거뜬합니다.
    private static final long CHUNK_SIZE_SECONDS = 60;
    private Future<?> currentPlayTask;

     /**
     * Pure DB로 Race Data (x,y 좌표/ 스피드) 정보 가져오기!
     * @param sessionKey
     * @param year
     * @param startTime
     * @return
     */
    @Transactional(readOnly = true)
    public List<IntegratedRaceDataDto> getRaceData(int year, int sessionKey, Long startTimeLong) {
        LocalDateTime startTs = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(startTimeLong), 
            ZoneId.of("UTC")
        );
        // 1. 끝나는 시간 계산 (요청 시간 + 60초)
        LocalDateTime endTs = startTs.plusSeconds(CHUNK_SIZE_SECONDS);
        // 🔥 [디버깅용 로그 추가] 프론트가 요청한 시간이 서버에선 언제로 인식되는지 확인
        /* System.out.println("=========================================");
        System.out.println("🔍 [요청] SessionKey: " + sessionKey);
        System.out.println("🔍 [요청] Timestamp(Long): " + startTimeLong);
        System.out.println("🔍 [변환] UTC 시간: " + startTs + " ~ " + endTs);
        System.out.println("========================================="); */
        // 2. 연도별 분기 처리 & Pure DB 조회
        if (year == 2024) {
            return raceData24Repository.findDataByTimeRange(sessionKey, startTs, endTs);
        } else if (year == 2025) {
            return raceData25Repository.findDataByTimeRange(sessionKey, startTs, endTs);
        }

        return Collections.emptyList();
    }

     
    /**
     * 수정 필요.. 일단 2025 데이터만 보고있음..
     * @param sessionKey
     * @param startTimeStr
     */
    /* 
    public void playRaceSession(int sessionKey, String startTimeStr) {
        if (currentPlayTask != null && !currentPlayTask.isDone()) {
            currentPlayTask.cancel(true);
        }

        currentPlayTask = taskExecutor.submit(() -> {
            try {
                log.info("📂 DB에서 세션 {} 데이터 로딩 중...", sessionKey);
                RaceSession25 session = raceSession25Repository.findById(sessionKey).orElse(null);
                if (session == null) {
                    log.error("세션 정보 없음: {}", sessionKey);
                    return;
                }

                List<RaceData25> raceData = raceData25Repository.findBySessionKeyOrderByTimestampAsc(sessionKey);
                
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

                List<RaceData25> playList = raceData.stream()
                        .filter(d -> !d.getTimestamp().isBefore(targetTime))
                        .toList();

                log.info("⏩ {} 부터 재생 시작! (남은 프레임: {})", targetTime, playList.size());

                for (RaceData25 entity : playList) {
                    if (Thread.currentThread().isInterrupted()) break; 

                    IntegratedRaceDataDto data = IntegratedRaceDataDto.builder()
                            .timestamp(entity.getTimestamp())
                            .driverNumber(entity.getDriverNumber())
                            .x(entity.getX())
                            .y(entity.getY())
                            .speed(entity.getSpeed())
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
    */
}
