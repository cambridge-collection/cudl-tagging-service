package ulcambridge.foundations.viewer.crowdsourcing.springsec;

import com.google.common.collect.ImmutableList;
import org.springframework.util.Assert;
import ulcambridge.foundations.viewer.crowdsourcing.springsec.WwwAuthenticate.DefaultChallenge;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ChallengeGenerators {

    /**
     * Get a challenge generator which unconditionally returns a single
     * {@link WwwAuthenticate.Challenge} with the specified auth scheme and
     * without any parameters.
     */
    public static ChallengeGenerator singleSchemeWithoutParams(String authScheme) {
        Optional<Collection<WwwAuthenticate.Challenge>> challenge =
            Optional.of(Collections.singletonList(
                new DefaultChallenge(authScheme)));

        return (request, authException) -> challenge;
    }

    /**
     * Create a ChallengeGenerator which delegates to a sequence of sub
     * generators. Each is asked in turn to produce a collection of challenges,
     * and the first collection that's found wins.
     */
    public static ChallengeGenerator chainOf(
        Collection<ChallengeGenerator> chain) {

        List<ChallengeGenerator> links = ImmutableList.copyOf(chain);
        links.forEach(Assert::notNull);

        return (request, authException) ->
            links.stream()
                .map(cg -> cg.getChallenges(request, authException))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(challenges -> {
                    if(challenges.isEmpty())
                        throw new RuntimeException(
                            "ChallengeGenerator returned empty collection");
                })
                .findAny();
    }

    /**
     * @see #chainOf(Collection)
     */
    public static ChallengeGenerator chainOf(ChallengeGenerator...chain) {
        return chainOf(Arrays.asList(chain));
    }

    private ChallengeGenerators() {
        throw new RuntimeException();
    }
}
