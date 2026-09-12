package gg.sanitycapped.emotes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import gg.sanitycapped.emotes.core.Naming;

/**
 * The expected values come from running sanitize()/apply_prefix() in
 * tools/build_emotes.py. If this drifts, the site promises trigger words the
 * addon build won't produce.
 */
class NamingTest {

    @ParameterizedTest
    @CsvSource({
            "cloudzUlt-128, scCloudzUlt",
            "FUCK-128, scFuck",
            "VynChatting-128, scVynChatting",
            "catJAM_2x, scCatJAM",
            "peepoHmm-128, scPeepoHmm",
            "pepe-laugh, scPepelaugh",
            "OOOO-128, scOooo",
            "scAlready, scAlready",
            "imIn, scImIn",
            "theChosen, scTheChosen",
            "weAreGathered, scWeAreGathered",
            "sadge_512, scSadge",
            "a, scA",
            "MiXeD, scMiXeD",
            "he''llo, scHello",
            "what?now, scWhatnow",
            "dot.dot, scDotdot",
            "big-3x, scBig",
            "x_1234px, scX",
            "scoop, scScoop",
    })
    void matchesTheBuildScript(String stem, String expected) {
        assertThat(Naming.triggerWord(stem)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"'  '", "'---'", "'?!.'", "''"})
    void namesMadeOnlyOfBreakCharactersLeaveNothing(String stem) {
        assertThat(Naming.triggerWord(stem)).isEmpty();
    }
}
