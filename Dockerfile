FROM anapsix/alpine-java:8_server-jre

ARG GPDVIZ_VERSION

RUN  echo "GPDVIZ_VERSION=${GPDVIZ_VERSION}"
RUN  mkdir -p /opt/gpdviz/jar
COPY jvm/target/scala-2.12/gpdviz-assembly-${GPDVIZ_VERSION}.jar /opt/gpdviz/jar/gpdviz.jar
COPY docker/gpdviz.sh /opt/gpdviz/

VOLUME /opt/gpdviz/conf
EXPOSE 5050

WORKDIR    /opt/gpdviz
ENTRYPOINT ["./gpdviz.sh"]
CMD        []
