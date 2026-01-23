package com.f1racing.f1_racing.domain.pastGrandprix.repository.year2024;

import org.springframework.data.jpa.repository.JpaRepository;


import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.RaceSession24;

public interface RaceSessionRepository extends JpaRepository<RaceSession24, Integer> {
    RaceSession24 findBySessionKey(Integer sessionKey);
}
