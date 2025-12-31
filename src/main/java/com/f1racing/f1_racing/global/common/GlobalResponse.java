package com.f1racing.f1_racing.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalResponse<T> {
	private boolean success;
	private String message;
	private T data;

	public static <T> GlobalResponse<T> success(T data) {
		return new GlobalResponse<>(true, "Success", data);
	}

	public static <T> GlobalResponse<T> success(String message, T data) {
		return new GlobalResponse<>(true, message, data);
	}

	public static <T> GlobalResponse<T> error(String message) {
		return new GlobalResponse<>(false, message, null);
	}
}

