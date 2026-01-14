package com.f1racing.f1_racing.domain.pastGrandprix.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DriverDto {
    @JsonProperty("driver_number")
    private Integer driverNumber;
    
    // 필요하다면 이름이나 팀 정보도 받을 수 있습니다.
    // @JsonProperty("full_name")
    // private String fullName;
}
