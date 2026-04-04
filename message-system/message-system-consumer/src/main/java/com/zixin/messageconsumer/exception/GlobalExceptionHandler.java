package com.zixin.messageconsumer.exception;

import com.zixin.utils.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result handleResponseStatusException(ResponseStatusException e) {
        log.warn("Access forbidden: {}", e.getReason());
        return Result.error(e.getReason() != null ? e.getReason() : "Access denied");
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleNullPointerException(NullPointerException e) {
        log.error("NullPointerException occurred", e);
        return Result.error("服务调用异常，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        return Result.error("Internal server error: " + e.getMessage());
    }
}
