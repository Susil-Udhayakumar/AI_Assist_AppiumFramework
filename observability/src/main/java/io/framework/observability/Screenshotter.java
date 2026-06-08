package io.framework.observability;

import io.framework.core.exception.FrameworkException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes a PNG screenshot from any {@link TakesScreenshot} source into a directory. */
public final class Screenshotter {

    public Path capture(TakesScreenshot source, Path dir, String name) {
        byte[] png = source.getScreenshotAs(OutputType.BYTES);
        Path file = dir.resolve(name.endsWith(".png") ? name : name + ".png");
        try {
            Files.write(file, png);
        } catch (IOException e) {
            throw new FrameworkException("Could not write screenshot: " + file, e);
        }
        return file;
    }
}
