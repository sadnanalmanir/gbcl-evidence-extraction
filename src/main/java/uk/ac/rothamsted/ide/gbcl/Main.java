package uk.ac.rothamsted.ide.gbcl;

import gate.Factory;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    static Set<String> processOnlyDocuments = new HashSet<>(Collections.singletonList(
            "x"
    ));

    /**
     * Accepts the command line arguments. For correct arguments invoke the method for running the text mining pipeline.
     * @param args formal arguments with values
     */
    public static void main(String[] args) {

        if (args.length > 0) {
            if (args.length > 1) {
                String arguments = Arrays.toString(args).replace(", ", " ");
                arguments = arguments.substring(1, arguments.length() - 1);
                logger.info("ARGUMENTS: " + arguments);
                runGbclEvidenceExtraction(arguments);
            } else {
                System.out.println("Insufficient number of arguments. See README.");
            }
        } else {
            System.out.println("No arguments submitted! See README.");
        }
        System.out.println("\nAll done.");
    }

    /**
     * Verify the arguments and start the pipeline if they are correct.
     * @param args Arguments to run the pipeline.
     */
    private static void runGbclEvidenceExtraction(String args) {


        try {
            long time = System.currentTimeMillis();

            String inputFileOrDirName;
            String xmlOutputDirName;

            String annotationResultsFileName;

            boolean runPipe;
            boolean runExport;

            {
                JSAP jsap = new JSAP();
                {
                    FlaggedOption s = new FlaggedOption("input").setStringParser(JSAP.STRING_PARSER).setLongFlag("input").setShortFlag('i');
                    s.setHelp("The name of input file or directory.");
                    jsap.registerParameter(s);
                }
                {
                    FlaggedOption s = new FlaggedOption("xmldir").setStringParser(JSAP.STRING_PARSER).setLongFlag("xmldir").setShortFlag('x');
                    s.setHelp("The name of xml file or directory.");
                    jsap.registerParameter(s);
                }
                {
                    FlaggedOption s = new FlaggedOption("annotationResults").setStringParser(JSAP.STRING_PARSER).setLongFlag("annotation").setShortFlag('o');
                    s.setHelp("Name of the CSV file for saving annotation results.");
                    jsap.registerParameter(s);
                }
                {
                    Switch s = new Switch("runPipe").setLongFlag("pipe");
                    s.setHelp("runPipe");
                    jsap.registerParameter(s);
                }
                {
                    Switch s = new Switch("runExport").setLongFlag("export");
                    s.setHelp("runExport");
                    jsap.registerParameter(s);
                }

                // Parse the command-line arguments.
                JSAPResult config = jsap.parse(args);

                // Help messages
                if (!config.success()) {
                    System.err.println();
                    System.err.println(" " + jsap.getUsage());
                    System.err.println();
                    System.err.println(jsap.getHelp());
                    System.exit(1);
                }

                inputFileOrDirName = config.getString("input");
                if (inputFileOrDirName != null) {
                    logger.info("corpus path: " + inputFileOrDirName);
                }

                xmlOutputDirName = config.getString("xmldir");
                if (xmlOutputDirName != null) {
                    logger.info("xml output path: " + xmlOutputDirName);
                }

                annotationResultsFileName = config.getString("annotationResults");
                if (annotationResultsFileName != null) {
                    logger.info("annotationResults File path: " + annotationResultsFileName);
                }

                runPipe = config.getBoolean("runPipe");
                if (runPipe) {
                    logger.info("runPipe: " + true);
                }

                runExport = config.getBoolean("runExport");
                if (runExport) {
                    logger.info("runExport: " + true);
                }


                // Create xml output directory if it does not exist.
                File xmlOutputDir = null;
                if (xmlOutputDirName != null) {
                    xmlOutputDir = Utils.createFileOrDirectoryIfNotExist(xmlOutputDirName);
                }

                // RUN PIPELINE
                if (runPipe) {
                    // Set GATE-X.X installation directory from project.properties in /src/main/resources
                    Properties pro = new Properties();
                    try {
                        pro.load(Files.newInputStream(new File(logger.getClass().getClassLoader().getResource("project.properties").toURI()).toPath()));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    String gateHome = pro.getProperty("GATE_HOME");

                    // Initialize pipeline.
                    Pipeline pipeline = new Pipeline(gateHome);
                /*
                 boolean runDocumentResetter,
                 boolean runTokenizer,
                 boolean runGazetteer,
                 boolean runSentenceSplitter,
                 boolean runPosTagger,
                 boolean runSemanticTagger,
                 boolean runCooccurrenceExtractor
                 */
                    pipeline.init(
                            true,
                            true,
                            true,
                            true,
                            true,
                            true,
                            true
                    );

                    //
                    // Prepare and Process corpus.
                    //
                    Map<String, String> fileIndex = Utils.initFileIndex(inputFileOrDirName);

                    // Monitor which file is currently processed.
                    int numberOfFilesToProcess = fileIndex.size();

                    if (processOnlyDocuments.size() > 1) {
                        numberOfFilesToProcess = processOnlyDocuments.size() - 1;
                    }
                    int numberOfFileProcessed = 0;

                    for (String fileName : fileIndex.keySet()) {

                        if (processOnlyDocuments.size() > 1 && !processOnlyDocuments.contains(fileName)) {
                            continue;
                        }

                        numberOfFileProcessed++;

                        logger.info("Annotating \n===============================================================\n"
                                + "Document (" + numberOfFileProcessed + " of " + numberOfFilesToProcess + "): " + fileName + " AT " + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())

                                + "\n===============================================================\n");

                        gate.Document doc = Factory.newDocument(new URL(fileIndex.get(fileName)));
                        doc.setName(fileName);

                        //log.info(doc.getContent());
                        pipeline.execute(doc);

                        if (xmlOutputDir != null) {
                            Utils.saveGateXml(doc, new File(xmlOutputDir + "/" + doc.getName() + ".xml"), false);
                        }

                        Factory.deleteResource(doc);

                        logger.info("Processing Document Time " + (System.currentTimeMillis() - time) / 1000 + " seconds = " + (System.currentTimeMillis() - time) / 1000 / 60 + " minutes");
                    }

                    logger.info("Processing Corpus Time " + (System.currentTimeMillis() - time) / 1000 + " seconds");
                }

                // Run annotation exporter
                if (runExport) {
                    // CooccurrenceMatcher.jape uses annotationTypes to annotate Sentences with various windows-length
                    // Coo_Export_1_sent indicates Cooccurrence in sentences of windows-1 i.e. each sentence
                    String sourceAnnotationType = "Coo_Export_1_sent";
                    // Annotations to export as csv columns for each sentence above
                    String[] targetAnnotationTypes = new String[]{"Pest", "Crop", "Location", "ImpactNumber", "ImpactNumberUnit", "ImpactDirection", "YieldMention"};
                    //String[] targetAnnotationTypes = new String[]{"Pest", "Crop", "Location", "ImpactNumberUnit"};
                    //String[] targetAnnotationTypes = new String[]{"Pest", "Crop", "Location"};
                    //String[] targetAnnotationTypes = new String[]{"Pest", "Crop"};
                    //String[] targetAnnotationTypes = new String[]{"Pest"};
                    //String[] targetAnnotationTypes = new String[]{"Pest", "ImpactNumberUnit", "ImpactDirection", "YieldMention"};


                    // target annotations contained in a sentence as a list
                    List<String> containedAnnotationTypes = Arrays.asList(targetAnnotationTypes);

                    // GATE Annotated XML files generated from the pipeline execution
                    // Linux path
                    // String xmlOutputDirName = "/home/sadnan/corpus/croploss/classified/annotatedXML";
                    // Windows path
                    // String xmlOutputDirName = "C:\\Users\\manirm\\Documents\\ProjectData\\croploss-textmining\\corpus\\classified\\annotatedOutput";

                    assert annotationResultsFileName != null;
                    BufferedWriter annBuffWriter = new BufferedWriter(new FileWriter(annotationResultsFileName, false));

                    AnnotationExporter annotationExporter = new AnnotationExporter();
                    // Old implementation
                    // AnnotationExporter.export2CSV(xmlOutputDirName, annBuffWriter);
                    // Latest implementation
                    annotationExporter.exportAnnotations2CSV(sourceAnnotationType, containedAnnotationTypes, xmlOutputDirName, annBuffWriter);
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}