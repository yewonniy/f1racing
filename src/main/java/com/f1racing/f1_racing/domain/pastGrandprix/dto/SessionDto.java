package com.f1racing.f1_racing.domain.pastGrandprix.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SessionDto {

    // 1. 세션 고유 키 (예: 9472) - 가장 중요!
    @JsonProperty("session_key")
    private Integer sessionKey;

    // 2. 개최 국가 이름 (예: Bahrain) - 화면 표시용
    @JsonProperty("country_name")
    private String countryName;

    // 3. 서킷 이름 (예: Sakhir) - 화면 표시용
    @JsonProperty("circuit_short_name")
    private String circuitShortName;

    // 4. 경기 시작 시간 (예: 2024-03-02T15:00:00...) - 정렬용
    @JsonProperty("date_start")
    private String dateStart;

    // 5. 세션 이름 (예: "Race", "Qualifying") - 우리는 "Race"만 골라낼 때 확인용
    @JsonProperty("session_name")
    private String sessionName;

    // 6. 연도 (예: 2024)
    @JsonProperty("year")
    private Integer year;
}
