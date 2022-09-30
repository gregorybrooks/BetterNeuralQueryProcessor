package edu.umass.ciir;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class BetterNeuralQueryProcessor {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderZ");
    private String analyticTasksFile;
    private String mode;
    private String logFileLocation;
    private String programDirectory;

    private String corpusFileLocation;
    private String targetCorpusFileName;
    private String runFileLocation;
    private TranslatorInterface translator;

    BetterNeuralQueryProcessor(String analyticTasksFile, String mode, String programDirectory, String logFileLocation,
                               String corpusFileLocation, String targetCorpusFileName, String runFileLocation) {
        this.mode = mode;
        this.analyticTasksFile = analyticTasksFile;
        this.logFileLocation = logFileLocation;
        this.programDirectory = programDirectory;
        this.corpusFileLocation = corpusFileLocation;
        this.targetCorpusFileName = targetCorpusFileName;
        this.runFileLocation = runFileLocation;
    }

    /**
     * Configures the logger for this program.
     * @param logFileName Name to give the log file.
     */
    private void configureLogger(String logFileName) {
        SimpleFormatter formatterTxt;
        FileHandler fileTxt;
        try {
            // suppress the logging output to the console
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            if (handlers[0] instanceof ConsoleHandler) {
                rootLogger.removeHandler(handlers[0]);
            }
            logger.setLevel(Level.INFO);
            fileTxt = new FileHandler(logFileName);
            // create a TXT formatter
            formatterTxt = new SimpleFormatter();
            fileTxt.setFormatter(formatterTxt);
            logger.addHandler(fileTxt);
        } catch (Exception cause) {
            throw new BetterQueryBuilderException(cause);
        }
    }

    /**
     * Sets up logging for this program.
     */
    public void setupLogging() {
        String logFileName = logFileLocation + "/better-neural-query-processor.log";
        configureLogger(logFileName);
    }

    List<Task> tasks = new ArrayList<>();

    private String getOptionalValue(JSONObject t, String field) {
        if (t.containsKey(field)) {
            return (String) t.get(field);
        } else {
            return "";
        }
    }

    private void readTaskFile() {
        try {
            logger.info("Reading analytic tasks info file " + analyticTasksFile);

            Reader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(analyticTasksFile)));
            JSONParser parser = new JSONParser();
            JSONArray tasksJSON = (JSONArray) parser.parse(reader);
            for (Object oTask : tasksJSON) {
                JSONObject t = (JSONObject) oTask;
                String taskNum = (String) t.get("task-num");
                String taskTitle = getOptionalValue(t, "task-title");
                String taskStmt = getOptionalValue(t, "task-stmt");
                String taskNarr = getOptionalValue(t, "task-narr");
                JSONObject taskDocMap = (JSONObject) t.get("task-docs");
                List<ExampleDocument> taskExampleDocuments = new ArrayList<>();
                for (Object value : taskDocMap.keySet()) {
                    String entryKey = (String) value;
                    JSONObject taskDoc = (JSONObject) taskDocMap.get(entryKey);
                    String highlight = getOptionalValue(taskDoc, "highlight");
                    String docText = (String) taskDoc.get("doc-text");
                    String docID = (String) taskDoc.get("doc-id");
                    List<String> sentences = new ArrayList<>();
                    JSONArray jsonSentences = (JSONArray) taskDoc.get("sentences");
                    for (Object jsonObjectSentenceDescriptor : jsonSentences) {
                        JSONObject jsonSentenceDescriptor = (JSONObject) jsonObjectSentenceDescriptor;
                        long start = (long) jsonSentenceDescriptor.get("start");
                        long end = (long) jsonSentenceDescriptor.get("end");
                        String sentence = docText.substring((int) start, (int) end);
                        sentences.add(sentence);
                    }
                    taskExampleDocuments.add(new ExampleDocument(docID, docText, highlight, sentences));
                }
                JSONArray taskRequests = (JSONArray) t.get("requests");
                List<Request> requests = new ArrayList<>();
                for (Object o : taskRequests) {
                    JSONObject request = (JSONObject) o;
                    String reqText = getOptionalValue(request, "req-text");
                    String reqNum = (String) request.get("req-num");
                    JSONObject requestDocMap = (JSONObject) request.get("req-docs");
                    List<ExampleDocument> requestExampleDocuments = new ArrayList<>();
                    for (Object value : requestDocMap.keySet()) {
                        String entryKey = (String) value;
                        JSONObject reqDoc = (JSONObject) requestDocMap.get(entryKey);
                        String highlight = getOptionalValue(reqDoc, "highlight");
                        String docText = (String) reqDoc.get("doc-text");
                        String docID = (String) reqDoc.get("doc-id");

                        List<String> sentences = new ArrayList<>();
                        JSONArray jsonSentences = (JSONArray) reqDoc.get("sentences");
                        for (Object jsonObjectSentenceDescriptor : jsonSentences) {
                            JSONObject jsonSentenceDescriptor = (JSONObject) jsonObjectSentenceDescriptor;
                            long start = (long) jsonSentenceDescriptor.get("start");
                            long end = (long) jsonSentenceDescriptor.get("end");
                            String sentence = docText.substring((int) start, (int) end);
                            sentences.add(sentence);
                        }

                        requestExampleDocuments.add(new ExampleDocument(docID, docText, highlight, sentences));
                    }
                    requests.add(new Request(reqNum, reqText, requestExampleDocuments));
                }
                tasks.add(new Task(taskNum, taskTitle, taskStmt, taskNarr, taskExampleDocuments, requests));
            }
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

    private void executeQuery(Task t, Request r, EmbeddingsIndex index, Writer writer) {
        logger.info("Executing query for task " + t.taskNum + ", request " + r.reqNum);

        try {
            Set sentenceSet = t.getExampleDocSentences();
            sentenceSet.addAll(r.getExampleDocSentences());
            List<String> sentences = new ArrayList<>(sentenceSet);
            List<String> translatedSentences = translator.getTranslations(sentences);

            List<String> hits = index.runQuery(translatedSentences);

            int rank = 1;
            for (String hit : hits) {
                String[] x = hit.split(" ");
                String docid = x[0];
                String score = x[1];
                writer.write(r.reqNum + " " + "Q0 " + docid + " " + rank + " " + score + " CLEAR\n");
                ++rank;
            }
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

    private void processQueries() {
        try {
            // device should be either "cpu" or "cuda:0"
            translator = new MarianTranslator(programDirectory, "ARABIC", "cpu");

            EmbeddingsIndex index = new EmbeddingsIndex(programDirectory, corpusFileLocation, targetCorpusFileName);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(runFileLocation + "/"+ mode + ".neural.out")));

            readTaskFile();
            // for every Request, get its example docs and get all their sentences, then call index.search with
            // those sentences. Get back the docids and scores, write them to the output runfile
            for (Task t : tasks) {
                for (Request r : t.getRequests()) {
                    if (r.reqNum.equals("IR-T3-r2")) {
                        executeQuery(t, r, index, writer);
                    }
                }
            }
            writer.close();
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

    private static String ensureTrailingSlash(String s) {
        if (!s.endsWith("/")) {
            return s + "/";
        } else {
            return s;
        }
    }

    public static void main (String[] args) {
        for (int x = 0; x < args.length; ++x) {
            System.out.println(x + ": " + args[x]);
        }
        String analyticTasksFile = args[0];
        String mode = args[1];
        String programDirectory = args[2];
        programDirectory = ensureTrailingSlash(programDirectory);
        String logFileLocation = args[3];
        String corpusFileLocation = args[4];
        String targetCorpusFileName = args[5];
        String runFileLocation = args[6];

        BetterNeuralQueryProcessor betterIR = new BetterNeuralQueryProcessor(analyticTasksFile, mode,
                programDirectory, logFileLocation, corpusFileLocation, targetCorpusFileName, runFileLocation);
        betterIR.setupLogging();

        betterIR.processQueries();
    }
}
