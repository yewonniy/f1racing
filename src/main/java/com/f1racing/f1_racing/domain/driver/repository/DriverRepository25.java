package com.f1racing.f1_racing.domain.driver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.f1racing.f1_racing.domain.driver.entity.Driver25;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository25 extends JpaRepository<Driver25, Long> {

	Optional<Driver25> findByDriverId(String driverId);

	@Query("SELECT d FROM Driver25 d ORDER BY d.position ASC")
	List<Driver25> findAllOrderByPosition();

	List<Driver25> findByTeam(String team);

	Optional<Driver25> findByLastName(String lastName);
}

