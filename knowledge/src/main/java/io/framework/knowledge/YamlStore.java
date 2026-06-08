package io.framework.knowledge;

import io.framework.core.exception.FrameworkException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tiny YAML-backed persistence for the learned stores (human-readable, git-friendly). */
final class YamlStore {

    private YamlStore() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> load(Path file) {
        if (file == null || !Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try (Reader r = Files.newBufferedReader(file)) {
            Object loaded = new Yaml().load(r);
            return loaded instanceof Map ? new LinkedHashMap<>((Map<String, Object>) loaded)
                    : new LinkedHashMap<>();
        } catch (IOException e) {
            throw new FrameworkException("Could not read knowledge file: " + file, e);
        }
    }

    static void save(Path file, Map<String, Object> data) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (Writer w = Files.newBufferedWriter(file)) {
                new Yaml().dump(data, w);
            }
        } catch (IOException e) {
            throw new FrameworkException("Could not write knowledge file: " + file, e);
        }
    }
}
