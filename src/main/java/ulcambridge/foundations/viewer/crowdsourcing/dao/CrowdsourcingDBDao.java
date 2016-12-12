package ulcambridge.foundations.viewer.crowdsourcing.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ulcambridge.foundations.viewer.crowdsourcing.model.Annotation;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentAnnotations;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentTags;
import ulcambridge.foundations.viewer.crowdsourcing.model.Tag;
import ulcambridge.foundations.viewer.crowdsourcing.model.Term;
import ulcambridge.foundations.viewer.crowdsourcing.model.Terms;
import ulcambridge.foundations.viewer.crowdsourcing.model.UserAnnotations;
import ulcambridge.foundations.viewer.utils.Utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Lei
 */
@Component
public class CrowdsourcingDBDao implements CrowdsourcingDao {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public CrowdsourcingDBDao(
        JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {

        Assert.notNull(jdbcTemplate);
        Assert.notNull(objectMapper);

        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public DocumentAnnotations getAnnotations(
        String userId, String documentId, int documentPageNo) {

        List<Annotation> annotations = queryJsonList(
            Annotation.class,
            "SELECT annotations\n" +
            "FROM\n" +
            "  \"DocumentAnnotations\",\n" +
            "  json_array_elements(annos->'annotations') as annotations\n" +
            "WHERE \"docId\" = ? AND oid = ? AND (annotations->>'page')::int = ?;\n",
            documentId, userId, documentPageNo);

        return new DocumentAnnotations(userId, documentId, annotations);
    }

    @Override
    public DocumentAnnotations getAnnotations(String userId, String documentId) {
        return getAnnotationsOpt(userId, documentId)
            .orElseGet(() -> new DocumentAnnotations(
                userId, documentId, Collections.emptyList()));
    }


    public Optional<DocumentAnnotations> getAnnotationsOpt(
        String userId, String documentId) {

        return queryJsonOptional(
            DocumentAnnotations.class, SQL_USER_DOCUMENT_ANNOTATIONS,
            documentId, userId);
    }

    @Override
    public DocumentTags getTagsByDocument(String documentId) {
        List<Tag> tags = queryJsonList(Tag.class,
            "SELECT tag\n" +
            "FROM \"DocumentTags\", json_array_elements(tags->'tags') as tag\n" +
            "WHERE \"docId\" = ?", documentId);

        return new DocumentTags(null, documentId, tags);
    }

    private static final String GET_REMOVED_TAGS_QUERY =
        "SELECT removedtags FROM \"DocumentRemovedTags\" " +
        "WHERE \"oid\" = ? AND \"docId\" = ?";

    @Override
    public DocumentTags getRemovedTags(String userId, String documentId) {

        return queryJsonOptional(DocumentTags.class, GET_REMOVED_TAGS_QUERY, userId,
                         documentId)
            .orElseGet(() ->
                new DocumentTags(userId, documentId, Collections.emptyList()));
    }

    @Override
    public Annotation addAnnotation(
            String userId, String documentId, Annotation annotation)
            throws SQLException {

        List<Annotation> annotations =
            queryJsonOptional(
                DocumentAnnotations.class,
                SQL_USER_DOCUMENT_ANNOTATIONS, documentId, userId)
            .map(DocumentAnnotations::getTerms)
            .orElse(Collections.emptyList())
            .stream()
            // Remove any matching annotation
            .filter(((Predicate<Annotation>)annotation::equals).negate())
            .collect(Collectors.toList());

        annotations.add(new Annotation(
            annotation.getName(), annotation.getRaw(), annotation.getValue(),
            annotation.getTarget(), annotation.getType(), annotation.getPage(),
            UUID.randomUUID(), Utils.getCurrentDateTime(),
            annotation.getPosition()));

        DocumentAnnotations da = new DocumentAnnotations(
            userId, documentId, annotations);

        sqlUpsertAnnotations(da);

        return annotation;
    }

    @Override
    public boolean removeAnnotation(String userId, String documentId, UUID annotationUuid) throws SQLException {
        return removeAnnotations(userId, documentId,
                Collections.singleton(annotationUuid)).size() == 1;
    }

    @Override
    public Set<UUID> removeAnnotations(
            String userId, String documentId, Collection<UUID> annotationIds)
            throws SQLException {

        DocumentAnnotations annotations = this.getAnnotations(
            userId, documentId);

        Set<UUID> toRemove = new HashSet<>(annotationIds);

        // Partition annotations into two groups: to be removed and to be kept.
        Map<Boolean, List<Annotation>> partitionedAnnotations =
            annotations.getTerms().stream()
                .collect(Collectors.partitioningBy(
                    a -> toRemove.contains(a.getUuid())));

        Set<UUID> removed = partitionedAnnotations.get(true).stream()
            .map(Annotation::getUuid)
            .collect(Collectors.toSet());

        if(!removed.isEmpty())
            sqlUpsertAnnotations(new DocumentAnnotations(
                annotations.getUserId(), annotations.getDocumentId(),
                partitionedAnnotations.get(false)));

        return removed;
    }

    @Override
    public int addTag(DocumentTags documentTags) throws SQLException {

        String docId = documentTags.getDocumentId();
        Assert.notNull(docId);
        PGobject json = jsonValue(documentTags);

        return jdbcTemplate.update(
            "INSERT INTO \"DocumentTags\" (\"docId\", tags) VALUES (?, ?)\n" +
            "ON CONFLICT (\"docId\") DO UPDATE SET tags = ?;",
            docId, json, json);
    }

    @Override
    public Tag getRemovedTag(String userId, String documentId, String tagName) {
        return queryJson(Tag.class,
            "SELECT tag FROM \n" +
            "  (SELECT json_array_elements(removedTags::json->'tags') as tag\n" +
            "   FROM \"DocumentRemovedTags\"\n" +
            "   WHERE \"docId\" = ?\n" +
            "         AND oid = ?\n" +
            "  ) AS tags\n" +
            "WHERE tag->>'name' = ?;",
            documentId, userId, tagName);
    }

    @Override
    public UpsertResult<DocumentTags> addRemovedTag(
        String userId, String documentId, Tag removedTag) throws SQLException {

        DocumentTags dt = getRemovedTags(userId, documentId);

        List<Tag> removedTags = dt.getTerms().stream()
            .filter(((Predicate<Object>)removedTag::equals).negate())
            .collect(Collectors.toList());

        boolean created = removedTags.size() > dt.getTerms().size();
        dt = new DocumentTags(dt.getUserId(), dt.getDocumentId(), removedTags);

        sqlUpsertRemovedTags(dt);

        return CrowdsourcingDao.upsertResult(dt, created);
    }

    public boolean removeRemovedTag(
        String userId, String documentId, String tagName) throws SQLException {

        DocumentTags dt = getRemovedTags(userId, documentId);
        List<Tag> tags = dt.getTerms().stream()
            .filter(t -> !t.getName().equals(tagName))
            .collect(Collectors.toList());

        boolean removed = dt.getTerms().size() > tags.size();

        if(!removed)
            return false;

        dt = new DocumentTags(dt.getUserId(), dt.getDocumentId(), tags);
        return sqlUpsertRemovedTags(dt) > 0;
    }

    private static final String GET_DOCUMENT_ANNOTATIONS_QUERY =
        "SELECT annotation\n" +
        "FROM\n" +
        "  \"DocumentAnnotations\",\n" +
        "  json_array_elements(annos->'annotations') as annotation\n" +
        "WHERE \"docId\" = ?";

    @Override
    public Collection<Term> getMergedAnnotationsByDocument(String documentId) {
        return this.queryStream(rows -> {
            Map<String, Term> merged = rows.map(row -> row.getString(1))
                .map(jsonConverter(Annotation.class))
                .collect(Terms.mergeTerms(true));

            return merged.values();

        }, GET_DOCUMENT_ANNOTATIONS_QUERY, documentId);
    }

    private static final String GET_DOCUMENT_REMOVED_TAGS_QUERY =
        "SELECT tag\n" +
        "FROM\n" +
        "  \"DocumentRemovedTags\",\n" +
        "  json_array_elements(removedTags->'tags') as tag\n" +
        "WHERE \"docId\" = ?\n";

    @Override
    public Collection<Term> getMergedRemovedTagsByDocument(String documentId) {

        return this.queryStream(rows -> {
            Map<String, Term> mergedTags = rows
                .map(row -> row.getString(1))
                .map(jsonConverter(Tag.class))
                .collect(Terms.mergeTerms(true));

            return mergedTags.values();
        }, GET_DOCUMENT_REMOVED_TAGS_QUERY, documentId);
    }

    private <T> Function<String, T> jsonConverter(Class<T> cls) {
        return s -> {
            try {
                return this.objectMapper.readValue(s, cls);
            }
            catch(IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    @Override
    public UserAnnotations getAnnotationsByUser(String userId) {
        List<DocumentAnnotations> docAnnotations = queryJsonList(
            DocumentAnnotations.class,
            "SELECT annos FROM \"DocumentAnnotations\" WHERE \"oid\" = ?",
            userId);

        return new UserAnnotations(userId, docAnnotations);
    }

    @Override
    public List<String> getAnnotatedDocuments() {
        return jdbcTemplate.query(
            "SELECT DISTINCT \"docId\" FROM \"DocumentAnnotations\"",
            (rs, r) -> rs.getString(1));
    }

    @Override
    public List<String> getTaggedDocuments() {
        return jdbcTemplate.query(
            "SELECT DISTINCT \"docId\" FROM \"DocumentTags\"",
            (rs, r) -> rs.getString(1));
    }

    private static final String SQL_USER_DOCUMENT_ANNOTATIONS =
            "SELECT annos\n" +
            "FROM\n" +
            "  \"DocumentAnnotations\"\n" +
            "WHERE \"docId\" = ? AND oid = ?\n" +
            "LIMIT 1;";

    private PGobject jsonValue(Object value)
        throws SQLException {

        PGobject obj = new PGobject();
        obj.setType("json");
        try {
            obj.setValue(this.objectMapper.writeValueAsString(value));
        }
        catch(JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
        return obj;
    }

    private int sqlUpsertAnnotations(DocumentAnnotations da)
        throws SQLException {

        String query =
            "INSERT INTO \"DocumentAnnotations\" (oid, \"docId\", annos) \n" +
            "VALUES (?, ?, ?) \n" +
            "ON CONFLICT (oid, \"docId\") DO UPDATE SET annos=?;";

        Assert.notNull(da.getUserId());
        Assert.notNull(da.getDocumentId());

        PGobject annoJson = jsonValue(da);
        return jdbcTemplate.update(query, da.getUserId(), da.getDocumentId(), annoJson, annoJson);
    }

    private int sqlUpsertRemovedTags(DocumentTags docTags) throws SQLException {
        String query = "UPDATE \"DocumentRemovedTags\" SET \"removedtags\" = ? WHERE \"oid\" = ? AND \"docId\" = ?; "
                + "INSERT INTO \"DocumentRemovedTags\" (\"oid\", \"docId\", \"removedtags\") " + "SELECT ?, ?, ? "
                + "WHERE NOT EXISTS (SELECT * FROM \"DocumentRemovedTags\" WHERE \"oid\" = ? AND \"docId\" = ?);";

        PGobject json = new PGobject();
        json.setType("json");

        try {
            json.setValue(this.objectMapper.writeValueAsString(docTags));
        }
        catch(JsonProcessingException e) {
            throw new RuntimeException(
                "Failed to convert DocumentTags to JSON", e);
        }

        String uid = docTags.getUserId();
        Assert.notNull(uid);
        String did = docTags.getDocumentId();
        Assert.notNull(did);

        return jdbcTemplate.update(
            query, json, uid, did, uid, did, json, uid, did);
    }

    @FunctionalInterface
    public interface ColumnExtractor<T> {
        T extract(ResultSet rs) throws SQLException;
    }

    public static ColumnExtractor<String> stringColumn(String name) {
        return rs -> rs.getString(name);
    }

    public static ColumnExtractor<String> stringColumn(int position) {
        return rs -> rs.getString(position);
    }

    private static <T> RowMapper<T> mapColumnJsonAs(
        Class<T> type, ColumnExtractor<String> columnExtractor,
        ObjectMapper mapper) {

        return (rs, rowNum) -> {
            try {
                return mapper.readValue(columnExtractor.extract(rs), type);
            }
            catch(IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    @FunctionalInterface
    private interface QueryFunc<RowT, ResultT> {
        ResultT query(String query, Object[] args, RowMapper<RowT> rm);
    }

    private <RowT, ResultT> ResultT internalQueryJson(
        QueryFunc<RowT, ResultT> queryMethod, Class<RowT> type,
        ColumnExtractor<String> jsonColumn, String query, Object...params) {

        return queryMethod.query(query, params,
            mapColumnJsonAs(type, jsonColumn, this.objectMapper));
    }

    private static final ColumnExtractor<String> DEFAULT_JSON_COLUMN =
        stringColumn(1);

    private <T> List<T> queryJsonList(
        Class<T> type, ColumnExtractor<String> jsonColumn, String query,
        Object...params) {

        return internalQueryJson(
            this.jdbcTemplate::query, type, jsonColumn, query, params);
    }

    private <T> List<T> queryJsonList(
        Class<T> type, String query, Object...params) {

        return queryJsonList(type, DEFAULT_JSON_COLUMN, query, params);
    }

    private <T> T queryJson(
        Class<T> type, ColumnExtractor<String> jsonColumn, String query,
        Object...params) {

        return internalQueryJson(
            this.jdbcTemplate::queryForObject, type, jsonColumn, query, params);
    }

    private <T> T queryJson(
        Class<T> type, String query, Object...params) {

        return queryJson(type, DEFAULT_JSON_COLUMN, query, params);
    }

    private <T> Optional<T> queryJsonOptional(
        Class<T> type, ColumnExtractor<String> jsonColumn, String query,
        Object...params) {

        try {
            return Optional.of(internalQueryJson(
                this.jdbcTemplate::queryForObject, type, jsonColumn, query,
                params));

        }
        catch(IncorrectResultSizeDataAccessException e) {
            return Optional.empty();
        }
    }

    private <T> Optional<T> queryJsonOptional(
        Class<T> type, String query,
        Object...params) {

        return queryJsonOptional(type, DEFAULT_JSON_COLUMN, query, params);
    }

    /**
     * {@link Stream} access to rows from a query.
     *
     * <p>The stream is passed to the provided stream handling function.
     * Evaluation of the stream must occur before the streamHandler function
     * returns, as the underlying JDBC resources will be closed after the
     * streamHandler returns.
     *
     * <p>The stream contains references to rows via SqlRowSet. References to
     * the row sets must not be retained after the first traversal, as the rows
     * are not cached. If they are then calling methods on them will fail.
     *
     * @param streamHandler A function which will be called with the stream of
     *                      results, and should process the stream to produce a
     *                      value.
     * @param query The SQL query to execute
     * @param params The parameters to be substituted into the query
     * @return A stream yielding an SqlRowSet for each row the the evaluation of
     *         the query produces.
     */
    private <T> T queryStream(
        Function<Stream<SqlRowSet>, T> streamHandler, String query,
        Object...params) {

        return this.jdbcTemplate.query(query, resultSet -> {

            SqlRowSet rowSet = new ResultSetWrappingSqlRowSet(resultSet);

            Stream<SqlRowSet> stream =  StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                    new SqlRowSetIterator(rowSet),
                    Spliterator.IMMUTABLE | Spliterator.NONNULL),
                // Don't make the stream over the row set iterator parallel.
                // Each row set returned by the iterator has to be accessed in
                // order, as the row set doesn't cache rows by default.
                false);

            return streamHandler.apply(stream);
        }, params);
    }

    /**
     * Exposes an {@link SqlRowSet} as an Iterator which returns its row set for
     * each row that exists in the row set.
     */
    private static class SqlRowSetIterator implements Iterator<SqlRowSet> {

        private final SqlRowSet rs;
        private SingleRowSqlRowSetView rowSetView;
        private State state;

        private enum State { POSSIBLY_AT_END, AT_NEXT, AT_END }

        public SqlRowSetIterator(SqlRowSet rs) {
            Assert.notNull(rs);

            this.rs = rs;
            this.state = State.POSSIBLY_AT_END;
        }

        @Override
        public boolean hasNext() {
            if(state == State.POSSIBLY_AT_END) {
                if(rowSetView != null)
                    rowSetView.invalidate();

                if(rs.next()) {
                    rowSetView = new SingleRowSqlRowSetView(rs);
                    state = State.AT_NEXT;
                }
                else
                    state = State.AT_END;
            }

            return state == State.AT_NEXT;
        }

        @Override
        public SqlRowSet next() {
            // Note that hasNext() returns true if it can advance the state to
            // AT_NEXT.
            if(state == State.POSSIBLY_AT_END && hasNext() ||
               state == State.AT_NEXT) {

                assert state == State.AT_NEXT;
                assert rowSetView != null;
                assert rowSetView.isValid();
                state = State.POSSIBLY_AT_END;
                return rowSetView;
            }

            assert state == State.AT_END;
            throw new NoSuchElementException();
        }
    }
}
