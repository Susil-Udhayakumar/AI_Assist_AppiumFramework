package io.framework.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CaptureLayoutTest {

    @Test
    void createsNestedRunDeviceTestDir(@TempDir Path base) {
        Path dir = new CaptureLayout(base).testDir("run1", "emulator-5554", "LoginTest");

        assertThat(Files.isDirectory(dir)).isTrue();
        assertThat(base.relativize(dir).toString().replace('\\', '/'))
                .isEqualTo("run1/emulator-5554/LoginTest");
    }

    @Test
    void sanitizesUnsafeCharacters() {
        assertThat(CaptureLayout.sanitize("a b/c:d")).isEqualTo("a_b_c_d");
    }
}
