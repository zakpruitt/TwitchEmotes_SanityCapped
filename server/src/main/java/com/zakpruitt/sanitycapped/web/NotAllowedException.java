package com.zakpruitt.sanitycapped.web;

public class NotAllowedException extends RuntimeException {

    public NotAllowedException(String message) {
        super(message);
    }
}
