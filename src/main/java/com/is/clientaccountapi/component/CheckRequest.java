package com.is.clientaccountapi.component;

import com.is.clientaccountapi.model.result.ApiResponse;
import com.is.clientaccountapi.model.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class CheckRequest {
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public boolean isInvalidId(Long id) {
        return id == null || id <= 0;
    }

    public boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public boolean isDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public <T> ApiResponse<T> dbError(Exception e) {
        log.error(e.getMessage(), e);
        return ApiResponse.error(ErrorCode.DB_ERROR, ErrorCode.DB_ERROR.getCause() + ": " + e.getMessage(), null);
    }
}
