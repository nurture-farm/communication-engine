FROM alpine:latest

ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
RUN apk add openjdk11

# Has to be set explictly to find binaries 
ENV PATH=$PATH:${JAVA_HOME}/bin
ARG BUILD_FOR=prod

LABEL Description="This image is used to start communication engine" Vendor="nurture.farm" Version="1.0"

RUN apk update && apk add tzdata
RUN cp /usr/share/zoneinfo/Asia/Kolkata  /etc/localtime
RUN echo "Asia/Kolkata" >  /etc/timezone

# Create a folder for staging a captain build
RUN mkdir -p /communication-engine/downloads

COPY target/communication-engine.jar /communication-engine/communication-engine.jar
COPY config/* /communication-engine/
COPY start.sh /communication-engine/
COPY sample_doc.pdf /communication-engine/
COPY sample_img.png /communication-engine/
COPY sample_video.mp4 /communication-engine/
RUN chmod a+x /communication-engine/start.sh

# This is the prometheus port
EXPOSE 8000

# Start the server
CMD ["sh", "/communication-engine/start.sh"]
