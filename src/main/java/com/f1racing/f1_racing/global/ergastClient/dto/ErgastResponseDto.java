package com.f1racing.f1_racing.global.ergastClient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErgastResponseDto {
    @JsonProperty("MRData")
    private MRData mrData;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MRData {
        @JsonProperty("StandingsTable")
        private StandingsTable standingsTable;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StandingsTable {
        @JsonProperty("StandingsLists")
        private List<StandingsList> standingsLists;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StandingsList {
        @JsonProperty("season")
        private String season;
        @JsonProperty("DriverStandings")
        private List<DriverStanding> driverStandings;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DriverStanding {
        private String position;
        private String points;
        private String wins;
        @JsonProperty("Driver")
        private DriverInfo driver;
        @JsonProperty("Constructors")
        private List<Constructor> constructors;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DriverInfo {
        private String driverId;
        private String permanentNumber;
        private String code;
        private String givenName;
        private String familyName;
        private String dateOfBirth;
        private String nationality;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Constructor {
        private String constructorId;
        private String name;
    }
}
