package com.f1racing.f1_racing.domain.driver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "driver_25")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver25 {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String driverId; // max_verstappen

	@Column(nullable = false)
	private String firstName;

	@Column(nullable = false)
	private String lastName; // Verstappen

	@Column(nullable = false)
	private String nationality;

	@Column(nullable = false)
	private String team; // RedBull

	@Column(nullable = false)
	private Integer driverNumber; // 1

	@Column(nullable = false)
	private Integer position; // 25 시즌 순위

	@Column(nullable = false)
	private Integer points; // 25 시즌 포인트

	@Column(nullable = false)
	private Integer wins; // 2025 시즌 우승 몇번

	private String dateOfBirth;

	private String permanentNumber;

	public void updatePosition(Integer position) {
		this.position = position;
	}

	public void updatePoints(Integer points) {
		this.points = points;
	}

	public void updateTeam(String team) {
		this.team = team;
	}
}

