package com.f1racing.f1_racing.domain.pastGrandprix.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SpeedInfoDto {
    @JsonProperty("date")
    private String date;

    @JsonProperty("speed")
    private Integer speed; // 시속 (km/h)
    
    @JsonProperty("rpm")
    private Integer rpm;   // 엔진 회전수 (나중에 쓸지도?)
    
    @JsonProperty("n_gear")
    private Integer gear;  // 기어 단수 (나중에 쓸지도?)

    @JsonProperty("driver_number")
    private Integer driverNumber;
} 
