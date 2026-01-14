package com.f1racing.f1_racing.domain.pastGrandprix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceDataDto {
    private LocalDateTime date;
    private Integer driverNumber;
    private Integer x;
    private Integer y;
    private Integer speed; // 합체된 속도 정보
}
