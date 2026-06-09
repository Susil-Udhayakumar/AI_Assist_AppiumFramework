package io.framework.reporting;

import io.framework.core.exception.FrameworkException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes the traceability matrix as rtm.html: one row per requirement with its coverage state. */
public final class RtmHtmlWriter {

    public Path write(List<RequirementCoverage> matrix, Path outputDir) {
        long covered = matrix.stream().filter(c -> c.status() != CoverageStatus.NOT_COVERED).count();
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>Traceability Matrix</title>")
                .append("<style>body{font-family:sans-serif}table{border-collapse:collapse}")
                .append("td,th{border:1px solid #ccc;padding:4px 8px}")
                .append(".COVERED_PASSING{color:#070}.COVERED_FAILING{color:#b00}.NOT_COVERED{color:#a60}</style>")
                .append("</head><body><h1>Requirement Traceability Matrix</h1>")
                .append("<p>Requirements: ").append(matrix.size())
                .append(" &middot; Covered: ").append(covered)
                .append(" &middot; Not covered: ").append(matrix.size() - covered).append("</p>")
                .append("<table><tr><th>Requirement</th><th>Status</th><th>Tests</th></tr>");
        for (RequirementCoverage c : matrix) {
            html.append("<tr><td>").append(esc(c.requirementId()))
                    .append("</td><td class=\"").append(c.status()).append("\">").append(c.status())
                    .append("</td><td>").append(esc(String.join(", ", c.coveringTests())))
                    .append("</td></tr>");
        }
        html.append("</table></body></html>");

        try {
            Files.createDirectories(outputDir);
            Path file = outputDir.resolve("rtm.html");
            Files.writeString(file, html.toString());
            return file;
        } catch (IOException e) {
            throw new FrameworkException("Could not write rtm.html to " + outputDir, e);
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
