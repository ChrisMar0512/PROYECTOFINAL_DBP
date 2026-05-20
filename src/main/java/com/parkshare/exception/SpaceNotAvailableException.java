package com.parkshare.exception;

public class SpaceNotAvailableException extends RuntimeException {
    public SpaceNotAvailableException(String message) {
        super(message);
    }
}
