FROM lwieske/java-8:jdk-8u92-slim

ENV MAVEN_HOME="/usr/lib/maven"
ENV MAVEN_URL="http://apache.openmirror.de/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz"

RUN wget -q $MAVEN_URL && \
    tar xfz apache-maven-3.3.9-bin.tar.gz && \
    rm apache-maven-3.3.9-bin.tar.gz && \
    mv apache-maven-3.3.9 $MAVEN_HOME && \
    ln -s $MAVEN_HOME/bin/mvn /usr/bin/mvn

CMD ["/usr/bin/mvn"]
