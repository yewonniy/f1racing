package com.f1racing.f1_racing.domain.driver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "drivers")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String driverId; // F1 API에서 제공하는 고유 ID

	@Column(nullable = false)
	private String firstName;

	@Column(nullable = false)
	private String lastName;

	@Column(nullable = false)
	private String nationality;

	@Column(nullable = false)
	private String team;

	@Column(nullable = false)
	private Integer driverNumber;

	@Column(nullable = false)
	private Integer position; // 현재 시즌 순위

	@Column(nullable = false)
	private Integer points; // 현재 시즌 포인트

	@Column(nullable = false)
	private Integer wins; // 현재 시즌 우승 몇번

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

