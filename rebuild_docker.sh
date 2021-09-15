set -v
docker rmi gregorybrooks/better-neural-query-processor:1.0.0
docker build -t gregorybrooks/better-neural-query-processor:1.0.0 .
