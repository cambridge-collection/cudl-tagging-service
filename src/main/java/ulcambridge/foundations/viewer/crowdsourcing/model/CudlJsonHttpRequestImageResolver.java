package ulcambridge.foundations.viewer.crowdsourcing.model;


import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jsonwebtoken.lang.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

public class CudlJsonHttpRequestImageResolver implements ImageResolver {


    private final String imageServerUrlTemplate;
    private final String jsonUrlTemplate;
    private final RestTemplate restTemplate;

    public CudlJsonHttpRequestImageResolver(
        String imageServerUrlTemplate, String jsonUrlTemplate,
        RestTemplate restTemplate) {

        Assert.hasText(imageServerUrlTemplate);
        Assert.hasText(jsonUrlTemplate);
        Assert.notNull(restTemplate);

        this.imageServerUrlTemplate = imageServerUrlTemplate;
        this.jsonUrlTemplate = jsonUrlTemplate;
        this.restTemplate = restTemplate;
    }

    private URI getImageUrl(String imagePath) {
        return UriComponentsBuilder.fromUriString(this.imageServerUrlTemplate)
            .buildAndExpand(imagePath)
            .encode()
            .toUri();
    }

    @Override
    public String resolveImageUrl(String documentId, int pageNumber)
        throws ImageResolverException {

        ObjectNode node = restTemplate.getForObject(
            this.jsonUrlTemplate, ObjectNode.class, documentId);

        String imagePath = Optional.of(node)
            .map(n -> n.get("pages"))
            .map(n -> n.get(pageNumber - 1))
            .map(n -> n.get("displayImageURL"))
            .map(n -> n.asText())
            .orElseThrow(() -> new ImageResolverException(
                String.format("Image for page %d not found in document: %s",
                              pageNumber, documentId)));

        return UriComponentsBuilder.fromUriString(this.imageServerUrlTemplate)
            .buildAndExpand(this.getImageUrl(imagePath))
            .encode()
            .toUriString();
    }
}
