mvn clean install -D skipTests
docker build -t platform/communication-engine:$1 .
