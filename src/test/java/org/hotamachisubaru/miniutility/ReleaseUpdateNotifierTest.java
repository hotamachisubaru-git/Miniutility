package org.hotamachisubaru.miniutility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseUpdateNotifierTest {
    @ParameterizedTest
    @CsvSource({
            "26.3,26.2,true",
            "26.2,26.3,false",
            "26.3,26.3,false",
            "v26.3,26.3-beta.2,true",
            "26.3-beta2,26.3-beta1,true",
            "26.3-rc2,26.3-rc1,true",
            "26.3-beta.2,26.3-alpha.10,true",
            "26.3-alpha.2,26.3-beta.1,false",
            "26.3.1,26.3,true",
            "invalid,26.3,false"
    })
    void comparesVersionsAndPrereleaseRevisions(String candidate, String current, boolean newer) {
        assertEquals(newer, ReleaseUpdateNotifier.isNewerVersion(candidate, current));
    }

    @Test
    void readsReleaseFieldsFromRootObjectAndDecodesEscapes() {
        var release = ReleaseUpdateNotifier.parseRelease("""
                {"author":{"html_url":"https://github.com/author"},
                 "tag_name":"v26.3", "html_url":"https:\\/\\/github.com\\/releases\\/26.3"}
                """);
        assertNotNull(release);
        assertEquals("v26.3", release.tag());
        assertEquals("https://github.com/releases/26.3", release.url());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "{", "{}",
            "{\"tag_name\":false,\"html_url\":\"https://github.com/release\"}",
            "{\"tag_name\":\"26.3\",\"html_url\":null}"})
    void ignoresMalformedOrIncompleteReleaseResponses(String body) {
        assertNull(ReleaseUpdateNotifier.parseRelease(body));
    }
}
