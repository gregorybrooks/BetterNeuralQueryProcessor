package edu.umass.ciir;
import java.util.*;
import java.util.logging.Logger;

public class EmbeddingsIndex {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderZ");
    private String programDirectory = "./";
    private String targetCorpusDirectory = "./";
    private String targetCorpusFileName = "";
    private String program = "";

    private LineOrientedPythonDaemon embeddingsIndexDaemon = null;

    private void startDaemon() {
        logger.info("Starting embeddings index daemon");
        embeddingsIndexDaemon = new LineOrientedPythonDaemon(programDirectory, program, targetCorpusDirectory);
    }

    // No need to do this, let it run until the whole program ends
    public void stopDaemon() {
        embeddingsIndexDaemon.stop();
    }

    EmbeddingsIndex(String programDirectory, String targetCorpusDirectory, String targetCorpusFileName) {
        if (!targetCorpusDirectory.endsWith("/")) {
            targetCorpusDirectory += "/";
        }

        this.programDirectory = programDirectory;
        this.targetCorpusDirectory = targetCorpusDirectory;
        this.targetCorpusFileName = targetCorpusFileName;
        this.program = "search.py";
        startDaemon();
    }

    public List<String> runQuery(List<String> querySentences) {
        return embeddingsIndexDaemon.getAnswers(querySentences);
    }

}
