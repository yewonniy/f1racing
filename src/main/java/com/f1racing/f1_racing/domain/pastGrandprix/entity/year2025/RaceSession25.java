package com.f1racing.f1_racing.domain.pastGrandprix.entity.year2025;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "race_session_25")
public class RaceSession25 {
    @Id
    private Integer sessionKey; // pk (OpenF1의 api session key 그대로 사용)

    private String countryName; // 국가 (Bahrain)
    private String circuitShortName; // 서킷 (Sakhir)
    private String dateStart; // 경기 시작 시간
}
