package com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lap_data_24", 
       indexes = @Index(name = "idx_lap_session", columnList = "sessionKey"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LapData24 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40)
    private String driverName; // 해당 랩의 패스티스트 드라이버 이름

    @Column(nullable = false)
    private Integer sessionKey; // ex: 9472

    @Column(nullable = false)
    private Integer lapNumber; // 현재 몇번째 랩인지

    @Column(name = "date_start")
    private LocalDateTime dateStart; // 해당 랩이 시작된 시간
    
    private Double lapDuration;  // 패스티스트 드라이버의 랩 타임
}
