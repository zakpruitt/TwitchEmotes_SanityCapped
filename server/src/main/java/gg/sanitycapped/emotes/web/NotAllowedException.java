package gg.sanitycapped.emotes.web;

/** Wrong passcode, or the right one for the wrong job. */
public class NotAllowedException extends RuntimeException {

    public NotAllowedException(String message) {
        super(message);
    }
}
