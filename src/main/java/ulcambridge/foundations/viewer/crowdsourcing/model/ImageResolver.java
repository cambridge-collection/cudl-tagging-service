package ulcambridge.foundations.viewer.crowdsourcing.model;


public interface ImageResolver {
    String resolveImageUrl(String documentId, int pageIndex)
        throws ImageResolverException;
}
