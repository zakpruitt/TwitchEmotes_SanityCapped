package com.zakpruitt.sanitycapped;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestSounds {

    /** MPEG1 Layer III, 128 kbps, 44.1 kHz: 417 bytes and 1152 samples a frame. */
    private static final byte[] HEADER = {(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x00};
    private static final int FRAME_BYTES = 417;
    private static final double FRAME_SECONDS = 1152 / 44100.0;

    static MockMultipartFile mp3(double seconds, int seed) {
        return new MockMultipartFile("file", "sound.mp3", "audio/mpeg", bytes(seconds, seed));
    }

    /** Silent but well-formed frames; the seed makes two files differ. */
    static byte[] bytes(double seconds, int seed) {
        int frames = (int) Math.ceil(seconds / FRAME_SECONDS);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < frames; i++) {
            out.writeBytes(HEADER);
            byte[] payload = new byte[FRAME_BYTES - HEADER.length];
            payload[0] = (byte) seed;
            out.writeBytes(payload);
        }
        return out.toByteArray();
    }

    /** The same audio behind an ID3v2 tag, as most downloaded MP3s have. */
    static byte[] tagged(double seconds, int seed) {
        byte[] tagBody = new byte[300];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{'I', 'D', '3', 4, 0, 0, 0, 0, (byte) (tagBody.length >> 7), (byte) (tagBody.length & 0x7F)});
        out.writeBytes(tagBody);
        out.writeBytes(bytes(seconds, seed));
        return out.toByteArray();
    }
}
