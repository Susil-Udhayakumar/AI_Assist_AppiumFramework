package io.framework.ai.heuristic;

import io.framework.locators.ElementHealer;
import io.framework.locators.HealRequest;
import io.framework.locators.LocatorCandidate;
import io.framework.locators.Strategy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic, offline {@link ElementHealer}. When every registered locator candidate misses,
 * it parses the page source and finds the on-screen element whose accessibility-id / resource-id
 * / text best matches the (tokenized) element name, then returns a {@link LocatorCandidate} for
 * it. No model, no network — this is the no-AI heal path.
 */
public final class HeuristicElementHealer implements ElementHealer {

    /** Minimum token-similarity to accept a heal (avoids confident-but-wrong matches). */
    private static final double THRESHOLD = 0.5;

    @Override
    public Optional<LocatorCandidate> heal(HealRequest request) {
        String xml = request.pageSource();
        if (xml == null || xml.isBlank()) {
            return Optional.empty();
        }
        Set<String> target = Tokenizer.tokens(request.elementName());
        if (target.isEmpty()) {
            return Optional.empty();
        }

        double bestScore = 0.0;
        LocatorCandidate best = null;
        for (Element e : elements(xml)) {
            for (Attr attr : attributes(e)) {
                double score = Tokenizer.jaccard(target, attr.tokens());
                if (score > bestScore) {
                    bestScore = score;
                    best = attr.candidate();
                }
            }
        }
        return bestScore >= THRESHOLD ? Optional.ofNullable(best) : Optional.empty();
    }

    private record Attr(LocatorCandidate candidate, Set<String> tokens) {
    }

    private static java.util.List<Attr> attributes(Element e) {
        java.util.List<Attr> attrs = new java.util.ArrayList<>();
        String acc = firstNonBlank(e.getAttribute("content-desc"), e.getAttribute("name"));
        String rid = e.getAttribute("resource-id");
        String text = firstNonBlank(e.getAttribute("text"), e.getAttribute("label"),
                e.getAttribute("value"));
        if (!acc.isBlank()) {
            attrs.add(new Attr(new LocatorCandidate(Strategy.ACCESSIBILITY_ID, acc), Tokenizer.tokens(acc)));
        }
        if (!rid.isBlank()) {
            attrs.add(new Attr(new LocatorCandidate(Strategy.ID, rid), Tokenizer.tokens(localId(rid))));
        }
        if (!text.isBlank()) {
            attrs.add(new Attr(new LocatorCandidate(Strategy.XPATH, "//*[@text='" + text + "']"),
                    Tokenizer.tokens(text)));
        }
        return attrs;
    }

    private static java.util.List<Element> elements(String xml) {
        java.util.List<Element> result = new java.util.ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            silence(factory);
            DocumentBuilder builder = factory.newDocumentBuilder();
            // swallow the parser's default stderr reporting; we fail closed to Optional.empty()
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException e) {
                    // ignore
                }
            });
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            NodeList all = doc.getElementsByTagName("*");
            for (int i = 0; i < all.getLength(); i++) {
                if (all.item(i) instanceof Element el) {
                    result.add(el);
                }
            }
        } catch (Exception parseFailure) {
            return java.util.List.of();
        }
        return result;
    }

    private static void silence(DocumentBuilderFactory factory) {
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignore) {
            // best-effort hardening; continue if the parser does not support the feature
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignore) {
            // best-effort
        }
        factory.setExpandEntityReferences(false);
    }

    private static String localId(String resourceId) {
        int cut = Math.max(resourceId.lastIndexOf('/'), resourceId.lastIndexOf(':'));
        return cut >= 0 ? resourceId.substring(cut + 1) : resourceId;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
