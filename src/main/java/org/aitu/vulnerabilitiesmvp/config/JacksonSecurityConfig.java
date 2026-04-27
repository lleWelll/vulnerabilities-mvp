package org.aitu.vulnerabilitiesmvp.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonSecurityConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonSecurityCustomizer(AppProperties appProperties) {
        return builder -> builder.postConfigurer(objectMapper -> objectMapper.getFactory().setStreamReadConstraints(
            StreamReadConstraints.builder()
                .maxDocumentLength(appProperties.getSecurity().getMaxRequestSizeBytes())
                .maxStringLength(appProperties.getSecurity().getMaxJsonStringLength())
                .maxNestingDepth(appProperties.getSecurity().getMaxJsonNestingDepth())
                .maxNumberLength(appProperties.getSecurity().getMaxJsonNumberLength())
                .build()
        ));
    }
}
