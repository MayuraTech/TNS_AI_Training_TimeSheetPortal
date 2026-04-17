package com.tns.tms.shared.exception;

public class TmsException extends RuntimeException {
    public TmsException(String message) {
        super(message);
    }

    public TmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
