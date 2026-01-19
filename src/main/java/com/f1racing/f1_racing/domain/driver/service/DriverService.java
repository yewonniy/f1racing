package com.f1racing.f1_racing.domain.driver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1racing.f1_racing.domain.driver.dto.DriverListResponseDTO;
import com.f1racing.f1_racing.domain.driver.dto.DriverResponseDTO;
import com.f1racing.f1_racing.domain.driver.entity.Driver24;
import com.f1racing.f1_racing.domain.driver.entity.Driver25;
import com.f1racing.f1_racing.domain.driver.repository.DriverRepository24;
import com.f1racing.f1_racing.domain.driver.repository.DriverRepository25;
import com.f1racing.f1_racing.global.ergastClient.F1Client;
import com.f1racing.f1_racing.global.ergastClient.dto.ErgastResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverService {

	private static final String REDIS_KEY_ALL_DRIVERS = "drivers:all"; // Redis라는 큰 사물함에서 물건을 찾기 위한 라벨
	private static final String REDIS_KEY_DRIVER_BY_ID = "driver:id:";
	private static final Duration CACHE_TTL = Duration.ofHours(1); // TTL 1시간 = 1시간 뒤에 해당 캐시 삭제

	private final DriverRepository25 driverRepository25;
	private final ObjectMapper objectMapper; // 자바 객체 <-> JSON 변환 담당 (Redis는 자바 객체 못알아들음. JSON으로 변환해야함)
	private final F1Client f1Client;

	private final DriverRepository24 driverRepository24;
	/**
	 * 모든 드라이버 조회 (순위 순)
	 * Look-aside 캐싱 패턴 적용: Redis → DB → Redis 저장
	 */
	@Transactional(readOnly = true)
	public DriverListResponseDTO getAllDrivers(int year) {
		// DB에서 조회
		List<DriverResponseDTO> driverDTOs;

		if (year == 2024) {
			List<Driver24> drivers = driverRepository24.findAllOrderByPosition(); // db에서 찾아옴.
			// entity -> dto 변환
			driverDTOs = drivers.stream() // stream = 드라이버 20명 한줄로 세움
				.map(DriverResponseDTO::from) // 모든 driver 하나하나에 대해 DriverResponseDTO.from(Driver24 driver) 을 실행
				.collect(Collectors.toList()); // 변신한 DTO를 다시 리스트에 담음
		} else if (year == 2025) {
			List<Driver25> drivers = driverRepository25.findAllOrderByPosition(); // db에서 찾아옴.
			// entity -> dto 변환
			driverDTOs = drivers.stream() // stream = 드라이버 20명 한줄로 세움
				.map(DriverResponseDTO::from) // 모든 driver 하나하나에 대해 DriverResponseDTO.from(Driver25 driver) 을 실행
				.collect(Collectors.toList()); // 변신한 DTO를 다시 리스트에 담음
		} else {
			driverDTOs = List.of();
		}

		DriverListResponseDTO response = DriverListResponseDTO.builder()
			.drivers(driverDTOs)
			.totalCount(driverDTOs.size())
			.build();

		return response;
	}

	/**
	 * 드라이버 ID로 조회
	 * Look-aside 캐싱 패턴 적용
	 */
	@Transactional(readOnly = true)
	public Optional<DriverResponseDTO> getDriverById(Long id) {

		// 2. Redis에 없으면 DB에서 조회
		log.info("Cache miss: DB에서 드라이버 {} 반환", id);
		Optional<Driver25> driver = driverRepository25.findById(id);
		if (driver.isEmpty()) {
			return Optional.empty();
		}

		DriverResponseDTO response = DriverResponseDTO.from(driver.get());

		return Optional.of(response);
	}

	/**
	 * 드라이버 ID(F1 API ID)로 조회
	 */
	@Transactional(readOnly = true)
	public Optional<DriverResponseDTO> getDriverByDriverId(String driverId) {
		Optional<Driver25> driver = driverRepository25.findByDriverId(driverId);
		return driver.map(DriverResponseDTO::from);
	}

	/**
	 * 팀별 드라이버 조회
	 */
	@Transactional(readOnly = true)
	public DriverListResponseDTO getDriversByTeam(String team, int year) {
		List<DriverResponseDTO> driverDTOs;

		if (year == 2024) {
			List<Driver24> drivers = driverRepository24.findByTeam(team);
			driverDTOs = drivers.stream()
				.map(DriverResponseDTO::from)
				.collect(Collectors.toList());
		} else if (year == 2025) {
			List<Driver25> drivers = driverRepository25.findByTeam(team);
			driverDTOs = drivers.stream()
				.map(DriverResponseDTO::from)
				.collect(Collectors.toList());
		} else {
			driverDTOs = List.of();
		}
		
		return DriverListResponseDTO.builder()
			.drivers(driverDTOs)
			.totalCount(driverDTOs.size())
			.build();
	}



	@Transactional
    public void fetchAndSaveAllDriversInfo2025() {
        // 1. Client를 통해 데이터 가져오기
        ErgastResponseDto response = f1Client.getDriverStandings(2025);

        // 데이터 껍질 벗기기 (null 체크)
        if (response == null || response.getMrData() == null) return;

        // 실제 드라이버 리스트 추출
        List<ErgastResponseDto.DriverStanding> standingList = 
            response.getMrData().getStandingsTable().getStandingsLists().get(0).getDriverStandings();

        // 2. 기존 데이터 삭제 (Sync 기능이므로 초기화)
        driverRepository25.deleteAll();
		driverRepository25.flush();

        // 3. 반복문으로 엔티티 변환 및 저장
        for (ErgastResponseDto.DriverStanding info : standingList) {
            
            // DTO 내부 드라이버/팀 꺼내기
            ErgastResponseDto.DriverInfo driverInfo = info.getDriver();
            ErgastResponseDto.Constructor constructor = info.getConstructors().get(0);

            // 숫자 변환 (String -> Integer) 안전하게 처리
            // 점수는 "25.5"처럼 소수점이 올 수도 있어서 Double로 1차 변환 후 int로 캐스팅
            int points = (int) Double.parseDouble(info.getPoints());
            int position = Integer.parseInt(info.getPosition());
			int wins = Integer.parseInt(info.getWins());
            // 등번호가 없으면 0번으로 처리 (신인 드라이버 등 예외 처리)
            int driverNum = parseIntegerOrDefault(driverInfo.getPermanentNumber(), 0); 

            // 빌더 패턴으로 엔티티 생성
            Driver25 driverEntity = Driver25.builder()
                .driverId(driverInfo.getDriverId())       // "max_verstappen"
                .firstName(driverInfo.getGivenName())     // "Max"
                .lastName(driverInfo.getFamilyName())     // "Verstappen"
                .nationality(driverInfo.getNationality()) // "Dutch"
                .team(constructor.getName())              // "Red Bull"
                .driverNumber(driverNum)                  // 33 (or 1)
                .position(position)                       // 1
                .points(points)                           // 450
                .dateOfBirth(driverInfo.getDateOfBirth()) // "1997-09-30"
                .permanentNumber(driverInfo.getPermanentNumber()) // "33" (String 저장)
				.wins(wins)
                .build();
			
			log.info("드라이버 : {} ",driverEntity);
            // 저장
            driverRepository25.save(driverEntity);
        }
    }

	@Transactional
    public void fetchAndSaveAllDriversInfo2024() {
        // 1. Client를 통해 데이터 가져오기
        ErgastResponseDto response = f1Client.getDriverStandings(2024);

        // 데이터 껍질 벗기기 (null 체크)
        if (response == null || response.getMrData() == null) return;

        // 실제 드라이버 리스트 추출
        List<ErgastResponseDto.DriverStanding> standingList = 
            response.getMrData().getStandingsTable().getStandingsLists().get(0).getDriverStandings();

        // 2. 기존 데이터 삭제 (Sync 기능이므로 초기화)
        driverRepository24.deleteAll();
		driverRepository24.flush();

        // 3. 반복문으로 엔티티 변환 및 저장
        for (ErgastResponseDto.DriverStanding info : standingList) {
            
            // DTO 내부 드라이버/팀 꺼내기
            ErgastResponseDto.DriverInfo driverInfo = info.getDriver();
            ErgastResponseDto.Constructor constructor = info.getConstructors().get(0);

            // 숫자 변환 (String -> Integer) 안전하게 처리
            // 점수는 "25.5"처럼 소수점이 올 수도 있어서 Double로 1차 변환 후 int로 캐스팅
            int points = (int) Double.parseDouble(info.getPoints());
            int position = Integer.parseInt(info.getPosition());
			int wins = Integer.parseInt(info.getWins());
            // 등번호가 없으면 0번으로 처리 (신인 드라이버 등 예외 처리)
            int driverNum = parseIntegerOrDefault(driverInfo.getPermanentNumber(), 0); 

            // 빌더 패턴으로 엔티티 생성
            Driver24 driverEntity = Driver24.builder()
                .driverId(driverInfo.getDriverId())       // "max_verstappen"
                .firstName(driverInfo.getGivenName())     // "Max"
                .lastName(driverInfo.getFamilyName())     // "Verstappen"
                .nationality(driverInfo.getNationality()) // "Dutch"
                .team(constructor.getName())              // "Red Bull"
                .driverNumber(driverNum)                  // 33 (or 1)
                .position(position)                       // 1
                .points(points)                           // 450
                .dateOfBirth(driverInfo.getDateOfBirth()) // "1997-09-30"
                .permanentNumber(driverInfo.getPermanentNumber()) // "33" (String 저장)
				.wins(wins)
                .build();
			
			log.info("드라이버 : {} ",driverEntity);
            // 저장
            driverRepository24.save(driverEntity);
        }

    }

    // [보너스] 숫자로 바꿀 때 null이거나 에러나면 기본값(0)을 주는 안전장치 메서드
    private int parseIntegerOrDefault(String value, int defaultValue) {
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
	}

