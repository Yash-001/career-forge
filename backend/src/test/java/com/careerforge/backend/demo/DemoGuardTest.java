package com.careerforge.backend.demo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class DemoGuardTest {

    @Test
    void demoMode_false_productionEnv_doesNotThrow() {
        DemoGuard guard = new DemoGuard(false, "production");
        assertThatNoException().isThrownBy(guard::enforce);
    }

    @Test
    void demoMode_false_emptyEnv_doesNotThrow() {
        DemoGuard guard = new DemoGuard(false, "");
        assertThatNoException().isThrownBy(guard::enforce);
    }

    @Test
    void demoMode_true_emptyEnv_doesNotThrow() {
        DemoGuard guard = new DemoGuard(true, "");
        assertThatNoException().isThrownBy(guard::enforce);
    }

    @Test
    void demoMode_true_developmentEnv_doesNotThrow() {
        DemoGuard guard = new DemoGuard(true, "development");
        assertThatNoException().isThrownBy(guard::enforce);
    }

    @Test
    void demoMode_true_productionEnv_throwsIllegalState() {
        DemoGuard guard = new DemoGuard(true, "production");
        assertThatThrownBy(guard::enforce)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CAREERFORGE_DEMO_MODE=true");
    }

    @Test
    void demoMode_true_productionEnv_caseInsensitive_throws() {
        DemoGuard guard = new DemoGuard(true, "PRODUCTION");
        assertThatThrownBy(guard::enforce)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isDemoMode_reflectsConstructorArg() {
        assertThatNoException().isThrownBy(() -> {
            DemoGuard on = new DemoGuard(true, "");
            assert on.isDemoMode();
            DemoGuard off = new DemoGuard(false, "");
            assert !off.isDemoMode();
        });
    }
}
