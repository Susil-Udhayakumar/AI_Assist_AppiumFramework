package io.framework.core.exception;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ElementNotFoundExceptionTest {

    @Test
    void messageListsTriedCandidates() {
        var ex = new ElementNotFoundException("loginButton",
                List.of("id=login", "accessibility-id=login_btn"));
        assertThat(ex).isInstanceOf(FrameworkException.class);
        assertThat(ex.elementName()).isEqualTo("loginButton");
        assertThat(ex.triedCandidates()).containsExactly("id=login", "accessibility-id=login_btn");
        assertThat(ex.getMessage()).contains("loginButton").contains("id=login");
    }
}
