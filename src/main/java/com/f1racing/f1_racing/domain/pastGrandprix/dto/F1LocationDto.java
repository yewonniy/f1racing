package com.f1racing.f1_racing.domain.pastGrandprix.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class F1LocationDto {
    @JsonProperty("date") // 시간
    private String timestamp;

    @JsonProperty("driver_number") // 드라이버 번호
    private int driverNumber;

    @JsonProperty("x") // x 좌표 (위도 경도가 아닌, 평면 지도용 좌표)
    private Integer x;

    @JsonProperty("y") // y 좌표
    private Integer y;
}   
