package com.f1racing.f1_racing.domain.pastGrandprix.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LapAndFastestDriver {

    private String driverName; // 해당 랩의 패스티스트 드라이버 이름
    private Integer lapNumber; // 현재 몇번째 랩인지
    private LocalDateTime dateStart; // 해당 랩이 시작된 시간
    private Double lapDuration;
}
