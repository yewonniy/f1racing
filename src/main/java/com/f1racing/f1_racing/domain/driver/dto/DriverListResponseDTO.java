package com.f1racing.f1_racing.domain.driver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverListResponseDTO {
	private List<DriverResponseDTO> drivers;
	private Integer totalCount;
}

