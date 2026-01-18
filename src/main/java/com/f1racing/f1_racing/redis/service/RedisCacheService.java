package com.f1racing.f1_racing.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2024.RaceData24Repository;
import com.f1racing.f1_racing.domain.pastGrandprix.repository.year2025.RaceData25Repository;
import com.f1racing.f1_racing.redis.dto.RaceDataRedisDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final RaceData24Repository raceData24Repository; // 👈 24년 저장소 추가
    private final RaceData25Repository raceData25Repository; // 👈 25년 저장소
    private final RedisTemplate<String, Object> redisTemplate;

    private static final long CACHE_TTL_HOURS = 1; 

    /**
     * DB 데이터를 Redis로 캐싱 (연도 파라미터 추가!)
     * 
     * 이게 얼마나 대단한 거냐면, 이제 사용자가 타임 슬라이더를 1초 단위로 막 드래그해도 DB는 아무 일도 안 하고
     * 메모리(Redis)에서 0.001초 만에 데이터를 툭툭 던져줄 수 있는 상태가 되는 것이다!!!!
     */
    // 비동기 캐싱 (백그라운드 작업) - 메모리 보호
    @Async // <- 별도 스레드에서 실행됨
    @Transactional(readOnly = true)
    public void cacheRaceDataAsync(int sessionKey, int year) {
        String key = "session:" + sessionKey + ":data";
        
        // 중복 실행 방지 (이미 누가 넣고 있으면 패스)
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) return;

        log.info("[Async] 대용량 데이터 캐싱 시작");

        int pageNumber = 0;
        int pageSize = 20000; // 한 번에 2만 개씩만 메모리에 올림 (메모리 안전)
        boolean hasNext = true;

        while (hasNext) {
            // 1. DB에서 2만 개만 가져옴 (PageRequest)
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<RaceDataRedisDto> pageData;

            if (year == 2024) {
                pageData = raceData24Repository.findAllBySessionKeyPaged(sessionKey, pageable);
            } else {
                pageData = raceData25Repository.findAllBySessionKeyPaged(sessionKey, pageable);
            }

            List<RaceDataRedisDto> dtoList = pageData.getContent(); // 우리가 요청한 2만개 데이터를 getContent로 꺼냄
            if (dtoList.isEmpty()) break;

            // 2. Redis 파이프라인으로 전송 (2만 개 묶음)
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                Set<ZSetOperations.TypedTuple<Object>> tuples = new HashSet<>();

                for (RaceDataRedisDto dto : dtoList) {
                    if (dto.getTimestamp() == null) continue;
                    double score = dto.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli();
                    tuples.add(new DefaultTypedTuple<>(dto, score));
                }

                redisTemplate.opsForZSet().add(key, tuples);
                return null;
            });

            log.info("🚀 [Async] {}페이지 저장 완료 ({} 건)", pageNumber, dtoList.size());

            // 3. 다음 페이지 준비
            if (pageData.hasNext()) {
                pageNumber++;
            } else {
                hasNext = false;
            }
        }
        
        // 만료 시간 설정
        redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.info("✅ [Async] 전체 캐싱 완료! (Session: {})", sessionKey);
    }

    public List<RaceDataRedisDto> getRaceDataByTimeRange(int sessionKey, int year, double startTs, double endTs) {

        /* String key = "session:" + sessionKey + ":data"; // Redis에서 조회할 key값

        // 1-1. Redis에 있으면 바로 반환 (Cache hit)
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Set<Object> rangeData = redisTemplate.opsForZSet().rangeByScore(key, startTs, endTs);
            log.info("🍏🍏🍏🍏캐시 hit!!!!🍏🍏🍏🍏");
            return (rangeData == null) ? List.of() : 
                   rangeData.stream().map(obj -> (RaceDataRedisDto) obj).collect(Collectors.toList());
        }

        // 1-2. Redis에 없으면? (Cache miss)
        log.info("💧💧Cache Miss!ㅠㅠ💧💧 (Session: {}) -> 백그라운드 적재 시작 & DB 즉시 조회", sessionKey);

        // A) 백그라운드에서 전체 캐싱 시작 (사용자는 안 기다림)
        cacheRaceDataAsync(sessionKey, year);  */

        // B) 당장 필요한 1초치만 DB에서 조회해서 바로 리턴 (0.1초 컷)
        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli((long) startTs), ZoneId.of("UTC"));
        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli((long) endTs), ZoneId.of("UTC"));
        
        if (year == 2024) {
             return raceData24Repository.findDataByTimeRange(sessionKey, start, end);
        } else {
             return raceData25Repository.findDataByTimeRange(sessionKey, start, end);
        }
    }

}