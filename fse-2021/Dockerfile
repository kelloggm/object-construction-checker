FROM ubuntu:20.04

LABEL Narges Shadab <nshad001@ucr.edu>

# Install basic software support
RUN apt-get update && \
    apt-get install --yes software-properties-common

# Install Java 11
RUN apt-get update && \
    apt-get install -y openjdk-11-jdk && \
    apt-get install -y ant && \
    apt-get clean;

# Install Java 8
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;

RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

# Install required softwares (curl & zip & wget)
RUN apt-get install curl -y
RUN apt-get install zip -y
RUN apt-get install wget -y

# update
RUN apt-get update -y

# Install python
RUN apt-get install -y python

# Install git
RUN apt-get install -y git

# Install Maven
ARG MAVEN_VERSION=3.6.3
ARG SHA=c35a1803a6e70a126e80b2b3ae33eed961f83ed74d18fcd16909b2d44d7dada3203f1ffe726c17ef8dcca2dcaa9fca676987befeadc9b9f759967a8cb77181c0
ARG BASE_URL=https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && echo "Downlaoding maven" \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  \
  && echo "Checking download hash" \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  \
  && echo "Unziping maven" \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  \
  && echo "Cleaning and setting links" \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven

#Install Gradle
ARG GRADLE_VERSION=6.8.3
ARG GRADLE_BASE_URL=https://services.gradle.org/distributions
ARG GRADLE_SHA=7faa7198769f872826c8ef4f1450f839ec27f0b4d5d1e51bade63667cbccd205
RUN mkdir -p /usr/share/gradle /usr/share/gradle/ref \
  && echo "Downlaoding gradle hash" \
  && curl -fsSL -o /tmp/gradle.zip ${GRADLE_BASE_URL}/gradle-${GRADLE_VERSION}-bin.zip \
  \
  && echo "Checking download hash" \
  && echo "${GRADLE_SHA}  /tmp/gradle.zip" | sha256sum -c - \
  \
  && echo "Unziping gradle" \
  && unzip -d /usr/share/gradle /tmp/gradle.zip \
   \
  && echo "Cleaning and setting links" \
  && rm -f /tmp/gradle.zip \
  && ln -s /usr/share/gradle/gradle-${GRADLE_VERSION} /usr/bin/gradle
ENV GRADLE_VERSION 6.8.3
ENV GRADLE_HOME /usr/bin/gradle
#ENV GRADLE_USER_HOME /cache
ENV PATH $PATH:$GRADLE_HOME/bin
#VOLUME $GRADLE_USER_HOME

# Install scc
ARG SCC_3_SHA=04f9e797b70a678833e49df5e744f95080dfb7f963c0cd34f5b5d4712d290f33
RUN mkdir -p /usr/share/scc \
  && echo "Downloading scc" \
  && curl -fsSL -o /tmp/scc.zip https://github.com/boyter/scc/releases/download/v3.0.0/scc-3.0.0-arm64-unknown-linux.zip \
  \
  && echo "Checking download hash" \
  && echo "${SCC_3_SHA} /tmp/scc.zip" | sha256sum -c - \
  \
  && echo "Unzipping scc" \
  && unzip -d /usr/share/scc /tmp/scc.zip \
  \
  && echo "Cleaning and setting links" \
  && rm -f /tmp/scc.zip \
  && ln -s /usr/share/scc/scc /usr/bin/scc

## Create a new user
RUN useradd -ms /bin/bash fse && \
    apt-get install -y sudo && \
    adduser fse sudo && \
    echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
USER fse
WORKDIR /home/fse

ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA_HOME

ENV JAVA8_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA8_HOME

ENV JAVA11_HOME /usr/lib/jvm/java-11-openjdk-amd64/
RUN export JAVA11_HOME

ENV OCC_BRANCH master
ENV OCC_REPO https://github.com/kelloggm/object-construction-checker.git

ENV PU_REPO=https://github.com/kelloggm/plume-util.git

ENV ZK_REPO https://github.com/kelloggm/zookeeper.git
ENV ZK_CMD "mvn --projects zookeeper-server --also-make clean install -DskipTests"
ENV ZK_CLEAN "mvn clean"

ENV HADOOP_REPO https://github.com/Nargeshdb/hadoop
ENV HADOOP_CMD "mvn --projects hadoop-hdfs-project/hadoop-hdfs --also-make clean compile -DskipTests"
ENV HADOOP_CLEAN "mvn clean"

ENV HBASE_REPO https://github.com/Nargeshdb/hbase
ENV HBASE_CMD "mvn --projects hbase-server --also-make clean install -DskipTests"
ENV HBASE_CLEAN "mvn clean"

# download ResourceLeakChecker
RUN git clone "${OCC_REPO}"

RUN cd object-construction-checker \
    && git checkout "${OCC_BRANCH}" \
    && ./gradlew install \
    && cd ..

RUN cp object-construction-checker/experimental-machinery/ablation/*.sh .
RUN cp object-construction-checker/experimental-machinery/case-studies/*.sh .

# download plume-util
RUN git clone "${PU_REPO}"
RUN cd plume-util \
    && git checkout no-suppressions \
    && cd ..

# download Zookeeper
RUN git clone "${ZK_REPO}"
RUN cd zookeeper \
    && git checkout no-suppressions \
    && cd ..

# download Hadoop
RUN git clone "${HADOOP_REPO}"
RUN cd hadoop \
    && git checkout no-suppressions \
    && cd ..

# download HBase
RUN git clone "${HBASE_REPO}"
RUN cd hbase \
    && git checkout no-suppressions \
    && cd ..

# analyze all the benchmarks once to populate local Maven repository
# our checker emits errors for plume-util and zookeeper, hence the '|| true'
RUN (./run-always-call-on-plume-util.sh > plume-util-out 2>&1) || true
RUN (./run-always-call-on-zookeeper.sh > zookeeper-out 2>&1) || true
RUN ./run-always-call-on-hadoop.sh > hadoop-out 2>&1
RUN ./run-always-call-on-hbase.sh > hbase-out 2>&1

# switch back to with-annotations branch
RUN cd plume-util \
    && git checkout with-annotations \
    && cd ..

RUN cd zookeeper \
    && git checkout with-annotations \
    && cd ..

RUN cd hadoop \
    && git checkout with-annotations \
    && cd ..

RUN cd hbase \
    && git checkout with-annotations \
    && cd ..

CMD ["/bin/bash"]
