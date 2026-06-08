package io.framework.devices;

import io.framework.core.exception.FrameworkException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Default {@link CommandRunner}: executes via {@link ProcessBuilder}, combined output, 30s cap. */
public final class ProcessCommandRunner implements CommandRunner {

    @Override
    public String run(String... command) {
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor(30, TimeUnit.SECONDS);
            return out;
        } catch (IOException e) {
            throw new FrameworkException("Command failed: " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrameworkException("Command interrupted: " + String.join(" ", command), e);
        }
    }
}
