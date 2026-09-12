package gg.sanitycapped.emotes.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import gg.sanitycapped.emotes.emote.EmoteException;

/**
 * Decides what each domain failure is worth in HTTP. Everything reaches the
 * pages as {"error": "..."}, which is the only shape they render.
 */
@RestControllerAdvice
class ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    record ApiError(String error) {
    }

    @ExceptionHandler(EmoteException.class)
    ResponseEntity<ApiError> handle(EmoteException e) {
        HttpStatus status = switch (e) {
            case EmoteException.Invalid ignored -> HttpStatus.BAD_REQUEST;
            case EmoteException.Duplicate ignored -> HttpStatus.CONFLICT;
            case EmoteException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case EmoteException.RateLimited ignored -> HttpStatus.TOO_MANY_REQUESTS;
            case EmoteException.PublishingDisabled ignored -> HttpStatus.SERVICE_UNAVAILABLE;
            case EmoteException.PublishFailed ignored -> HttpStatus.BAD_GATEWAY;
            case EmoteException.MissingImage ignored -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(NotAllowedException.class)
    ResponseEntity<ApiError> handle(NotAllowedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handle(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(new ApiError("That image is too big."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handle(Exception e) {
        log.error("Unhandled failure", e);
        return ResponseEntity.internalServerError().body(new ApiError("Something broke on the server."));
    }
}
