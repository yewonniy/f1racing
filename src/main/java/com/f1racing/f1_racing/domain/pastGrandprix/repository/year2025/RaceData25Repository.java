package com.f1racing.f1_racing.domain.pastGrandprix.repository.year2025;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025.RaceData25;

public interface RaceData25Repository extends JpaRepository<RaceData25, Long>{
    List<RaceData25> findBySessionKeyOrderByTimestampAsc(Integer sessionKey);
    
    // 이미 저장된 경기인지 확인용
    boolean existsBySessionKey(Integer sessionKey);

}
