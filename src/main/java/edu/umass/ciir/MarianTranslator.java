package edu.umass.ciir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * A translator that uses Rab's translator.
 */
public class MarianTranslator implements TranslatorInterface {
    private static final Logger logger = Logger.getLogger("BetterQueryBuilderZ");

    private String programDirectory = "./";
    private String language = "";
    private String program = "";
    private String device = "";

    private LineOrientedPythonDaemon translatorDaemon = null;

    private void startDaemon() {
        logger.info("Starting translator daemon");
        translatorDaemon = new LineOrientedPythonDaemon(programDirectory,
                program, programDirectory, device);
    }

    // No need to do this, let it run until the whole program ends
    public void stopSpacy() {
        translatorDaemon.stop();
    }

    MarianTranslator(String programDirectory, String language, String device) {
        if (!programDirectory.endsWith("/")) {
            programDirectory += "/";
        }
        this.programDirectory = programDirectory;
        this.language = language;
        this.device = device;
        if (language.equals("ARABIC")) {
            this.program = "machine-translation-service/batch_translate.py";
        } else if (language.equals("FARSI")) {
            this.program = "persiannlp/batch_translate.py";
        }
        startDaemon();
    }

    // need to do this but only on the translated words, not on the Galago structured query
    // operators and their parameters
    // For now, omitting this filtering
    private static String filterCertainCharactersPostTranslation(String q) {
        if (q == null || q.length() == 0) {
            return q;
        }
        else {
            q = q.replaceAll("\\.", " ");  // with the periods the queries hang
            q = q.replaceAll("@", " ");  // Galago has @ multi-word token thing
            q = q.replaceAll("\"", " "); // remove potential of mis-matched quotes
            q = q.replaceAll("“", " ");  // causes Galago to silently run an empty query
            q = q.replaceAll("”", " ");
            return q;
        }
    }

    private List<String> callPythonTranslationProgram(List<String> strings, Integer numTranslationAlternatives,
                                                      String weighted)  {
        try {
            String program = "machine-translation-service/batch_translate.py";
            logger.info("Calling " + programDirectory + program + " " + programDirectory);
            ProcessBuilder processBuilder = new ProcessBuilder(
                    programDirectory + program, programDirectory);
            processBuilder.directory(new File(programDirectory));

            Process process = processBuilder.start();

            BufferedWriter called_process_stdin = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            BufferedReader called_process_stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            for (String s : strings) {
                called_process_stdin.write(s + "\n");
            }
            called_process_stdin.write("EOD\n");
            called_process_stdin.flush();
            called_process_stdin.close();

            List<String> phrases = new ArrayList<>();
            String line;
            while ((line = called_process_stdout.readLine()) != null) {
                phrases.add(line);
            }
            int exitVal = process.waitFor();
            if (exitVal != 0) {
                System.out.println("Unexpected exit value calling " + programDirectory + program);
                System.out.println("Input strings:");
                for (String x : strings) {
                    System.out.println(x);
                }
                if (phrases.size() > 0) {
                    System.out.println("Here are phrases returned so far:");
                    for (String x : phrases) {
                        System.out.println(x);
                    }
                }

                throw new BetterQueryBuilderException("Unexpected " + exitVal + " exit value calling " + programDirectory + program );
            }
            return phrases;
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

    @Override
    public List<String> getTranslationTerms (List<String> strings) {
        return callPythonTranslationProgram(strings, 2, "get_terms");
    }

    @Override
    public List<String> getSingleTranslationTerm(List<String> strings) {
        return callPythonTranslationProgram(strings, 1, "get_terms");
    }

     private int MAX_SENTENCES_PER_TRANSLATION = 10;

    @Override
    public String getTranslation(String text) {
        List<String> inputList = new ArrayList<>(Arrays.asList(text.split("[.]")));

        List<List<String>> parts = new ArrayList<>();
        final int N = inputList.size();
        for (int i = 0; i < N; i += MAX_SENTENCES_PER_TRANSLATION) {
            parts.add(new ArrayList<String>(
                    inputList.subList(i, Math.min(N, i + MAX_SENTENCES_PER_TRANSLATION)))
            );
        }

        String outputString = "";
        for (List<String> part : parts) {
            List<String> outputList;
            outputList = translatorDaemon.getAnswers(part);
            for (String phrase : outputList) {
                outputString += phrase + " ";
            }
        }
        return outputString;
    }

    @Override
    public List<String> getTranslations(List<String> inputList) {

        List<List<String>> parts = new ArrayList<>();
        final int N = inputList.size();
        for (int i = 0; i < N; i += MAX_SENTENCES_PER_TRANSLATION) {
            parts.add(new ArrayList<String>(
                    inputList.subList(i, Math.min(N, i + MAX_SENTENCES_PER_TRANSLATION)))
            );
        }

        List<String> finalList = new ArrayList<>();
        for (List<String> part : parts) {
            List<String> outputList;
            outputList = translatorDaemon.getAnswers(part);
            finalList.addAll(outputList);
        }
        return finalList;
    }
}
