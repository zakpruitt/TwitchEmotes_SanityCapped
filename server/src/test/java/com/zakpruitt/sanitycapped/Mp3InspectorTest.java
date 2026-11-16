package com.zakpruitt.sanitycapped;

import com.zakpruitt.sanitycapped.audio.Mp3Inspector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class Mp3InspectorTest {

    private final Mp3Inspector inspector = new Mp3Inspector();

    @Test
    void theLengthComesFromCountingFrames() {
        assertThat(inspector.inspect(TestSounds.bytes(3, 1)))
                .hasValueSatisfying(info -> assertThat(info.seconds()).isCloseTo(3.0, within(0.05)));
    }

    @Test
    void anId3TagInFrontIsSkipped() {
        assertThat(inspector.inspect(TestSounds.tagged(2, 1)))
                .hasValueSatisfying(info -> assertThat(info.seconds()).isCloseTo(2.0, within(0.05)));
    }

    @Test
    void bytesThatAreNotAnMp3AreRefused() {
        assertThat(inspector.inspect("definitely not audio".getBytes())).isEmpty();
        assertThat(inspector.inspect(TestImages.bytes(java.awt.Color.RED))).isEmpty();
        assertThat(inspector.inspect(new byte[0])).isEmpty();
    }

    @Test
    void oneStrayHeaderIsNotEnough() {
        byte[] noise = new byte[2000];
        noise[100] = (byte) 0xFF;
        noise[101] = (byte) 0xFB;
        noise[102] = (byte) 0x90;

        assertThat(inspector.inspect(noise)).isEmpty();
    }
}
