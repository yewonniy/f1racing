package com.f1racing.f1_racing.domain.pastGrandprix.entity.year2024;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceSession {
    @Id
    private Integer sessionKey; // pk (OpenF1의 api session key 그대로 사용)

    private String countryName; // 국가 (Bahrain)
    private String circuitShortName; // 서킷 (Sakhir)
    private String dateStart; // 경기 시작 시간
    @Column(name = "gmt_offset")
    private String gmtOffset; // 예 : "11:00:00" 
}
