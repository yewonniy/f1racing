package com.f1racing.f1_racing.domain.pastGrandprix.repository.year2024;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.RaceData24;
import com.f1racing.f1_racing.redis.dto.RaceDataRedisDto;

import io.lettuce.core.dynamic.annotation.Param;

public interface RaceData24Repository extends JpaRepository<RaceData24, Long> {

        // 특정 경기의 데이터를 시간순으로 정렬해서 가져오기
        List<RaceData24> findBySessionKeyOrderByTimestampAsc(Integer sessionKey);
    
        // 이미 저장된 경기인지 확인용
        boolean existsBySessionKey(Integer sessionKey);

        List<RaceData24> findAllBySessionKey(Integer sessionKey);

        @Query("SELECT new com.f1racing.f1_racing.redis.dto.RaceDataRedisDto(" +
           "r.driverNumber, r.x, r.y, r.speed, r.timestamp) " +
           "FROM RaceData25 r WHERE r.sessionKey = :sessionKey " +
           "AND r.timestamp >= :startTs AND r.timestamp < :endTs")
        List<RaceDataRedisDto> findDataByTimeRange(
                @Param("sessionKey") Integer sessionKey,
                @Param("startTs") LocalDateTime startTs, // DB 타입이 LocalDateTime이면 변환 필요
                @Param("endTs") LocalDateTime endTs);

        // 2. [메모리 보호용] 데이터를 페이징(Page) 처리해서 가져오기
        @Query("SELECT new com.f1racing.f1_racing.redis.dto.RaceDataRedisDto(" +
                "r.driverNumber, r.x, r.y, r.speed, r.timestamp) " +
                "FROM RaceData25 r WHERE r.sessionKey = :sessionKey")
        Page<RaceDataRedisDto> findAllBySessionKeyPaged(@Param("sessionKey") Integer sessionKey, Pageable pageable);
}
