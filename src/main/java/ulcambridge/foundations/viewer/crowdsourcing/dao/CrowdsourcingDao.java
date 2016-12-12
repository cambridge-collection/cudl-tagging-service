package ulcambridge.foundations.viewer.crowdsourcing.dao;

import ulcambridge.foundations.viewer.crowdsourcing.model.Annotation;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentAnnotations;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentTags;
import ulcambridge.foundations.viewer.crowdsourcing.model.Tag;
import ulcambridge.foundations.viewer.crowdsourcing.model.Term;
import ulcambridge.foundations.viewer.crowdsourcing.model.UserAnnotations;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Lei
 *
 */
public interface CrowdsourcingDao {

    DocumentAnnotations getAnnotations(String userId, String documentId, int documentPageNo);

    DocumentAnnotations getAnnotations(String userId, String documentId);

    Collection<Term> getMergedAnnotationsByDocument(String documentId);

    Collection<Term> getMergedRemovedTagsByDocument(String documentId);

    UserAnnotations getAnnotationsByUser(String userId);

    DocumentTags getTagsByDocument(String documentId);

    Tag getRemovedTag(String userId, String documentId, String tagName);

    DocumentTags getRemovedTags(String userId, String documentId);

    Annotation addAnnotation(String userId, String documentId, Annotation annotation) throws SQLException;

    int addTag(DocumentTags documentTags) throws SQLException;

    UpsertResult<DocumentTags> addRemovedTag(String userId, String documentId, Tag removedTag) throws SQLException;

    boolean removeRemovedTag(String userId, String documentId, String tagName) throws SQLException;

    boolean removeAnnotation(String userId, String documentId, UUID annotationUuid) throws SQLException;

    /**
     * Remove multiple annotations owned by a single user from a document.
     *
     * @param userId The ID of the user whose annotations are to be removed
     * @param documentId The ID of the document from which annotations will be removed
     * @param annotationIds The IDs of the annotations to be removed
     * @return The set of annotation IDs that were removed
     * @throws SQLException
     */
    Set<UUID> removeAnnotations(String userId, String documentId, Collection<UUID> annotationIds) throws SQLException;

    List<String> getAnnotatedDocuments();

    List<String> getTaggedDocuments();

    interface UpsertResult<T> {
        T getValue();
        boolean wasCreated();
        default boolean wasUpdated() { return !this.wasCreated(); }
    }

    static <T> UpsertResult<T> upsertResult(T value, boolean created) {
        return new UpsertResult<T>() {
            @Override
            public T getValue() {
                return value;
            }

            @Override
            public boolean wasCreated() {
                return created;
            }
        };
    }
}
