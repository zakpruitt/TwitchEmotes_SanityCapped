package com.zakpruitt.sanitycapped.audio;

import com.zakpruitt.sanitycapped.audio.dto.AudioInfo;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reads an upload as MPEG Layer III frames, rather than trusting its name or
 * content type. Walking every frame also gives the length exactly, whatever
 * the bitrate does. Mirrored by duration() in tools/build_sounds.py.
 */
@Component
public class Mp3Inspector {

    /** How far past the start (or an ID3 tag) the first frame may sit. */
    private static final int SYNC_SEARCH_BYTES = 4096;

    private static final int[] BITRATES_V1 = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};
    private static final int[] BITRATES_V2 = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160};
    private static final int[] RATES_V1 = {44100, 48000, 32000};
    private static final int[] RATES_V2 = {22050, 24000, 16000};
    private static final int[] RATES_V25 = {11025, 12000, 8000};

    /** Empty when the bytes are not an MP3. */
    public Optional<AudioInfo> inspect(byte[] data) {
        int position = firstFrame(data);
        if (position < 0) {
            return Optional.empty();
        }

        double seconds = 0;
        for (Frame frame = Frame.at(data, position); frame != null; frame = Frame.at(data, position)) {
            seconds += frame.seconds();
            position += frame.length();
        }
        return Optional.of(new AudioInfo(Math.round(seconds * 1000)));
    }

    /**
     * The first sync has to lead to a second frame (or the end of the file), or
     * any two bytes that happen to look like a header would pass.
     */
    private static int firstFrame(byte[] data) {
        int start = audioStart(data);
        int limit = Math.min(data.length, start + SYNC_SEARCH_BYTES);

        for (int i = start; i < limit; i++) {
            Frame frame = Frame.at(data, i);
            if (frame != null && (i + frame.length() >= data.length || Frame.at(data, i + frame.length()) != null)) {
                return i;
            }
        }
        return -1;
    }

    /** Skips a leading ID3v2 tag, whose size is four 7-bit bytes. */
    private static int audioStart(byte[] data) {
        if (data.length < 10 || data[0] != 'I' || data[1] != 'D' || data[2] != '3') {
            return 0;
        }
        int size = (data[6] & 0x7F) << 21 | (data[7] & 0x7F) << 14 | (data[8] & 0x7F) << 7 | (data[9] & 0x7F);
        boolean footer = (data[5] & 0x10) != 0;
        return 10 + size + (footer ? 10 : 0);
    }

    private record Frame(int length, double seconds) {

        /** Null when there is no valid Layer III header at this offset. */
        static Frame at(byte[] data, int i) {
            if (i < 0 || i + 4 > data.length || (data[i] & 0xFF) != 0xFF || (data[i + 1] & 0xE0) != 0xE0) {
                return null;
            }
            int version = (data[i + 1] >> 3) & 3;   // 3 = MPEG1, 2 = MPEG2, 0 = MPEG2.5
            int layer = (data[i + 1] >> 1) & 3;     // 1 = Layer III
            int bitrateIndex = (data[i + 2] >> 4) & 0xF;
            int rateIndex = (data[i + 2] >> 2) & 3;
            int padding = (data[i + 2] >> 1) & 1;
            if (version == 1 || layer != 1 || bitrateIndex == 0 || bitrateIndex == 15 || rateIndex == 3) {
                return null;
            }

            boolean mpeg1 = version == 3;
            int bitrate = (mpeg1 ? BITRATES_V1 : BITRATES_V2)[bitrateIndex] * 1000;
            int rate = (mpeg1 ? RATES_V1 : version == 2 ? RATES_V2 : RATES_V25)[rateIndex];
            int length = (mpeg1 ? 144 : 72) * bitrate / rate + padding;
            return new Frame(length, (mpeg1 ? 1152.0 : 576.0) / rate);
        }
    }
}
