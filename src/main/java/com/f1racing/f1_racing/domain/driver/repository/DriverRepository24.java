package com.f1racing.f1_racing.domain.driver.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.f1racing.f1_racing.domain.driver.entity.Driver24;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository24 extends JpaRepository<Driver24, Long> {
	Optional<Driver24> findByDriverId(String driverId);

	@Query("SELECT d FROM Driver24 d ORDER BY d.position ASC")
	List<Driver24> findAllOrderByPosition();

	List<Driver24> findByTeam(String team);
    
    Optional<Driver24> findByLastName(String lastName);
} 
