package ulcambridge.foundations.viewer.crowdsourcing;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Charsets;

import ulcambridge.foundations.viewer.crowdsourcing.dao.CrowdsourcingDao;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentAnnotations;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentTags;
import ulcambridge.foundations.viewer.crowdsourcing.model.DocumentTerms;
import ulcambridge.foundations.viewer.crowdsourcing.model.FileReader;
import ulcambridge.foundations.viewer.crowdsourcing.model.JsonResponse;
import ulcambridge.foundations.viewer.crowdsourcing.model.SourceReader;
import ulcambridge.foundations.viewer.crowdsourcing.model.Tag;
import ulcambridge.foundations.viewer.crowdsourcing.model.TermCombiner;

/**
 * Handles requests for generating combined xmls for xtf search.
 *
 * @author Lei
 *
 */
@RestController
@RequestMapping("/crowdsourcing/xtfxml")
public class CrowdsourcingXMLController {

    private static final Logger logger = LoggerFactory.getLogger(CrowdsourcingXMLController.class);

    private final CrowdsourcingDao dataSource;

    @Value("${api.cs.key}")
    private String apiKey;

    @Value("${path.meta}")
    private String pathMeta;
    @Value("${path.fragment}")
    private String pathFrag;

    @Value("${path.anno}")
    private String pathAnno;
    @Value("${path.tag}")
    private String pathTag;
    @Value("${path.annotag}")
    private String pathAnnoTag;
    @Value("${path.annometa}")
    private String pathAnnoMeta;
    @Value("${path.tagmeta}")
    private String pathTagMeta;
    @Value("${path.annotagmeta}")
    private String pathAnnoTagMeta;

    @Autowired
    public CrowdsourcingXMLController(CrowdsourcingDao crowdsourcingDao) {
        Assert.notNull(crowdsourcingDao);

        this.dataSource = crowdsourcingDao;
    }

    // on path /updatefrags
    @RequestMapping(value = "/updatefrags/{key}", method = RequestMethod.GET)
    public JsonResponse handleUpdateFragments(@PathVariable("key") String key) {

        if (!key.equals(apiKey))
            return new JsonResponse("400", "API key missing");

        // TODO validate filenames, skip invalid filenames
        //
        SourceReader sr = new SourceReader(pathMeta, pathFrag);
        List<File> fragmentFiles = sr.listFragments();

        if (fragmentFiles == null) {
            logger.error("Fragments not found");
            return new JsonResponse("400", "Fragments not found");
        }

        int size = fragmentFiles.size(), counter = 0, counterSkip = 0;

        //for (String documentPath : fragmentNames) {
        for (File fragmentFile : fragmentFiles) {
            String documentId = FilenameUtils.getBaseName(fragmentFile.getName());

            counter++;
            logger.info(counter + "/" + size + ": " + documentId);

            try {
                // extract tags from file
                List<Tag> tags = sr.extractKeywords(fragmentFile.getPath());

                DocumentTags dt = new DocumentTags();
                dt.setDocumentId(documentId);
                dt.setTotal(tags.size());
                dt.setTags(tags);

                int rowsAffected = dataSource.addTag(documentId, dt);

            } catch (Exception e) {
                counterSkip++;
                e.printStackTrace();
            }
        }
        logger.info("skip: " + counterSkip);

        return new JsonResponse("200", "Tags added/updated in database");
    }

    // on path /
    @RequestMapping(value = "/{type}/{key}", method = RequestMethod.GET)
    public JsonResponse handleGenerateXMLsForXTFSearch(@PathVariable("type") String type, @PathVariable("key") String key) {

        if (!key.equals(apiKey))
            return new JsonResponse("400", "API key missing");

        // annotation xml
        if (type.equals("anno")) {
            generateXML_Anno();
            return new JsonResponse("200", "Annotation XMLs generated");
        }

        // tag xml
        else if (type.equals("tag")) {
            generateXML_Tag();
            return new JsonResponse("200", "Combined XMLs (tags+removed tags) generated");
        }

        // annotation & tag xml
        else if (type.equals("annotag")) {
            generateXML_AnnoTag();
            return new JsonResponse("200", "Combined XMLs (annotations and tags+removed tags) generated");
        } else if (type.equals("annometa") || type.equals("tagmeta") || type.equals("annotagmeta")) {
                SourceReader sr = new SourceReader(pathMeta, pathFrag);
                List<File> metadataFiles = sr.listMetadata();

                if (metadataFiles == null) {
                    logger.error("Metadata not found, " + pathMeta);
                    return new JsonResponse("400", "Metadata not found");
                }

                // annotation & metadata xml
                if (type.equals("annometa")) {
                    generateXML_AnnoMeta(metadataFiles, sr);
                    return new JsonResponse("200", "Combined XMLs (annotations and metadata) generated");
                }

                // tag & metadata xml
                else if (type.equals("tagmeta")) {
                    generateXML_TagMeta(metadataFiles, sr);
                    return new JsonResponse("200", "Combined XMLs (tags+removed tags and metadata) generated");
                }

                // annotation, tag & metadata xml
                else if (type.equals("annotagmeta")) {
                    generateXML_AnnoTagMeta(metadataFiles, sr);
                    return new JsonResponse("200", "Combined XMLs (annotations, tags+removed tags and metadata) generated");
                }

                else {
                    return new JsonResponse("400", "Bad requeist");
                }
        } else if (type.equals("all")) {
            generateXML_Anno();
            generateXML_Tag();
            generateXML_AnnoTag();

            SourceReader sr = new SourceReader(pathMeta, pathFrag);
            List<File> metadataFiles = sr.listMetadata();

            if (metadataFiles == null) {
                logger.error("Metadata not found");
                return new JsonResponse("400", "Metadata not found");
            }

            generateXML_AnnoMeta(metadataFiles, sr);
            generateXML_TagMeta(metadataFiles, sr);
            generateXML_AnnoTagMeta(metadataFiles, sr);

            return new JsonResponse("200", "XML documents for XTF search generated");
        } else {
            return new JsonResponse("400", "Bad requeist");
        }
    }

    private void generateXML_Anno() {
        List<String> documentIds = dataSource.getAnnotatedDocuments();

        int counter = 0, size = documentIds.size();

        for (String documentId : documentIds) {
            DocumentAnnotations docAnnotation = dataSource.getAnnotationsByDocument(documentId);

            DocumentTerms docTerms = new DocumentTerms(docAnnotation.getDocumentId(), docAnnotation.getTotal(), docAnnotation.getTerms());

            String path = (new File(pathAnno, documentId + ".xml")).getPath();
            new FileReader().save(path, docTerms.toJAXBString(docTerms));

            counter++;
            logger.info(counter + "/" + size + ": " + documentId);
        }
    }

    private void generateXML_Tag() {
        List<String> documentIds = dataSource.getTaggedDocuments();

        int counter = 0, size = documentIds.size();

        for (String documentId : documentIds) {
            DocumentTags docTags = dataSource.getTagsByDocument(documentId);
            DocumentTags docRemovedTags = dataSource.getRemovedTagsByDocument(documentId);

            // combine tags with removed tags
            DocumentTerms docTerms = new TermCombiner().combine_Tag_RemovedTag(docTags, docRemovedTags);

            String path = (new File(pathTag, documentId + ".xml")).getPath();
            new FileReader().save(path, docTerms.toJAXBString(docTerms));

            counter++;
            logger.info(counter + "/" + size + ": " + documentId);
        }
    }

    private void generateXML_AnnoTag() {
        List<String> annotatedDocuments = dataSource.getAnnotatedDocuments();
        List<String> taggedDocuments = dataSource.getTaggedDocuments();
        Set<String> distinctDocumentIdsSet = new HashSet<String>(annotatedDocuments);
        distinctDocumentIdsSet.addAll(taggedDocuments);

        List<String> documentIds = new ArrayList<String>(distinctDocumentIdsSet);

        int counter = 0, size = documentIds.size();

        for (String documentId : documentIds) {
            DocumentAnnotations docAnnotations = dataSource.getAnnotationsByDocument(documentId);
            DocumentTags docTags = dataSource.getTagsByDocument(documentId);
            DocumentTags docRemovedTags = dataSource.getRemovedTagsByDocument(documentId);

            // combine annotations, tags and removed tags
            DocumentTerms docTerms = new TermCombiner().combine_Anno_Tag_RemovedTag(docAnnotations, docTags, docRemovedTags);

            String path = (new File(pathAnnoTag, documentId + ".xml")).getPath();
            new FileReader().save(path, docTerms.toJAXBString(docTerms));

            counter++;
            logger.info(counter + "/" + size + ": " + documentId);
        }
    }

    private void generateXML_AnnoMeta(List<File> metadataFiles, SourceReader sr) {
        int counter = 0, size = metadataFiles.size();

        for (File metadataFile : metadataFiles) {
            String metadataId = FilenameUtils.getBaseName(metadataFile.getName());

            DocumentAnnotations docAnnotations = dataSource.getAnnotationsByDocument(FilenameUtils.getBaseName(metadataId));

            // combine annotations with metadata (level up raw)
            DocumentTerms docTerms = new TermCombiner().updateToMetaLevel(docAnnotations);

            String xml = sr.combineXMLs(new FileReader().read(metadataFile.getPath(), Charsets.UTF_8), docTerms.toJAXBString(docTerms));
            String path = (new File(pathAnnoMeta, metadataId + ".xml")).getPath();
            new FileReader().save(path, xml);

            counter++;
            logger.info(counter + "/" + size + ": " + metadataId);
        }
    }

    private void generateXML_TagMeta(List<File> metadataFiles, SourceReader sr) {
        int counter = 0, size = metadataFiles.size();

        for (File metadataFile : metadataFiles) {
            String metadataId = FilenameUtils.getBaseName(metadataFile.getName());

            DocumentTags docTags = dataSource.getTagsByDocument(metadataId);
            DocumentTags docRemovedTags = dataSource.getRemovedTagsByDocument(metadataId);

            // combine tags, removed tags with meta (level up raw)
            DocumentTerms docTerms = new TermCombiner().updateToMetaLevel(docTags, docRemovedTags);

            String xml = sr.combineXMLs(new FileReader().read(metadataFile.getPath(), Charsets.UTF_8), docTerms.toJAXBString(docTerms));
            String path = (new File(pathTagMeta, metadataId + ".xml")).getPath();
            new FileReader().save(path, xml);

            counter++;
            logger.info(counter + "/" + size + ": " + metadataId);
        }
    }

    private void generateXML_AnnoTagMeta(List<File> metadataFiles, SourceReader sr) {
        int counter = 0, size = metadataFiles.size();

        for (File metadataFile : metadataFiles) {
            String metadataId = FilenameUtils.getBaseName(metadataFile.getName());

            DocumentAnnotations docAnnotations = dataSource.getAnnotationsByDocument(metadataId);
            DocumentTags docTags = dataSource.getTagsByDocument(metadataId);
            DocumentTags docRemovedTags = dataSource.getRemovedTagsByDocument(metadataId);

            // combine annotations, tags, removed tags with meta (level up raw)
            DocumentTerms docTerms = new TermCombiner().updateToMetaLevel(docAnnotations, docTags, docRemovedTags);

            String xml = sr.combineXMLs(new FileReader().read(metadataFile.getPath(), Charsets.UTF_8), docTerms.toJAXBString(docTerms));
            String path = (new File(pathAnnoTagMeta, metadataId + ".xml")).getPath();
            new FileReader().save(path, xml);

            counter++;
            logger.info(counter + "/" + size + ": " + metadataId);
        }
    }

}
