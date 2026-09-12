package gg.sanitycapped.emotes.emote;

/**
 * Everything that can go wrong with an emote, in the language of emotes rather
 * than of HTTP. The web layer decides what status each one deserves; the message
 * is written to be read by whoever is standing at the form.
 */
public abstract sealed class EmoteException extends RuntimeException {

    private EmoteException(String message) {
        super(message);
    }

    /** The upload cannot be accepted as it is: wrong type, no name, too big. */
    public static final class Invalid extends EmoteException {
        public Invalid(String message) {
            super(message);
        }
    }

    /** We already have this emote, by name or by image. */
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

    /** No GitHub token, so approving has nowhere to commit to. */
    public static final class PublishingDisabled extends EmoteException {
        public PublishingDisabled(String message) {
            super(message);
        }
    }

    /** GitHub refused the commit or the delete. */
    public static final class PublishFailed extends EmoteException {
        public PublishFailed(String message) {
            super(message);
        }
    }

    /** The row is here but its image is not; the volume lost it. */
    public static final class MissingImage extends EmoteException {
        public MissingImage(String message) {
            super(message);
        }
    }
}
