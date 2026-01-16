package com.f1racing.f1_racing.domain.driver.controller;

import com.f1racing.f1_racing.domain.driver.dto.DriverListResponseDTO;
import com.f1racing.f1_racing.domain.driver.dto.DriverResponseDTO;
import com.f1racing.f1_racing.domain.driver.service.DriverService;
import com.f1racing.f1_racing.global.common.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Swagger는 이 클래스를 보고 만들어지는 것임!!
 */
@Tag(name = "Driver", description = "F1 드라이버 정보 API")
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

	private final DriverService driverService;

	@Operation(summary = "모든 드라이버 조회", description = "순위 순으로 정렬된 모든 드라이버 정보를 조회합니다.")
	@GetMapping
	public ResponseEntity<GlobalResponse<DriverListResponseDTO>> getAllDrivers() {
		DriverListResponseDTO response = driverService.getAllDrivers();
		return ResponseEntity.ok(GlobalResponse.success(response));
	}

	@Operation(summary = "드라이버 ID로 조회", description = "드라이버 ID로 특정 드라이버 정보를 조회합니다.")
	@GetMapping("/{id}")
	public ResponseEntity<GlobalResponse<DriverResponseDTO>> getDriverById(@PathVariable Long id) {
		Optional<DriverResponseDTO> driver = driverService.getDriverById(id);
		if (driver.isEmpty()) {
			return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(GlobalResponse.error("Driver not found with id: " + id));
		}
		return ResponseEntity.ok(GlobalResponse.success(driver.get()));
	}

	@Operation(summary = "F1 API 드라이버 ID로 조회", description = "F1 API에서 제공하는 드라이버 ID로 조회합니다.")
	@GetMapping("/driver-id/{driverId}")
	public ResponseEntity<GlobalResponse<DriverResponseDTO>> getDriverByDriverId(
		@PathVariable String driverId) {
		Optional<DriverResponseDTO> driver = driverService.getDriverByDriverId(driverId);
		if (driver.isEmpty()) {
			return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(GlobalResponse.error("Driver not found with driverId: " + driverId));
		}
		return ResponseEntity.ok(GlobalResponse.success(driver.get()));
	}

	@Operation(summary = "팀별 드라이버 조회", description = "특정 팀의 드라이버들을 조회합니다.")
	@GetMapping("/team/{team}")
	public ResponseEntity<GlobalResponse<DriverListResponseDTO>> getDriversByTeam(
		@PathVariable String team) {
		DriverListResponseDTO response = driverService.getDriversByTeam(team);
		return ResponseEntity.ok(GlobalResponse.success(response));
	}

	/**
	 * 외부 API 호출해서 드라이버 정보 얻어오기
	 * @return
	 */
	@Operation(summary = "호출하지 마삼")
	@PostMapping("/sync")
    public GlobalResponse<String> syncDrivers() {
        driverService.fetchAndSaveAllDriversInfo();
        return GlobalResponse.success("F1 데이터 동기화 완료");
    }

}

