package com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 조회 속도를 위해 인덱스 걸기 (세션키 + 시간 순서)
@Table(name = "race_data_25", indexes = @Index(name = "idx_session_time", columnList = "sessionKey, timestamp"))
public class RaceData25 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 데이터가 수백만 개라 Long 타입 PK 필요

    private Integer sessionKey;   // 어느 경기인지 (FK 개념)
    private Integer driverNumber; // 누구인지
    
    private LocalDateTime timestamp; // 언제
    
    private Integer x;
    private Integer y;
    
    private Integer speed;
}
