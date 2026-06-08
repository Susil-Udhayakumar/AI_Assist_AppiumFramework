package io.framework.ai.heuristic;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TokenizerTest {

    @Test
    void splitsCamelCase() {
        assertThat(Tokenizer.tokens("loginButton")).containsExactly("login", "button");
    }

    @Test
    void splitsSeparators() {
        assertThat(Tokenizer.tokens("login_button-field")).containsExactly("login", "button", "field");
    }

    @Test
    void nullIsEmpty() {
        assertThat(Tokenizer.tokens(null)).isEmpty();
    }

    @Test
    void jaccardIdenticalIsOne() {
        assertThat(Tokenizer.jaccard(Set.of("login", "button"), Set.of("button", "login")))
                .isEqualTo(1.0);
    }

    @Test
    void jaccardDisjointIsZero() {
        assertThat(Tokenizer.jaccard(Set.of("login"), Set.of("settings"))).isZero();
    }
}
