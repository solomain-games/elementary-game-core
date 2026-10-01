package com.solomain.elementary.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmokeTest {

    @Test
    void buildAndTestInfrastructureWorks() {
        assertThat(Runtime.version().feature()).isGreaterThanOrEqualTo(25);
    }
}
