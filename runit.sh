#!/bin/bash
set -v

java -jar target/BetterNeuralQueryProcessor-1.0.0.jar $eventExtractorFileLocation/$INPUTFILE $MODE $HOME $logFileLocation  $corpusFileLocation $targetCorpusFileName $runFileLocation

set +e
chmod -f a+rw $runFileLocation/*
# If the chmod had a problem, ignore it
exit 0
