package com.zakpruitt.sanitycapped.web.exception;

import com.zakpruitt.sanitycapped.emote.exception.EmoteException;
import com.zakpruitt.sanitycapped.web.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
class ApiErrorHandler {


    @ExceptionHandler(EmoteException.class)
    ResponseEntity<ErrorResponse> handle(EmoteException e) {
        HttpStatus status = switch (e) {
            case EmoteException.Invalid ignored -> HttpStatus.BAD_REQUEST;
            case EmoteException.Duplicate ignored -> HttpStatus.CONFLICT;
            case EmoteException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case EmoteException.RateLimited ignored -> HttpStatus.TOO_MANY_REQUESTS;
            case EmoteException.PublishingDisabled ignored -> HttpStatus.SERVICE_UNAVAILABLE;
            case EmoteException.PublishFailed ignored -> HttpStatus.BAD_GATEWAY;
            case EmoteException.MissingImage ignored -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(NotAllowedException.class)
    ResponseEntity<ErrorResponse> handle(NotAllowedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Add your name so we know who to thank."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handle(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("That image is too big."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handle(Exception e) {
        log.error("Unhandled failure", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Something broke on the server."));
    }
}
