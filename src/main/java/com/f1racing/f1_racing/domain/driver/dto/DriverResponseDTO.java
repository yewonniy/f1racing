package com.f1racing.f1_racing.domain.driver.dto;

import com.f1racing.f1_racing.domain.driver.entity.Driver24;
import com.f1racing.f1_racing.domain.driver.entity.Driver25;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponseDTO {
	private Long id;
	private String driverId;
	private String firstName;
	private String lastName;
	private String fullName;
	private String nationality;
	private String team;
	private Integer driverNumber;
	private Integer position;
	private Integer points;
	private String dateOfBirth;
	private String permanentNumber;
    private Integer wins;

	public static DriverResponseDTO from(Driver25 driver) {
		return DriverResponseDTO.builder()
			.id(driver.getId())
			.driverId(driver.getDriverId())
			.firstName(driver.getFirstName())
			.lastName(driver.getLastName())
			.fullName(driver.getFirstName() + " " + driver.getLastName())
			.nationality(driver.getNationality())
			.team(driver.getTeam())
			.driverNumber(driver.getDriverNumber())
			.position(driver.getPosition())
			.points(driver.getPoints())
			.dateOfBirth(driver.getDateOfBirth())
			.permanentNumber(driver.getPermanentNumber())
            .wins(driver.getWins())
			.build();
	}
}

