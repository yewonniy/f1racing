package com.f1racing.f1_racing.domain.pastGrandprix.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RaceDataRequestDto {
    private int year;           // 연도 (2024 vs 2025 분기 처리용)
    private int sessionKey;     // 경기 세션 ID
    private Long startTime;     // 요청 시작 시간 (밀리초!!! 타임스탬프)
    private String clientId;
    // 참고: endTime은 서버에서 정한다. (프론트가 달라고 하는 대로 다 주면 위험하니까!)
}
