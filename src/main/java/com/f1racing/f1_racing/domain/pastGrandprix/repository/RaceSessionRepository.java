package com.f1racing.f1_racing.domain.pastGrandprix.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.RaceSession;

public interface RaceSessionRepository extends JpaRepository<RaceSession, Integer> {

}
