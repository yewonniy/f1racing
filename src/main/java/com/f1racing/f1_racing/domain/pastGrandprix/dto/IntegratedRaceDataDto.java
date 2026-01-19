package com.f1racing.f1_racing.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025.RaceData25;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.RaceData24;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegratedRaceDataDto {
    private Integer driverNumber; // 드라이버 번호 (예: 1)
    private Integer x;            // X 좌표
    private Integer y;            // Y 좌표
    private Integer speed;        // 속도
    private LocalDateTime timestamp; // 시간

    // 1. 2025년 엔티티 -> DTO 변환
    public static IntegratedRaceDataDto from(RaceData25 entity) {
        return IntegratedRaceDataDto.builder()
                .driverNumber(entity.getDriverNumber())
                .x(entity.getX())
                .y(entity.getY())
                .speed(entity.getSpeed())
                .timestamp(entity.getTimestamp())
                .build();
    }

    // 2. 2024년 엔티티 -> DTO 변환
    public static IntegratedRaceDataDto from(RaceData24 entity) {
        return IntegratedRaceDataDto.builder()
                .driverNumber(entity.getDriverNumber())
                .x(entity.getX())
                .y(entity.getY())
                .speed(entity.getSpeed())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
