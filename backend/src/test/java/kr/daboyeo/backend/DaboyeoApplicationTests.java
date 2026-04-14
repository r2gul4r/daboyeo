package kr.daboyeo.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DaboyeoApplicationTests {

    @Test
    void projectNameIsStable() {
        assertThat("daboyeo-backend").startsWith("daboyeo");
    }
}
