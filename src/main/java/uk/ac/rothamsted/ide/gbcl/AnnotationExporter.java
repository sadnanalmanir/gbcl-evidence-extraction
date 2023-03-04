package uk.ac.rothamsted.ide.gbcl;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import gate.util.OffsetComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AnnotationExporter {
    private static final Logger logger = LogManager.getLogger(AnnotationExporter.class);

    public AnnotationExporter() throws Exception {

        if (!Gate.isInitialised()) {
            Properties pro = new Properties();
            try {
                //pro.load(new FileInputStream(new File("resources/project.properties")));
                pro.load(Files.newInputStream(new File(logger.getClass().getClassLoader().getResource("project.properties").toURI()).toPath()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            //
            // Load GATE.
            //
            String gateHome = pro.getProperty("GATE_HOME");
            logger.info("Initializing GATE...");
            // Set GATE HOME directory.
            Gate.setGateHome(new File(gateHome));
            // Set GATE plugins directory.
            Gate.setPluginsHome(new File(gateHome, "plugins"));
            System.out.println(Gate.getPluginsHome());
            // Initialise GATE.
            Gate.init();
            logger.info("Initializing GATE... Done.");
        }
    }

    protected void exportAnnotations2CSV(String sourceAnnotationType, Collection<String> containedAnnotationTypes, String xmlOutputDirName, BufferedWriter annBuffWriter) throws InvalidOffsetException, ResourceInstantiationException, IOException {
        // initialize directory with annotated XML files
        Map<String, String> fileIndex = Utils.initFileIndex(xmlOutputDirName);
        // Monitor file currently being processed.
        int numberOfFilesToProcess = fileIndex.size();
        int numberOfFileProcessed = 0;

        // Add headers to the columns in the spreadsheet
        annBuffWriter.write("document" +
                "\t" + "Annotation Type" +
                "\t" + "Sentence" +
                "\t" + "Pest" +
                "\t" + "Crop" +
                "\t" + "Location" +
                "\t" + "ImpactNumber" +
                "\t" + "ImpactNumberUnit" +
                "\t" + "ImpactDirection" +
                "\t" + "YieldMention" +
                "\n");

        // Examine each annotated XML document
        for (String fileName : fileIndex.keySet()) {
            numberOfFileProcessed++;

            logger.info("Looking up annotations from \n===============================================================\n"
                    + "Document (" + numberOfFileProcessed + " of " + numberOfFilesToProcess + "): " + fileName + " AT " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())

                    + "\n===============================================================\n");

            gate.Document doc = Factory.newDocument(new URL(fileIndex.get(fileName)));
            doc.setName(fileName);
            writeSentenceAnnotations2CSV(doc, sourceAnnotationType, containedAnnotationTypes, annBuffWriter);
            Factory.deleteResource(doc);
        }
        annBuffWriter.close();
    }


    /**
     * @param doc                      Gate docuent
     * @param sourceAnnotationType     Types of annotation which must be of sentence
     * @param containedAnnotationTypes Types of annotation which must be of sentence
     * @param textBuffWriter           Writer for text to a character-output stream
     */
    private void writeSentenceAnnotations2CSV(Document doc, String sourceAnnotationType, Collection<String> containedAnnotationTypes, BufferedWriter textBuffWriter) throws IOException, InvalidOffsetException {

        // Get all sentences from annotated document
        AnnotationSet sourceAS = doc.getAnnotations().get(sourceAnnotationType);
        List<Annotation> sourceAnns = new ArrayList<>(sourceAS);
        sourceAnns.sort(new OffsetComparator());


        // list for each of the column header to hold multiple annotation instances in the same sentences
        // There may be multiple pests [pest1, pest2, pest3, ...] in the same sentence
        List<String> pestAnnContent = new ArrayList<>();
        List<String> cropAnnContent = new ArrayList<>();
        List<String> locationAnnContent = new ArrayList<>();
        List<String> impactNumberAnnContent = new ArrayList<>();
        List<String> impactNumberUnitAnnContent = new ArrayList<>();
        List<String> impactDirectionAnnContent = new ArrayList<>();
        List<String> yieldMentionAnnContent = new ArrayList<>();

        for (Annotation sourceAnn : sourceAnns) {
            long start = sourceAnn.getStartNode().getOffset();
            long end = sourceAnn.getEndNode().getOffset();


            logger.info("sourceAnnotationType: " + sourceAnnotationType);
            for (String annotationType : containedAnnotationTypes) {
                logger.info("annotationType: " + annotationType);
                //AnnotationSet annotationTypeMentions = doc.getAnnotations().get(annotationType,start,end);
                Set<Annotation> annotationTypeMentions = doc.getAnnotations().get(annotationType, start, end);
                if (annotationTypeMentions.isEmpty()) {
                    //containsAll = false;
                    break;
                } else {
                    //features.put("fake", "fake");

                    for (Annotation ann : annotationTypeMentions) {
                        String annString = doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()).toString().replace("\n", " ").trim();
                        logger.info("annString: " + annString);
                        if (annotationType.equals("Pest")) {
                            pestAnnContent.add(annString);
                        }
                        if (annotationType.equals("Crop")) {
                            cropAnnContent.add(annString);
                        }
                        if (annotationType.equals("Location")) {
                            locationAnnContent.add(annString);
                        }
                        if (annotationType.equals("ImpactNumber")) {
                            impactNumberAnnContent.add(annString);
                        }
                        if (annotationType.equals("ImpactNumberUnit")) {
                            impactNumberUnitAnnContent.add(annString);
                        }
                        if (annotationType.equals("ImpactDirection")) {
                            impactDirectionAnnContent.add(annString);
                        }
                        if (annotationType.equals("YieldMention")) {
                            yieldMentionAnnContent.add(annString);
                        }
                    }
                }
            }
            if (textBuffWriter != null) {
                //System.out.println("not null");
                textBuffWriter.write(doc.getName() +
                        "\t" + sourceAnnotationType +
                        // replace sentence line breaks with a whitespace
                        "\t" + doc.getContent().getContent(start, end).toString().replace("\n", " ").trim() +
                        "\t" + pestAnnContent +
                        "\t" + cropAnnContent +
                        "\t" + locationAnnContent +
                        "\t" + impactNumberAnnContent +
                        "\t" + impactNumberUnitAnnContent +
                        "\t" + impactDirectionAnnContent +
                        "\t" + yieldMentionAnnContent +
                        "\n");
                //textBuffWriter.write(doc.getName() + "\t" + annString + "\t" + (String) ann.getFeatures().get("Modifier") + "\t" + (String) ann.getFeatures().get("Object") + "\t" + (String) ann.getFeatures().get("Protein") + "\n");
            }
            // reset lists
            pestAnnContent.clear();
            cropAnnContent.clear();
            locationAnnContent.clear();
            impactNumberAnnContent.clear();
            impactNumberUnitAnnContent.clear();
            impactDirectionAnnContent.clear();
            yieldMentionAnnContent.clear();


        }
    }



    protected void collectAnnotationRows(Document doc, String sourceAnnotation, Collection<String> individualAnnotation, BufferedWriter resultsWriter) throws InvalidOffsetException, IOException {

        // Row of annotations retrieved
        String allAnnotations;

        // List of all source annotations
        AnnotationSet sourceAS = doc.getAnnotations().get(sourceAnnotation);
        List<Annotation> sourceAnns = new ArrayList<>(sourceAS);
        sourceAnns.sort(new OffsetComparator());

        // list for each of the column header to hold multiple annotation instances in the same sentences
        // There may be multiple pests [pest1, pest2, pest3, ...] in the same sentence
        Set<String> pestAnnContent = new HashSet<>();
        Set<String> cropAnnContent = new HashSet<>();
        Set<String> locationAnnContent = new HashSet<>();
        Set<String> impactNumberAnnContent = new HashSet<>();
        Set<String> impactNumberUnitAnnContent = new HashSet<>();
        Set<String> impactDirectionAnnContent = new HashSet<>();
        Set<String> yieldMentionAnnContent = new HashSet<>();

        for (Annotation sourceAnn : sourceAnns) {
            long start = sourceAnn.getStartNode().getOffset();
            long end = sourceAnn.getEndNode().getOffset();


            logger.info("sourceAnnotationType: " + sourceAnnotation);
            for (String annotationType : individualAnnotation) {
                logger.info("annotationType: " + annotationType);
                //AnnotationSet annotationTypeMentions = doc.getAnnotations().get(annotationType,start,end);
                Set<Annotation> annotationTypeMentions = doc.getAnnotations().get(annotationType, start, end);
                if (annotationTypeMentions.isEmpty()) {
                    //containsAll = false;
                    break;
                } else {
                    //features.put("fake", "fake");

                    for (Annotation ann : annotationTypeMentions) {
                        String annString = getCleanAnnotationContent(doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()).toString());
                        logger.info("annString: " + annString);
                        if (annotationType.equals("Pest")) {
                            pestAnnContent.add(annString);
                        }
                        if (annotationType.equals("Crop")) {
                            cropAnnContent.add(annString);
                        }
                        if (annotationType.equals("Location")) {
                            locationAnnContent.add(annString);
                        }
                        if (annotationType.equals("ImpactNumber")) {
                            impactNumberAnnContent.add(annString);
                        }
                        if (annotationType.equals("ImpactNumberUnit")) {
                            impactNumberUnitAnnContent.add(annString);
                        }
                        if (annotationType.equals("ImpactDirection")) {
                            impactDirectionAnnContent.add(annString);
                        }
                        if (annotationType.equals("YieldMention")) {
                            yieldMentionAnnContent.add(annString);
                        }
                    }
                }
            }



            allAnnotations = doc.getName() +
                        "\t" + sourceAnnotation +
                        // replace sentence line breaks with a whitespace
                        "\t" + getCleanAnnotationContent(doc.getContent().getContent(start, end).toString()) +
                        "\t" + pestAnnContent +
                        "\t" + cropAnnContent +
                        "\t" + locationAnnContent +
                        "\t" + impactNumberAnnContent +
                        "\t" + impactNumberUnitAnnContent +
                        "\t" + impactDirectionAnnContent +
                        "\t" + yieldMentionAnnContent +
                        "\n";
            // write row to the spreadsheet
            resultsWriter.write(allAnnotations);

            // reset lists
            pestAnnContent.clear();
            cropAnnContent.clear();
            locationAnnContent.clear();
            impactNumberAnnContent.clear();
            impactNumberUnitAnnContent.clear();
            impactDirectionAnnContent.clear();
            yieldMentionAnnContent.clear();

        }

    }


    private String getCleanAnnotationContent(String originalAnnotationContent) {
        // New line Unix and Windows
        final String NEW_LINE_UNIX = "\n";
        final String NEW_LINE_WINDOWS = "\r\n";

        if (originalAnnotationContent.contains(NEW_LINE_WINDOWS)){
            return originalAnnotationContent.replace(NEW_LINE_WINDOWS, " ").trim();
        } else if (originalAnnotationContent.contains(NEW_LINE_UNIX)){
            return originalAnnotationContent.replace(NEW_LINE_UNIX, " ").trim();
        } else
            return originalAnnotationContent;
    }

    public static void main(String[] args) throws Exception {
        // CooccurrenceMatcher.jape uses annotationTypes to annotate Sentences with various windows-length
        // Coo_Export_1_sent indicates Cooccurrence in sentences of windows-1 i.e. each sentence
        String sourceAnnotationType = "Coo_Export_1_sent";
        // Annotations to export as csv columns for each sentence above
        //String[] targetAnnotationTypes = new String[]{"Pest", "Crop", "Location", "ImpactNumberUnit", "ImpactDirection", "YieldMention"};
        //String[] targetAnnotationTypes = new String[]{"Pest", "Crop", "Location", "ImpactNumberUnit"};
        //String[] targetAnnotationTypes = new String[]{"Pest", "Crop", "Location"};
        String[] targetAnnotationTypes = new String[]{"Pest", "Crop"};
        //String[] targetAnnotationTypes = new String[]{"Pest"};


        // target annotations contained in a sentence as a list
        List<String> containedAnnotationTypes = Arrays.asList(targetAnnotationTypes);

        // GATE Annotated XML files generated from the pipeline execution
        // Linux path
        String xmlOutputDirName = "/home/sadnan/corpus/croploss/classified/annotatedXML";
        // Windows path
        // String xmlOutputDirName = "C:\\Users\\manirm\\Documents\\ProjectData\\croploss-textmining\\corpus\\classified\\annotatedOutput";

        BufferedWriter annBuffWriter = new BufferedWriter(new FileWriter("annotations.csv", false));

        AnnotationExporter annotationExporter = new AnnotationExporter();
        // Old implementation
        // AnnotationExporter.export2CSV(xmlOutputDirName, annBuffWriter);
        // Latest implementation
        annotationExporter.exportAnnotations2CSV(sourceAnnotationType, containedAnnotationTypes, xmlOutputDirName, annBuffWriter);

    }


}
