set -v
set -o allexport
source /mnt/scratch/BETTER/BETTER_TEST_ENVIRONMENTS/BETTER_MITRE_EVAL_JAN_2021/clear_ir.env

MODE=AUTO
INPUTFILE=AUTO.analytic_tasks.json

java -jar target/BetterNeuralQueryProcessor-1.0.0.jar $scratchFileLocation/eventextractorfiles/$INPUTFILE $MODE $PWD/ $scratchFileLocation/logfiles/AUTO/  /mnt/scratch/BETTER/BETTER_TEST_ENVIRONMENTS/BETTER_MITRE_EVAL_JAN_2021/corpus/arabic arabic-corpus.jl ${scratchFileLocation}runfiles
