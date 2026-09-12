package gg.sanitycapped.emotes.emote;

/**
 * What the upload form learns while someone is still filling it in.
 *
 * @param name      the trigger word their typing would produce
 * @param nameTaken status of the emote already holding that name, if any
 * @param exact     name of the emote that is byte-for-byte this image, if any
 */
public record DuplicateCheck(String name, EmoteStatus nameTaken, String exact) {
}
