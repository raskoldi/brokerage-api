package com.brokerage.api.exception;
import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends ApiException {

    public InsufficientFundsException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
