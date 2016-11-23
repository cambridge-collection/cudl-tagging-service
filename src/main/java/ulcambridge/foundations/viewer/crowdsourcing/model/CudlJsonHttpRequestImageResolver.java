package ulcambridge.foundations.viewer.crowdsourcing.model;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import io.jsonwebtoken.lang.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Optional;

public class CudlJsonHttpRequestImageResolver implements ImageResolver {


    private final URI imageServerBaseUrl;
    private final URI jsonBaseUrl;
    private final RestTemplate restTemplate;

    public CudlJsonHttpRequestImageResolver(
        URI imageServerBaseUrl, URI jsonBaseUrl,
        RestTemplate restTemplate) {

        Assert.notNull(imageServerBaseUrl);
        Assert.notNull(jsonBaseUrl);
        Assert.notNull(restTemplate);

        this.imageServerBaseUrl = imageServerBaseUrl;
        this.jsonBaseUrl = jsonBaseUrl;
        this.restTemplate = restTemplate;
    }

    private URI getImageUrl(String imagePath) {
        URI path;
        try {
            path = URI.create(UriUtils.encodePath(
                imagePath, Charsets.UTF_8.name()));
        }
        catch(UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        return imageServerBaseUrl.resolve(path);
    }

    private URI getJsonUrl(String documentId) {
        URI idPathSegment;
        try {
            idPathSegment = URI.create(UriUtils.encodePathSegment(
                documentId, Charsets.UTF_8.name()));
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        return jsonBaseUrl.resolve(idPathSegment);
    }

    @Override
    public String resolveImageUrl(String documentId, int pageNumber)
        throws ImageResolverException {

        ObjectNode node = restTemplate.getForObject(
            getJsonUrl(documentId), ObjectNode.class);

        String imagePath = Optional.of(node)
            .map(n -> n.get("pages"))
            .map(n -> n.get(pageNumber - 1))
            .map(n -> n.get("displayImageURL"))
            .map(n -> n.asText())
            .orElseThrow(() -> new ImageResolverException(
                String.format("Image for page %d not found in document: %s",
                              pageNumber, documentId)));

        return this.getImageUrl(imagePath).toString();
    }
}
