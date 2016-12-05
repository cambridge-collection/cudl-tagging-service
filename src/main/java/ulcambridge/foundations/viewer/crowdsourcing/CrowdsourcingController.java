package ulcambridge.foundations.viewer.crowdsourcing;

import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import ulcambridge.foundations.viewer.crowdsourcing.dao.CrowdsourcingDao;
import ulcambridge.foundations.viewer.crowdsourcing.dao.CrowdsourcingDao.UpsertResult;
import ulcambridge.foundations.viewer.crowdsourcing.model.Annotation;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentAnnotations;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentTags;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentTerms;
import ulcambridge.foundations.viewer.crowdsourcing.model.ImageResolver;
import ulcambridge.foundations.viewer.crowdsourcing.model.ImageResolverException;
import ulcambridge.foundations.viewer.crowdsourcing.model.Tag;
import ulcambridge.foundations.viewer.crowdsourcing.model.TermCombiner;
import ulcambridge.foundations.viewer.crowdsourcing.model.UserAnnotations;
import ulcambridge.foundations.viewer.rdf.RDFReader;
import ulcambridge.foundations.viewer.utils.Utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles requests for the crowdsourcing platform.
 *
 * @author Lei
 *
 */
@RestController
@CrossOrigin(allowCredentials = "false")
@RequestMapping("/crowdsourcing")
public class CrowdsourcingController {

    private static final Logger logger = LoggerFactory.getLogger(CrowdsourcingController.class);
    private static final String MEDIA_RDF = "application/rdf+xml";

    private final CrowdsourcingDao dataSource;
    private final TermCombiner termCombiner;
    private final ImageResolver imageResolver;

    @Autowired
    public CrowdsourcingController(
        CrowdsourcingDao crowdsourcingDao, TermCombiner termCombiner,
        ImageResolver imageResolver) {

        Assert.notNull(crowdsourcingDao);
        Assert.notNull(termCombiner);
        Assert.notNull(imageResolver);

        this.dataSource = crowdsourcingDao;
        this.termCombiner = termCombiner;
        this.imageResolver = imageResolver;
    }

    private static final CacheControl CACHE_PRIVATE = CacheControl.noCache();
    private static final CacheControl CACHE_PUBLIC_INFREQUENTLY_CHANGING =
            CacheControl.empty()
                .cachePublic()
                .sMaxAge(30, TimeUnit.MINUTES);

    private static class PermissionDeniedException extends RuntimeException { }

    @ExceptionHandler(PermissionDeniedException.class)
    private ResponseEntity<Void> handlePermissionDenied(PermissionDeniedException e ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(SQLException.class)
    private ResponseEntity<Void> handleSqlException(SQLException e) {
        logger.error("Database operation failed", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    // on path /anno/get
    @RequestMapping(value = "/anno/{docId}/{docPage}",
                    method = RequestMethod.GET,
                    produces = { "application/json" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentAnnotations> handleAnnotationsFetch(
        @PathVariable("docId") String documentId,
        @PathVariable("docPage") int documentPageNo) throws IOException {

        DocumentAnnotations docAnnotations = dataSource.getAnnotations(
            getCurrentUserId(), documentId, documentPageNo);

        return ResponseEntity.status(HttpStatus.OK)
                .cacheControl(CACHE_PRIVATE)
                .body(docAnnotations);
    }

    @RequestMapping(
        value = "/anno/{docId}",
        method = RequestMethod.POST,
        headers = { "Content-type=application/json" },
        consumes = { "application/json" }, produces = { "application/json" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Annotation> handleAnnotationAdd(
        @PathVariable("docId") String documentId,
        @RequestBody Annotation annotation) throws SQLException, IOException {

        URI annotationLocation = UriComponentsBuilder
            .fromUriString("./{docId}/{docPage}")
            .buildAndExpand(documentId, annotation.getPage()).encode().toUri();

        return ResponseEntity.status(HttpStatus.CREATED)
            .location(annotationLocation)
            .body(dataSource.addAnnotation(
                getCurrentUserId(), documentId, annotation));
    }

    @RequestMapping(value = "/anno/{docId}/{uuid}",
                    method = RequestMethod.DELETE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> handleAnnotationRemove(
            @PathVariable("docId") String documentId,
            @PathVariable("uuid") UUID annotationId)
            throws SQLException, IOException {

        boolean removed = dataSource.removeAnnotation(
            getCurrentUserId(), documentId, annotationId);

        return (removed ? ResponseEntity.noContent()
                        : ResponseEntity.notFound()).build();
    }

    /**
     * Delete the annotations created by the logged-in user with the specified
     * IDs from a document.
     */
    @RequestMapping(value = "/anno/{docId}",
                    method = RequestMethod.DELETE,
                    consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Set<UUID>> removeAnnotations(
            @PathVariable("docId") String documentId,
            @RequestParam("uuid") List<UUID> annotationIds)
            throws SQLException, IOException {

        Set<UUID> removed = dataSource.removeAnnotations(
                getCurrentUserId(), documentId, annotationIds);

        return ResponseEntity.ok().body(removed);
    }

    // on path /tag/get
    @RequestMapping(value = "/tag/{docId}",
                    method = RequestMethod.GET,
                    produces = { "application/json" })
    public ResponseEntity<DocumentTerms> handleTagsFetch(
        @PathVariable("docId") String documentId) throws IOException {

        // combine tags with annotations and removed tags
        DocumentTags docTags = dataSource.getTagsByDocument(documentId);
        DocumentTags docRemovedTags = dataSource.getRemovedTagsByDocument(documentId);
        DocumentAnnotations docAnnotations = dataSource.getAnnotationsByDocument(documentId);

        DocumentTerms docARTTerms = termCombiner.combine_Anno_Tag_RemovedTag(
            docAnnotations, docTags, docRemovedTags);

        return ResponseEntity.ok()
                .cacheControl(CACHE_PUBLIC_INFREQUENTLY_CHANGING)
                .body(docARTTerms);
    }

    // on path /rmvtag/get
    @RequestMapping(value = "/rmvtag/{docId}",
                    method = RequestMethod.GET,
                    produces = { "application/json" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentTags> handleRemovedTagsFetch(
        @PathVariable("docId") String documentId) throws IOException {

        DocumentTags docTags = dataSource.getRemovedTags(
            getCurrentUserId(), documentId);

        return ResponseEntity.ok()
                .cacheControl(CACHE_PRIVATE)
                .body(docTags);
    }

    @RequestMapping(value = "/rmvtag/{docId}/{tag}",
        method = RequestMethod.GET,
        produces = { "application/json" })
    @PreAuthorize("isAuthenticated()")
    public Tag handleRemovedTagsFetch(
        @PathVariable("docId") String documentId,
        @PathVariable("tag") String tag) throws IOException {

        return dataSource.getRemovedTag(getCurrentUserId(), documentId, tag);
    }

    @RequestMapping(value = "/rmvtag/{docId}",
        method = RequestMethod.POST,
        headers = { "Content-type=application/json" },
        consumes = { "application/json" }, produces = { "application/json" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentTags> handleRemovedTagAddOrUpdate(
        @PathVariable("docId") String documentId, @RequestBody Tag removedTag)
        throws SQLException, IOException {

        UpsertResult<DocumentTags> dt = dataSource.addRemovedTag(
            getCurrentUserId(), documentId, removedTag);

        // 201 if new, 200 if updated
        return ResponseEntity
            .status(dt.wasCreated() ? HttpStatus.CREATED : HttpStatus.OK)
            .location(
                UriComponentsBuilder.fromUriString("./{docId}/{tag}")
                    .buildAndExpand(documentId, removedTag.getName())
                    .encode().toUri())
            .body(dt.getValue());
    }

    @RequestMapping(value = "/rmvtag/{docId}/{tag}",
                    method = RequestMethod.DELETE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> handleRemovedTagDelete(
        @PathVariable("docId") String documentId,
        @PathVariable("tag") String tagName) throws SQLException {

        boolean deleted = dataSource.removeRemovedTag(
            getCurrentUserId(), documentId, tagName);

        if(deleted)
            return ResponseEntity.noContent().build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("Tag does not exist: " + tagName);
    }

    // on path /export
    @RequestMapping(value = "/export", method = RequestMethod.GET, produces = MEDIA_RDF)
    @PreAuthorize("isAuthenticated()")
    public void handleUserContributionsExport(HttpServletRequest request, HttpServletResponse response) throws IOException, ImageResolverException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        UserAnnotations userAnnotations = dataSource.getAnnotationsByUser(auth.getName());

        String baseUrl = String.format("%s://%s:%d/", request.getScheme(), request.getServerName(), request.getServerPort());

        RDFReader rr = new RDFReader(auth.getName(), baseUrl, imageResolver);

        for (DocumentAnnotations docAnnotations : userAnnotations.getDocumentAnnotations()) {
            String documentId = docAnnotations.getDocumentId();
            for (Annotation annotation : docAnnotations.getAnnotations()) {
                rr.addElement(annotation, documentId);
            }
        }

        response.setHeader("Content-Disposition", "attachment; filename=" + ("USER" + "_" + Utils.getCurrentDateTime().toString()) + ".rdf");
        response.setHeader("Cache-Control", CACHE_PRIVATE.getHeaderValue());
        response.setHeader("Content-Type", MEDIA_RDF);

        OutputStream os = response.getOutputStream();
        rr.getModel().write(os, RDFFormat.RDFXML.getLang().getName());
        response.flushBuffer();
        os.close();
    }

    // on path /export
    @RequestMapping(value = "/export/{docId}", method = RequestMethod.GET, produces = MEDIA_RDF)
    @PreAuthorize("isAuthenticated()")
    public void handleUserDocumentContributionsExport(@PathVariable("docId") String documentId, HttpServletRequest request,
            HttpServletResponse response) throws IOException, ImageResolverException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        DocumentAnnotations docAnnotations = dataSource.getAnnotations(auth.getName(), documentId);

        String baseUrl = String.format("%s://%s:%d/", request.getScheme(), request.getServerName(), request.getServerPort());

        RDFReader rr = new RDFReader(auth.getName(), baseUrl, imageResolver);

        for (Annotation annotation : docAnnotations.getAnnotations()) {
            rr.addElement(annotation, documentId);
        }

        // prepareResp(response, "application/rdf+xml; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + (documentId + "_" + Utils.getCurrentDateTime().toString()) + ".rdf");
        response.setHeader("Cache-Control", CACHE_PRIVATE.getHeaderValue());

        OutputStream os = response.getOutputStream();
        rr.getModel().write(os, RDFFormat.RDFXML.getLang().getName());
        response.flushBuffer();
        os.close();
    }

    @ExceptionHandler
    public void handleObjectNotFound(
        HttpServletResponse resp, EmptyResultDataAccessException e)
        throws IOException {

        resp.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }
}
