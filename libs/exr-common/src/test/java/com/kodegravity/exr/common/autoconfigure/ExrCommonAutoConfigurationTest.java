package com.kodegravity.exr.common.autoconfigure;

import com.kodegravity.exr.common.web.CorrelationIdFilter;
import com.kodegravity.exr.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the auto-configuration's contract without starting a real application.
 * {@code ApplicationContextRunner} is the standard tool for this: it is fast and it
 * lets us assert the CONDITIONAL behaviour, which is where auto-configuration bugs hide.
 */
class ExrCommonAutoConfigurationTest {

    private final WebApplicationContextRunner servletRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExrCommonAutoConfiguration.class));

    private final ApplicationContextRunner nonWebRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExrCommonAutoConfiguration.class));

    @Test
    @DisplayName("a servlet web application gets the filter and the error handler")
    void registersBeansInServletApplication() {
        servletRunner.run(context -> {
            assertThat(context).hasSingleBean(CorrelationIdFilter.class);
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
        });
    }

    @Test
    @DisplayName("a non-web application gets nothing (no pointless filter)")
    void backsOffWhenNotAWebApplication() {
        nonWebRunner.run(context -> {
            assertThat(context).doesNotHaveBean(CorrelationIdFilter.class);
            assertThat(context).doesNotHaveBean(GlobalExceptionHandler.class);
        });
    }

    @Test
    @DisplayName("a service's own bean wins over the library default")
    void userBeanOverridesDefault() {
        servletRunner.withUserConfiguration(CustomFilterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(CorrelationIdFilter.class);
                    assertThat(context.getBean(CorrelationIdFilter.class))
                            .isSameAs(CustomFilterConfiguration.CUSTOM);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomFilterConfiguration {

        static final CorrelationIdFilter CUSTOM = new CorrelationIdFilter();

        @Bean
        CorrelationIdFilter myOwnFilter() {
            return CUSTOM;
        }
    }
}
