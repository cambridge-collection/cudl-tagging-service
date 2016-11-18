package ulcambridge.foundations.viewer.crowdsourcing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ulcambridge.foundations.viewer.crowdsourcing.model.TermCombiner;

@Configuration
public class CrowdsourcingConfig {

    @Bean
    public TermCombiner termCombiner(
        @Value("${cudl.tagging.weight.anno}") int annoWeight,
        @Value("${cudl.tagging.weight.removedtag}") int removedTagWeight,
        @Value("${cudl.tagging.weight.tag}") int tagWeight,
        @Value("${cudl.tagging.weight.meta}") int metaWeight) {

        return new TermCombiner(
            annoWeight, removedTagWeight, tagWeight, metaWeight);
    }
}
