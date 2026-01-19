package com.f1racing.f1_racing.domain.pastGrandprix.repository.year2025;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // 2. 올바른 Import

import com.f1racing.f1_racing.domain.pastGrandprix.dto.IntegratedRaceDataDto;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025.RaceData25;

public interface RaceData25Repository extends JpaRepository<RaceData25, Long>{

    List<RaceData25> findBySessionKeyOrderByTimestampAsc(Integer sessionKey);

    boolean existsBySessionKey(Integer sessionKey);

    // 4. Pure DB 최적화 쿼리 (필요한 컬럼만 조회 + DTO 변환)
    @Query("SELECT new com.f1racing.f1_racing.domain.pastGrandprix.dto.IntegratedRaceDataDto(" +
           "r.driverNumber, r.x, r.y, r.speed, r.timestamp) " +
           "FROM RaceData25 r " + 
           "WHERE r.sessionKey = :sessionKey " +
           "AND r.timestamp >= :startTs AND r.timestamp < :endTs" +
              "ORDER BY r.timestamp ASC")
    List<IntegratedRaceDataDto> findDataByTimeRange(
            @Param("sessionKey") Integer sessionKey,
            @Param("startTs") Long startTs, // 5. 타입 일치 (보통 DB timestamp는 Long)
            @Param("endTs") Long endTs);

    // 5. 대량 데이터 조회용 (Page보다 Slice가 성능상 유리 - Count 쿼리 안 날림)
    @Query("SELECT new com.f1racing.f1_racing.domain.pastGrandprix.dto.IntegratedRaceDataDto(" +
            "r.driverNumber, r.x, r.y, r.speed, r.timestamp) " +
            "FROM RaceData25 r WHERE r.sessionKey = :sessionKey")
    Slice<IntegratedRaceDataDto> findAllBySessionKeySliced(@Param("sessionKey") Integer sessionKey, Pageable pageable);
}
