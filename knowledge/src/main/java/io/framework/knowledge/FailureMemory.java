package io.framework.knowledge;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fingerprints failures (normalizing out volatile numbers/addresses) so recurring failures are
 * auto-classified by lookup before any AI runs, and known ones link to an existing defect
 * instead of filing duplicates.
 */
public final class FailureMemory {

    private final Path file;
    private final Map<String, FailureRecord> records = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public FailureMemory(Path baseDir) {
        this.file = baseDir.resolve("learned").resolve("failures.yaml");
        for (Map.Entry<String, Object> e : YamlStore.load(file).entrySet()) {
            Map<String, Object> v = (Map<String, Object>) e.getValue();
            records.put(e.getKey(), new FailureRecord(
                    String.valueOf(v.getOrDefault("classification", "")),
                    v.get("defectId") == null ? null : String.valueOf(v.get("defectId"))));
        }
    }

    /** Stable fingerprint that groups failures differing only in numbers/hex/whitespace. */
    public static String fingerprint(String failureText) {
        String norm = (failureText == null ? "" : failureText)
                .toLowerCase()
                .replaceAll("0x[0-9a-f]+", "#")
                .replaceAll("\\d+", "#")
                .replaceAll("\\s+", " ")
                .trim();
        return Hashes.sha1(norm);
    }

    public Optional<FailureRecord> lookup(String fingerprint) {
        return Optional.ofNullable(records.get(fingerprint));
    }

    public void record(String fingerprint, String classification, String defectId) {
        records.put(fingerprint, new FailureRecord(classification, defectId));
        save();
    }

    public int size() {
        return records.size();
    }

    private void save() {
        Map<String, Object> out = new LinkedHashMap<>();
        records.forEach((k, r) -> {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("classification", r.classification());
            v.put("defectId", r.defectId());
            out.put(k, v);
        });
        YamlStore.save(file, out);
    }
}
