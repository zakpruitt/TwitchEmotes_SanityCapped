package com.zakpruitt.sanitycapped.web.exception;

public class NotAllowedException extends RuntimeException {

    public NotAllowedException(String message) {
        super(message);
    }
}
