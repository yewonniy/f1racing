package com.f1racing.f1_racing.domain.pastGrandprix.repository.year2024;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.LapData24;

public interface LapData24Repository extends JpaRepository <LapData24, Long>{

    List<LapData24> findBySessionKeyOrderByLapNumberAsc(Integer sessionKey);
    
    boolean existsBySessionKey(Integer sessionKey);
}
