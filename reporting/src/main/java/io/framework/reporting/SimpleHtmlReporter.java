package io.framework.reporting;

import io.framework.core.exception.FrameworkException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Built-in self-contained HTML reporter: a summary line + a results table in report.html. */
public final class SimpleHtmlReporter implements Reporter {

    @Override
    public String name() {
        return "html";
    }

    @Override
    public void report(List<TestResult> results, Path outputDir) {
        ResultSummary s = ResultSummary.of(results);
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset=\"utf-8\"><title>Test Report</title>")
                .append("<style>body{font-family:sans-serif}table{border-collapse:collapse}")
                .append("td,th{border:1px solid #ccc;padding:4px 8px}.FAIL{color:#b00}.PASS{color:#070}</style>")
                .append("</head><body>")
                .append("<h1>Test Report</h1>")
                .append("<p>Total: ").append(s.total())
                .append(" &middot; Passed: ").append(s.passed())
                .append(" &middot; Failed: ").append(s.failed())
                .append(" &middot; Skipped: ").append(s.skipped())
                .append(" &middot; Duration(ms): ").append(s.durationMs())
                .append("</p>")
                .append("<table><tr><th>Test</th><th>Status</th><th>Device</th><th>ms</th><th>Message</th></tr>");
        for (TestResult r : results) {
            html.append("<tr><td>").append(esc(r.name()))
                    .append("</td><td class=\"").append(r.status()).append("\">").append(r.status())
                    .append("</td><td>").append(esc(r.device()))
                    .append("</td><td>").append(r.durationMs())
                    .append("</td><td>").append(esc(r.message() == null ? "" : r.message()))
                    .append("</td></tr>");
        }
        html.append("</table></body></html>");

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve("report.html"), html.toString());
        } catch (IOException e) {
            throw new FrameworkException("Could not write report.html to " + outputDir, e);
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
