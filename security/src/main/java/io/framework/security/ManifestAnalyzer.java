package io.framework.security;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Static audit of an AndroidManifest.xml: risky application flags and over-exposed components.
 * Parsed without namespace awareness, so attributes are read by their literal {@code android:}
 * prefixed names.
 */
public final class ManifestAnalyzer {

    private static final String[] COMPONENTS = {"activity", "service", "receiver", "provider"};

    public List<Finding> analyze(String manifestXml) {
        List<Finding> findings = new ArrayList<>();
        Document doc = parse(manifestXml);
        if (doc == null) {
            return findings;
        }

        NodeList apps = doc.getElementsByTagName("application");
        if (apps.getLength() > 0 && apps.item(0) instanceof Element app) {
            if (isTrue(app.getAttribute("android:debuggable"))) {
                findings.add(new Finding("MANIFEST_DEBUGGABLE", "Application is debuggable",
                        Severity.HIGH, "MASVS-RESILIENCE-1", "android:debuggable=\"true\"",
                        "Set android:debuggable=\"false\" (or remove it) for release builds."));
            }
            if (isTrue(app.getAttribute("android:allowBackup"))) {
                findings.add(new Finding("MANIFEST_ALLOW_BACKUP", "Application backups are allowed",
                        Severity.MEDIUM, "MASVS-STORAGE-2", "android:allowBackup=\"true\"",
                        "Set android:allowBackup=\"false\" to prevent adb backup of app data."));
            }
            if (isTrue(app.getAttribute("android:usesCleartextTraffic"))) {
                findings.add(new Finding("MANIFEST_CLEARTEXT", "Cleartext (HTTP) traffic permitted",
                        Severity.HIGH, "MASVS-NETWORK-1", "android:usesCleartextTraffic=\"true\"",
                        "Disable cleartext traffic and enforce HTTPS via a network security config."));
            }
        }

        for (String tag : COMPONENTS) {
            NodeList nodes = doc.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i) instanceof Element component
                        && isTrue(component.getAttribute("android:exported"))
                        && component.getAttribute("android:permission").isBlank()) {
                    String name = component.getAttribute("android:name");
                    findings.add(new Finding("MANIFEST_EXPORTED_" + tag.toUpperCase(),
                            "Exported " + tag + " without a permission",
                            Severity.MEDIUM, "MASVS-PLATFORM-1",
                            tag + " " + name + " android:exported=\"true\"",
                            "Set android:exported=\"false\" or guard it with a signature permission."));
                }
            }
        }
        return findings;
    }

    private static boolean isTrue(String value) {
        return "true".equalsIgnoreCase(value);
    }

    private static Document parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (Exception ignore) {
                // best effort
            }
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException e) {
                    // ignore
                }
            });
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception parseFailure) {
            return null;
        }
    }
}
