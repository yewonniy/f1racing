package com.f1racing.f1_racing.domain.pastGrandprix.repository.year2024;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024.RaceData;

public interface RaceDataRepository extends JpaRepository<RaceData, Long> {

        // 특정 경기의 데이터를 시간순으로 정렬해서 가져오기
        List<RaceData> findBySessionKeyOrderByTimestampAsc(Integer sessionKey);
    
        // 이미 저장된 경기인지 확인용
        boolean existsBySessionKey(Integer sessionKey);
}
