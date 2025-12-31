package com.f1racing.f1_racing.domain.driver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.f1racing.f1_racing.domain.driver.entity.Driver;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

	Optional<Driver> findByDriverId(String driverId);

	@Query("SELECT d FROM Driver d ORDER BY d.position ASC")
	List<Driver> findAllOrderByPosition();

	List<Driver> findByTeam(String team);
}

