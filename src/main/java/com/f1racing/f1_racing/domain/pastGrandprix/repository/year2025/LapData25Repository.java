package com.f1racing.f1_racing.domain.pastGrandprix.repository.year2025;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025.LapData25;

public interface LapData25Repository extends JpaRepository<LapData25, Long> {
    // 재생할 때: 이 리스트를 통째로 가져와서 Redis나 메모리에 올림
    List<LapData25> findBySessionKeyOrderByLapNumberAsc(Integer sessionKey);
    
    boolean existsBySessionKey(Integer sessionKey);
    
}
