package ulcambridge.foundations.viewer.crowdsourcing.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ulcambridge.foundations.viewer.crowdsourcing.model.Annotation;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentAnnotations;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentTags;
import ulcambridge.foundations.viewer.crowdsourcing.model.GsonFactory;
import ulcambridge.foundations.viewer.crowdsourcing.model.JSONConverter;
import ulcambridge.foundations.viewer.crowdsourcing.model.Tag;
import ulcambridge.foundations.viewer.crowdsourcing.model.UserAnnotations;
import ulcambridge.foundations.viewer.utils.Utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public DocumentAnnotations getAnnotations(String userId, String documentId, int documentPageNo) {
        List<Annotation> annotations = sqlGetAnnotations(userId, documentId, documentPageNo);

        DocumentAnnotations da = new DocumentAnnotations();
        da.setAnnotations(annotations);
        da.setUserId(userId);
        da.setDocumentId(documentId);
        da.setTotal(annotations.size());

        return da;
    }

    @Override
    public DocumentAnnotations getAnnotations(String userId, String documentId) {
        return sqlGetAnnotations(userId, documentId);
    }

    @Override
    public DocumentTags getTagsByDocument(String documentId) {
        // query
        JsonObject dt = sqlGetTags(documentId);

        if (!dt.has("docId")) {
            dt.addProperty("docId", documentId);
        }
        if(!dt.has("tags")) {
            dt.add("tags", new JsonArray());
            dt.addProperty("total", 0);
        }

        return new JSONConverter().toDocumentTags(dt);
    }

    private static final String GET_REMOVED_TAGS_QUERY =
        "SELECT removedtags FROM \"DocumentRemovedTags\" " +
        "WHERE \"oid\" = ? AND \"docId\" = ?";

    @Override
    public DocumentTags getRemovedTags(String userId, String documentId) {

        return queryJsonOptional(DocumentTags.class, GET_REMOVED_TAGS_QUERY, userId,
                         documentId)
            .orElseGet(() -> {
                // FIXME: Make model classes immutable
                // FIXME: Get rid of nullable fields on model classes
                // FIXME: Get rid of no-arg constructors on model classes
                DocumentTags dt = new DocumentTags();
                dt.setUserId(userId);
                dt.setDocumentId(documentId);
                dt.setTags(new ArrayList<>());
                return dt;
            });
    }

    @Override
    public Annotation addAnnotation(
            String userId, String documentId, Annotation annotation)
            throws SQLException {

        DocumentAnnotations da = sqlGetAnnotations(userId, documentId);
        List<Annotation> annotations = new ArrayList<Annotation>(da.getAnnotations());

        if (annotations.contains(annotation)) {
            annotations.remove(annotation);
        }
        annotation.setDate(Utils.getCurrentDateTime());
        annotation.setUuid(UUID.randomUUID());
        annotations.add(annotation);
        da.setAnnotations(annotations);
        da.setTotal(annotations.size());

        JsonObject newJson = new JSONConverter().toJsonDocumentAnnotations(da);

        // query
        int rowsAffected = sqlUpsertAnnotations(userId, documentId, newJson);

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

        JsonObject json;
        try {
            json = getUserDocumentAnnotations(userId, documentId);
        }
        catch(IncorrectResultSizeDataAccessException e) {
            return Collections.emptySet();
        }

        Set<UUID> removed = removeAnnotationsWithIds(json, annotationIds);

        if(removed.size() > 0)
            sqlUpdateAnnotations(userId, documentId, json);

        return removed;
    }

    private Set<UUID> removeAnnotationsWithIds(
            JsonObject json, Collection<UUID> annotationIds)
            throws SQLException {

        if(!(json.has("annotations") && json.get("annotations").isJsonArray()))
            throw new SQLException(
                    "JSON's top-level \"annotations\" field is not an array");

        JsonArray annotations = json.getAsJsonArray("annotations");
        JsonArray remainingAnnotations = new JsonArray();

        Set<UUID> removed = removeAnnotationsWithIds(
                annotations, remainingAnnotations, new HashSet<UUID>(annotationIds));

        json.add("annotations", remainingAnnotations);

        return removed;
    }

    private Set<UUID> removeAnnotationsWithIds(JsonArray annotations, JsonArray remaining, Set<UUID> annotationIds)
            throws SQLException {

        if(remaining == null || remaining.size() != 0)
            throw new IllegalArgumentException(
                    "remaining must be an empty JsonArray");

        Set<UUID> removed = new HashSet<UUID>();
        for(JsonElement e : annotations) {
            if(!e.isJsonObject()) {
                remaining.add(e);
                continue;
            }

            UUID uuid = getUuid("uuid", e.getAsJsonObject().get("uuid"));
            if(annotationIds.contains(uuid)) {
                removed.add(uuid);
            }
            else {
                remaining.add(e);
            }
        }

        return removed;
    }

    private UUID getUuid(String fieldName, JsonElement e) throws SQLException {
        if(e == null || !(e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()))
            throw new SQLException(String.format("%s is not a string", fieldName));

        String uuid = e.getAsJsonPrimitive().getAsString();
        try {
            return UUID.fromString(uuid);
        }
        catch(IllegalArgumentException ex) {
            String msg = String.format("Field %s contained an invalid UUID: %s",
                                        fieldName, uuid);
            throw new SQLException(msg, ex);
        }
    }

    @Override
    public int addTag(String documentId, DocumentTags documentTags) throws SQLException {
        JsonObject newJson = new JSONConverter().toJsonDocumentTags(documentTags);

        // query
        int rowsAffected = sqlUpsertTag(documentId, newJson);

        return 1;
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
    public UpsertResult<DocumentTags> addRemovedTag(String userId, String documentId, Tag removedTag) throws SQLException {
        DocumentTags dt = getRemovedTags(userId, documentId);

        List<Tag> removedTags = dt.getTags();

        boolean exists = removedTags.contains(removedTag);
        if(exists)
            removedTags.remove(removedTag);

        removedTags.add(removedTag);
        dt.setTotal(removedTags.size());

        sqlUpsertRemovedTags(dt);

        return CrowdsourcingDao.upsertResult(dt, !exists);
    }

    public boolean removeRemovedTag(
        String userId, String documentId, String tagName) throws SQLException {

        DocumentTags dt = getRemovedTags(userId, documentId);
        List<Tag> tags = dt.getTags().stream()
            .filter(t -> !t.getName().equals(tagName))
            .collect(Collectors.toList());

        boolean removed = dt.getTags().size() > tags.size();

        if(!removed)
            return false;

        dt.setTags(tags);
        dt.setTotal(tags.size()); // -_-
        return sqlUpsertRemovedTags(dt) > 0;
    }

    @Override
    public DocumentAnnotations getAnnotationsByDocument(String documentId) {
        // query
        List<JsonObject> docAnnotationList = sqlGetAnnotationsByDocument(documentId);

        JSONConverter jc = new JSONConverter();

        Set<Annotation> annotations = new HashSet<Annotation>();
        for (JsonObject docAnnos : docAnnotationList) {
            if (!docAnnos.has("annotations"))
                continue;
            JsonArray annos = docAnnos.getAsJsonArray("annotations");
            Iterator<JsonElement> it = annos.iterator();
            while (it.hasNext()) {
                JsonObject anno = (JsonObject) it.next();
                Annotation annotation = jc.toAnnotation(anno);
                annotations.add(annotation);
            }
        }

        JsonObject json = new JsonObject();
        JsonArray distinctAnnos = new JsonArray();

        for (Annotation annotation : annotations) {
            distinctAnnos.add(jc.toJsonAnnotation(annotation));
        }

        json.addProperty("docId", documentId);
        json.addProperty("total", distinctAnnos.size());
        json.add("annotations", distinctAnnos);

        return jc.toDocumentAnnotations(json);
    }

    @Override
    public DocumentTags getRemovedTagsByDocument(String documentId) {
        // query
        List<JsonObject> docRemovedTagList = sqlGetRemovedTagsByDocument(documentId);

        JSONConverter jc = new JSONConverter();

        Set<Tag> removedTags = new HashSet<Tag>();
        for (JsonObject docRemovedTags : docRemovedTagList) {
            if (!docRemovedTags.has("tags"))
                continue;
            JsonArray rmvTags = docRemovedTags.getAsJsonArray("tags");
            Iterator<JsonElement> it = rmvTags.iterator();
            while (it.hasNext()) {
                JsonObject rmvTag = (JsonObject) it.next();
                Tag tag = jc.toTag(rmvTag);
                removedTags.add(tag);
            }
        }

        JsonObject json = new JsonObject();
        JsonArray distinctRemovedTags = new JsonArray();

        for (Tag tag : removedTags) {
            distinctRemovedTags.add(jc.toJsonTag(tag));
        }

        json.addProperty("docId", documentId);
        json.addProperty("total", distinctRemovedTags.size());
        json.add("tags", distinctRemovedTags);

        return jc.toDocumentTags(json);
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
        String query = "SELECT DISTINCT \"docId\" FROM \"DocumentAnnotations\"";

        return jdbcTemplate.query(query, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                String docId = rs.getString("docId");
                return docId;
            }
        });
    }

    @Override
    public List<String> getTaggedDocuments() {
        String query = "SELECT DISTINCT \"docId\" FROM \"DocumentTags\"";

        return jdbcTemplate.query(query, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                String docId = rs.getString("docId");
                return docId;
            }
        });
    }

    private static final class GsonRowMapper<T> implements RowMapper<T> {
        private final Class<T> clazz;
        private final int columnIndex;
        private final Gson gson;

        public GsonRowMapper(Class<T> clazz) {
            this(clazz, 1, GsonFactory.create());
        }

        public GsonRowMapper(Class<T> clazz, int jsonColumnIndex, Gson gson) {
            if(jsonColumnIndex < 1)
                throw new IllegalArgumentException(
                        "index must be >= 1, got: " + jsonColumnIndex);

            if(clazz == null)
                throw new IllegalArgumentException("class was null");

            if(gson == null)
                throw new IllegalArgumentException("gson was null");


            this.columnIndex = jsonColumnIndex;
            this.clazz = clazz;
            this.gson = gson;
        }

        @Override
        public T mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                return this.gson.fromJson(rs.getString(this.columnIndex), clazz);
            }
            catch(JsonSyntaxException e) {
                throw new SQLException("Unable to map json to " + this.clazz, e);
            }
        }
    }

    private static final RowMapper<DocumentAnnotations> DOC_ANNOTATIONS_ROW_MAPPER =
            new GsonRowMapper<DocumentAnnotations>(DocumentAnnotations.class);

    private DocumentAnnotations sqlGetAnnotations(final String userId, final String documentId) {
        try {
            return jdbcTemplate.queryForObject(
                    SQL_USER_DOCUMENT_ANNOTATIONS,
                    DOC_ANNOTATIONS_ROW_MAPPER,
                    documentId, userId);
        }
        catch(IncorrectResultSizeDataAccessException e) {
            DocumentAnnotations da = new DocumentAnnotations();
            da.setDocumentId(documentId);
            da.setUserId(userId);
            return da;
        }
    }

    private List<Annotation> sqlGetAnnotations(final String userId, final String documentId, final int documentPageNo) {
        String query =
                "SELECT annotations\n" +
                "FROM\n" +
                "  \"DocumentAnnotations\",\n" +
                "  json_array_elements(annos->'annotations') as annotations\n" +
                "WHERE \"docId\" = ? AND oid = ? AND (annotations->>'page')::int = ?;\n";

        Object[] params = new Object[] {documentId, userId, documentPageNo};

        return jdbcTemplate.query(query, params, new RowMapper<Annotation>() {
            Gson gson = GsonFactory.create();

            @Override
            public Annotation mapRow(ResultSet rs, int rowNum) throws SQLException {
                return gson.fromJson(rs.getString(1), Annotation.class);
            }
        });
    }

    private static final String SQL_USER_DOCUMENT_ANNOTATIONS =
            "SELECT annos\n" +
            "FROM\n" +
            "  \"DocumentAnnotations\"\n" +
            "WHERE \"docId\" = ? AND oid = ?\n" +
            "LIMIT 1;";

    private static final RowMapper<JsonObject> JSON_OBJECT_ROW_MAPPER =
            new GsonRowMapper<JsonObject>(JsonObject.class);


    /**
     * Get all of a user's annotations for a given document.
     */
    private JsonObject getUserDocumentAnnotations(String userId, String documentId) {
        return jdbcTemplate.queryForObject(
                SQL_USER_DOCUMENT_ANNOTATIONS, JSON_OBJECT_ROW_MAPPER,
                documentId, userId);
    }

    private JsonObject sqlGetTags(final String documentId) {
        String query = "SELECT tags FROM \"DocumentTags\" WHERE \"docId\" = ?";

        return jdbcTemplate.query(query, new Object[] { documentId }, new ResultSetExtractor<JsonObject>() {
            @Override
            public JsonObject extractData(ResultSet rs) throws SQLException, DataAccessException {
                List<String> dts = new ArrayList<String>();
                while (rs.next()) {
                    String json = rs.getString("tags");
                    dts.add(json);
                }
                return (dts.isEmpty()) ? new JsonObject() : (JsonObject) new JsonParser().parse(dts.get(0));
            }
        });
    }

    private JsonObject sqlGetRemovedTags(final String userId, final String documentId) {
        String query = "SELECT removedtags FROM \"DocumentRemovedTags\" WHERE \"oid\" = ? AND \"docId\" = ?";

        return jdbcTemplate.query(query, new Object[] { userId, documentId }, new ResultSetExtractor<JsonObject>() {
            @Override
            public JsonObject extractData(ResultSet rs) throws SQLException, DataAccessException {
                List<String> removedTags = new ArrayList<String>();
                while (rs.next()) {
                    String json = rs.getString("removedtags");
                    removedTags.add(json);
                }
                return (removedTags.isEmpty()) ? new JsonObject() : (JsonObject) new JsonParser().parse(removedTags.get(0));
            }
        });
    }

    private int sqlUpsertAnnotations(String userId, String documentId, JsonObject newJson) throws SQLException {
        String query = "UPDATE \"DocumentAnnotations\" SET \"annos\" = ? WHERE \"oid\" = ? AND \"docId\" = ?; "
                + "INSERT INTO \"DocumentAnnotations\" (\"oid\", \"docId\", \"annos\") " + "SELECT ?, ?, ? "
                + "WHERE NOT EXISTS (SELECT * FROM \"DocumentAnnotations\" WHERE \"oid\" = ? AND \"docId\" = ?);";

        PGobject json = new PGobject();
        json.setType("json");
        json.setValue(newJson.toString());

        return jdbcTemplate.update(query, new Object[] { json, userId, documentId, userId, documentId, json, userId, documentId });
    }

    private int sqlUpdateAnnotations(String userId, String documentId, JsonObject newJson) throws SQLException {
        String query = "UPDATE \"DocumentAnnotations\" SET \"annos\" = ? WHERE \"oid\" = ? AND \"docId\" = ?;";

        PGobject json = new PGobject();
        json.setType("json");
        json.setValue(newJson.toString());

        return jdbcTemplate.update(query, new Object[] { json, userId, documentId });
    }

    private int sqlUpsertTag(String documentId, JsonObject newJson) throws SQLException {
        String query = "UPDATE \"DocumentTags\" SET \"tags\" = ? WHERE \"docId\" = ?; " + "INSERT INTO \"DocumentTags\" (\"docId\", \"tags\") "
                + "SELECT ?, ? " + "WHERE NOT EXISTS (SELECT * FROM \"DocumentTags\" WHERE \"docId\" = ?);";

        PGobject json = new PGobject();
        json.setType("json");
        json.setValue(newJson.toString());

        return jdbcTemplate.update(query, new Object[] { json, documentId, documentId, json, documentId });
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

    private List<JsonObject> sqlGetAnnotationsByDocument(final String documentId) {
        String query = "SELECT annos FROM \"DocumentAnnotations\" WHERE \"docId\" = ?";

        return jdbcTemplate.query(query, new Object[] { documentId }, new RowMapper<JsonObject>() {
            @Override
            public JsonObject mapRow(ResultSet rs, int rowNum) throws SQLException {
                String tag = rs.getString("annos");
                return (JsonObject) new JsonParser().parse(tag);
            }
        });
    }

    private List<JsonObject> sqlGetRemovedTagsByDocument(String documentId) {
        String query = "SELECT removedtags FROM \"DocumentRemovedTags\" WHERE \"docId\" = ?";

        return jdbcTemplate.query(query, new Object[] { documentId }, new RowMapper<JsonObject>() {
            @Override
            public JsonObject mapRow(ResultSet rs, int rowNum) throws SQLException {
                String removedTag = rs.getString("removedtags");
                return (JsonObject) new JsonParser().parse(removedTag);
            }
        });
    }

    private List<JsonObject> sqlGetAnnotationsByUser(final String userId) {
        String query = "SELECT annos FROM \"DocumentAnnotations\" WHERE \"oid\" = ?";

        return jdbcTemplate.query(query, new Object[] { userId }, new RowMapper<JsonObject>() {
            @Override
            public JsonObject mapRow(ResultSet rs, int rowNum) throws SQLException {
                String tag = rs.getString("annos");
                return (JsonObject) new JsonParser().parse(tag);
            }
        });
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
}
