export CLASSPATH=/communication-engine/communication-engine.jar
java -server -Dfile.encoding=UTF-8 -cp $CLASSPATH -Xms${Xms} -Xmx${Xmx} \
-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=2 -XX:ConcGCThreads=2 -XX:InitiatingHeapOccupancyPercent=70 \
-DLog4j2ContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector \
-Darchaius.configurationSource.additionalUrls=file:///communication-engine/config_${ENV}.properties \
-Dlog4j.configurationFile=/communication-engine/log4j2.xml \
farm.nurture.communication.engine.Application