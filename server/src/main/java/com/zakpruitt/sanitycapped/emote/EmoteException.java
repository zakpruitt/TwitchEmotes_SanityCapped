package com.zakpruitt.sanitycapped.emote;

/**
 * Failures in the language of emotes; ApiErrorHandler decides their status.
 */
public abstract sealed class EmoteException extends RuntimeException {

    private EmoteException(String message) {
        super(message);
    }

    public static final class Invalid extends EmoteException {
        public Invalid(String message) {
            super(message);
        }
    }

    public static final class Duplicate extends EmoteException {
        public Duplicate(String message) {
            super(message);
        }
    }

    public static final class NotFound extends EmoteException {
        public NotFound(String message) {
            super(message);
        }
    }

    public static final class RateLimited extends EmoteException {
        public RateLimited(String message) {
            super(message);
        }
    }

    public static final class PublishingDisabled extends EmoteException {
        public PublishingDisabled(String message) {
            super(message);
        }
    }

    public static final class PublishFailed extends EmoteException {
        public PublishFailed(String message) {
            super(message);
        }
    }

    public static final class MissingImage extends EmoteException {
        public MissingImage(String message) {
            super(message);
        }
    }
}
