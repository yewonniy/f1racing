package com.f1racing.f1_racing.domain.driver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1racing.f1_racing.domain.driver.dto.DriverListResponseDTO;
import com.f1racing.f1_racing.domain.driver.dto.DriverResponseDTO;
import com.f1racing.f1_racing.domain.driver.entity.Driver;
import com.f1racing.f1_racing.domain.driver.repository.DriverRepository;
import com.f1racing.f1_racing.global.client.F1Client;
import com.f1racing.f1_racing.global.client.dto.ErgastResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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

	private final DriverRepository driverRepository;
	private final RedisTemplate<String, String> redisTemplate; // <key, value>
	private final ObjectMapper objectMapper; // 자바 객체 <-> JSON 변환 담당 (Redis는 자바 객체 못알아들음. JSON으로 변환해야함)
	private final F1Client f1Client;
	/**
	 * 모든 드라이버 조회 (순위 순)
	 * Look-aside 캐싱 패턴 적용: Redis → DB → Redis 저장
	 */
	@Transactional(readOnly = true)
	public DriverListResponseDTO getAllDrivers() {
		// 1. Redis에서 조회 시도
		String cachedData = redisTemplate.opsForValue().get(REDIS_KEY_ALL_DRIVERS); // drivers:all이라는 라벨이 붙은 통이 있나 확인
		if (cachedData != null) {
			log.info("Cache hit: Redis에서 모든 드라이버 데이터 조회 성공");
			try {
				DriverListResponseDTO cachedResponse = objectMapper.readValue(
					cachedData, DriverListResponseDTO.class); // readValue : 이 JSON을 자바 DTO 객체로 변환해라.
				return cachedResponse;
			} catch (Exception e) {
				log.warn("JSON을 자바 DTO 객체로 변환 실패, DB에서 데이터 조회", e);
			}
		}

		// 2. Redis에 없으면 DB에서 조회
		log.info("Cache miss: DB에서 모든 드라이버 데이터 조회 시도");
		List<Driver> drivers = driverRepository.findAllOrderByPosition(); // db에서 찾아옴.
		// entity -> dto 변환
		List<DriverResponseDTO> driverDTOs 
			= drivers.stream() // stream = 드라이버 20명 한줄로 세움
			.map(DriverResponseDTO::from) // 모든 driver 하나하나에 대해 DriverResponseDTO.from(Driver driver) 을 실행
			.collect(Collectors.toList()); // 변신한 DTO를 다시 리스트에 담음

		DriverListResponseDTO response = DriverListResponseDTO.builder()
			.drivers(driverDTOs)
			.totalCount(driverDTOs.size())
			.build();

		// 3. DB에서 가져온 데이터를 Redis에 저장 ⭐⭐ (TTL 1시간)
		try {
			String jsonData = objectMapper.writeValueAsString(response); // 자바 객체 -> JSON
			redisTemplate.opsForValue().set(REDIS_KEY_ALL_DRIVERS, jsonData, CACHE_TTL); // 해당 키 칸에, jsonData를 넣고, TTL (유통기한)을 붙임
			log.info("Redis에 캐싱 성공 with TTL 1 hour");
		} catch (Exception e) {
			log.error("Redis에 캐싱 실패", e);
		}

		return response;
	}

	/**
	 * 드라이버 ID로 조회
	 * Look-aside 캐싱 패턴 적용
	 */
	@Transactional(readOnly = true)
	public Optional<DriverResponseDTO> getDriverById(Long id) {
		// 1. Redis에서 조회 시도
		String cacheKey = REDIS_KEY_DRIVER_BY_ID + id;
		String cachedData = redisTemplate.opsForValue().get(cacheKey);
		if (cachedData != null) {
			log.info("Cache hit: Redis에서 드라이버 {} 반환", id);
			try {
				DriverResponseDTO cachedDriver = objectMapper.readValue(
					cachedData, DriverResponseDTO.class);
				return Optional.of(cachedDriver);
			} catch (Exception e) {
				log.warn("JSON을 자바 DTO 객체로 변환 실패, DB에서 데이터 조회", e);
			}
		}

		// 2. Redis에 없으면 DB에서 조회
		log.info("Cache miss: DB에서 드라이버 {} 반환", id);
		Optional<Driver> driver = driverRepository.findById(id);
		if (driver.isEmpty()) {
			return Optional.empty();
		}

		DriverResponseDTO response = DriverResponseDTO.from(driver.get());

		// 3. DB에서 가져온 데이터를 Redis에 저장 (TTL 1시간)
		try {
			String jsonData = objectMapper.writeValueAsString(response);
			redisTemplate.opsForValue().set(cacheKey, jsonData, CACHE_TTL);
			log.info("Redis에 캐싱 성공 {}", id);
		} catch (Exception e) {
			log.error("Redis에 캐싱 실패", e);
		}

		return Optional.of(response);
	}

	/**
	 * 드라이버 ID(F1 API ID)로 조회
	 */
	@Transactional(readOnly = true)
	public Optional<DriverResponseDTO> getDriverByDriverId(String driverId) {
		Optional<Driver> driver = driverRepository.findByDriverId(driverId);
		return driver.map(DriverResponseDTO::from);
	}

	/**
	 * 팀별 드라이버 조회
	 */
	@Transactional(readOnly = true)
	public DriverListResponseDTO getDriversByTeam(String team) {
		List<Driver> drivers = driverRepository.findByTeam(team);
		List<DriverResponseDTO> driverDTOs = drivers.stream()
			.map(DriverResponseDTO::from)
			.collect(Collectors.toList());

		return DriverListResponseDTO.builder()
			.drivers(driverDTOs)
			.totalCount(driverDTOs.size())
			.build();
	}

	/**
	 * 캐시 무효화 (드라이버 정보 업데이트 시 사용(레이스 끝난 일요일))
	 */
	public void evictCache() {
		redisTemplate.delete(REDIS_KEY_ALL_DRIVERS); // 모든 드라이버 캐시 삭제
		log.info("Evicted all drivers cache");
	}

	public void evictCache(Long driverId) { // 특정 드라이버 캐시 삭제
		String cacheKey = REDIS_KEY_DRIVER_BY_ID + driverId;
		redisTemplate.delete(cacheKey);
		log.info("Evicted driver {} cache", driverId);
	}

	@Transactional
    public void fetchAndSaveAllDriversInfo() {
        // 1. Client를 통해 데이터 가져오기
        ErgastResponseDto response = f1Client.getDriverStandings();

        // 데이터 껍질 벗기기 (null 체크)
        if (response == null || response.getMrData() == null) return;

        // 실제 드라이버 리스트 추출
        List<ErgastResponseDto.DriverStanding> standingList = 
            response.getMrData().getStandingsTable().getStandingsLists().get(0).getDriverStandings();

        // 2. 기존 데이터 삭제 (Sync 기능이므로 초기화)
        driverRepository.deleteAll();
		driverRepository.flush();

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
            Driver driverEntity = Driver.builder()
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

            // 저장
            driverRepository.save(driverEntity);
        }

        // 4. 캐시 삭제 (Eviction)
        redisTemplate.delete(REDIS_KEY_ALL_DRIVERS);
        log.info("F1 Data Synced & Cache Evicted!");
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

