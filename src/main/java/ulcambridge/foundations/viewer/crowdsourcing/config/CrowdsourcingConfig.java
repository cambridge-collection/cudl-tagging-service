package ulcambridge.foundations.viewer.crowdsourcing.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ulcambridge.foundations.viewer.crowdsourcing.model.CudlJsonHttpRequestImageResolver;
import ulcambridge.foundations.viewer.crowdsourcing.model.ImageResolver;
import ulcambridge.foundations.viewer.crowdsourcing.model.Term;
import ulcambridge.foundations.viewer.crowdsourcing.model.TermCombiner;
import ulcambridge.foundations.viewer.crowdsourcing.model.TermCombiners;
import ulcambridge.foundations.viewer.crowdsourcing.model.TermType;
import ulcambridge.foundations.viewer.crowdsourcing.model.Terms;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class CrowdsourcingConfig {

    @Bean
    public Map<TermType, Double> termTypeWeights(
        @Value("${cudl.tagging.weight.anno}") double annoWeight,
        @Value("${cudl.tagging.weight.removedtag}") double removedTagWeight,
        @Value("${cudl.tagging.weight.tag}") double tagWeight) {

        EnumMap<TermType, Double> weights = new EnumMap<>(TermType.class);
        weights.put(TermType.ANNOTATION, annoWeight);
        weights.put(TermType.REMOVED_TAG, removedTagWeight);
        weights.put(TermType.TAG, tagWeight);

        return Collections.unmodifiableMap(weights);
    }

    @Bean
    public TermCombiner.Factory<
        TermType, Term, Term, Collection<Term>> weightedTermCombiner(
        @Qualifier("termTypeWeights") Map<TermType, Double> weights) {

        return TermCombiners.weightedToMap(weights)
            .postProcessedBy(terms -> terms.values().stream()
                .filter(Terms::hasPositiveValue)
                .collect(Collectors.toList()));
    }

    @Bean
    public ImageResolver imageResolver(
        @Value("${cudl.imageserver-base-url}") URI imageserverBaseUrl,
        @Value("${cudl.json-base-url}") URI jsonBaseUrl,
        RestTemplate restTemplate) {

        return new CudlJsonHttpRequestImageResolver(
            imageserverBaseUrl, jsonBaseUrl, restTemplate);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
