package edu.umass.ciir;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LineOrientedPythonDaemon {
    private  ProcessBuilder sentenceProcessBuilder = null;
    private  Process sentenceProcess = null;
    private  BufferedWriter sentenceCalledProcessStdin = null;
    private  BufferedReader sentenceCalledProcessStdout = null;

    LineOrientedPythonDaemon(String programDirectory, String programName) {
            this(programDirectory, programName, "", "");
    }

    LineOrientedPythonDaemon(String programDirectory, String programName, String parameter1) {
        this(programDirectory, programName, parameter1, "");
    }

    LineOrientedPythonDaemon(String programDirectory, String programName, String parameter1, String parameter2) {
        try {
            System.out.println("Starting " + programDirectory + programName + " " + parameter1 + " " + parameter2);
            sentenceProcessBuilder = new ProcessBuilder("python3",
                    programDirectory + programName, parameter1, parameter2);
            sentenceProcessBuilder.directory(new File(programDirectory));
            sentenceProcess = sentenceProcessBuilder.start();

            sentenceCalledProcessStdin = new BufferedWriter(
                    new OutputStreamWriter(sentenceProcess.getOutputStream()));
            sentenceCalledProcessStdout = new BufferedReader(
                    new InputStreamReader(sentenceProcess.getInputStream()));

            System.out.println("Waiting for daemon to be READY");
            String line;
            while ((line = sentenceCalledProcessStdout.readLine()) != null) {
                if (line.equals("READY")) {
                    break;
                }
            }
            System.out.println("Daemon is READY");

        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

    public  synchronized List<String> getAnswers(String text) {
        List<String> phrases = new ArrayList<String>();
        try {
            sentenceCalledProcessStdin.write(text + "\n");
            sentenceCalledProcessStdin.write("EOD\n");
            sentenceCalledProcessStdin.flush();

            String line;
            while ((line = sentenceCalledProcessStdout.readLine()) != null) {
                if (line.equals("EOL")) {
                    break;
                }
                phrases.add(line);
            }
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
        return phrases;
    }

    public  synchronized List<String> getAnswers(List<String> lines) {
        List<String> phrases = new ArrayList<String>();
        try {
            for (String line : lines) {
                sentenceCalledProcessStdin.write(line + "\n");
            }
            sentenceCalledProcessStdin.write("EOD\n");
            sentenceCalledProcessStdin.flush();

            String line;
            while ((line = sentenceCalledProcessStdout.readLine()) != null) {
                if (line.equals("EOL")) {
                    break;
                }
                phrases.add(line);
            }
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
        return phrases;
    }

    // No need to do this, let it run until the whole program ends
    public  void stop() {
        try {
            sentenceProcess.destroy();
        } catch (Exception e) {
            throw new BetterQueryBuilderException(e);
        }
    }

}
