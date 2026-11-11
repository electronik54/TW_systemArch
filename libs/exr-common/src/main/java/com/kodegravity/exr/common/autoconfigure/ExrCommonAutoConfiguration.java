package com.kodegravity.exr.common.autoconfigure;

import com.kodegravity.exr.common.web.CorrelationIdFilter;
import com.kodegravity.exr.common.web.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Wires the EXR common web plumbing into any service that has this library on its
 * classpath - so no service has to remember to register it.
 *
 * <p>Why the conditions matter:
 * <ul>
 *   <li>{@code @ConditionalOnWebApplication(SERVLET)} - a batch job or a Kafka
 *       consumer has no servlet stack; a filter there is meaningless and would
 *       only confuse the startup log.</li>
 *   <li>{@code @ConditionalOnMissingBean} - a service that needs different
 *       behaviour can define its own bean and silently win. That is the polite
 *       way for a library to hold an opinion.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ExrCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
