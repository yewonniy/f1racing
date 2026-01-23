package com.f1racing.f1_racing.domain.pastGrandprix.repository.year2025;

import org.springframework.data.jpa.repository.JpaRepository;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025.RaceSession25;

public interface RaceSession25Repository extends JpaRepository<RaceSession25, Integer>{
    RaceSession25 findBySessionKey(Integer sessionKey);
}