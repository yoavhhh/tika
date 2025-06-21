# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

# "random" uid/gid hopefully not used anywhere else
# This needs to be set globally and then referenced in
# the subsequent stages -- see TIKA-3912
ARG UID_GID="35002:35002"

FROM ubuntu:plucky AS base

ARG UID_GID
ARG TIKA_VERSION
ARG JRE='openjdk-17-jdk'

RUN apt-get update && \
    apt-get install -y $JRE

# Set Java 17 as default
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

# Verify
RUN java -version

FROM base AS runtime

ARG UID_GID
ARG JRE

RUN set -eux \
    && apt-get install --yes --no-install-recommends gnupg2 software-properties-common \
    && apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends $JRE \
        gdal-bin \
        imagemagick \
        tesseract-ocr \
        tesseract-ocr-eng \
        tesseract-ocr-ita \
        tesseract-ocr-fra \
        tesseract-ocr-spa \
        tesseract-ocr-deu \
        tesseract-ocr-jpn \
    && echo ttf-mscorefonts-installer msttcorefonts/accepted-mscorefonts-eula select true | debconf-set-selections \
    && DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends \
        xfonts-utils \
        fonts-freefont-ttf \
        fonts-liberation \
        ttf-mscorefonts-installer \
        wget \
        cabextract \
    && apt-get clean -y \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# RUN apt-get update && apt-get install -y libtsk19 && apt-get install -y sleuthkit

# Install dependencies
RUN apt-get update && apt-get install -y \
    autoconf \
    automake \
    libtool \
    libsqlite3-dev \
    libssl-dev \
    libz-dev \
    libbz2-dev \
    flex \
    bison \
    libafflib-dev \
    libewf-dev \
    libcppunit-dev \
    wget \
    build-essential \
    git \
    ant \
    openjdk-17-jdk \
    && rm -rf /var/lib/apt/lists/*

# Download and install SleuthKit 4.10.1
RUN wget https://github.com/sleuthkit/sleuthkit/releases/download/sleuthkit-4.10.1/sleuthkit-4.10.1.tar.gz && \
    tar -xzf sleuthkit-4.10.1.tar.gz && \
    cd sleuthkit-4.10.1 && \
    # ./bootstrap && \
    ./configure --with-afflib --with-ewf --with-vhdi --with-vmdk --with-bfio --enable-java && \
    make -j$(nproc) && \
    make install && \
    ldconfig && \
    cd .. && rm -rf sleuthkit-4.10.1 sleuthkit-4.10.1.tar.gz

ARG TIKA_VERSION
ENV TIKA_VERSION=$TIKA_VERSION

COPY tika-app/target/tika-app-${TIKA_VERSION}.jar /tika-app-${TIKA_VERSION}.jar
COPY tika-server/tika-server-standard/target/tika-server-standard-${TIKA_VERSION}.jar /tika-server-standard-${TIKA_VERSION}.jar

COPY lib/ /opt/tika/lib/
ENV LD_LIBRARY_PATH=/opt/tika/lib/

# COPY pom.xml /tmp/install_jars/pom.xml
# COPY tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-fs-module/lib/sleuthkit-4.10.1.jar /tmp/install_jars/jars/
# RUN apt-get update && apt-get install -y --no-install-recommends maven \
#     && cd /tmp/install_jars \
#     && mvn install:install-file -Dfile=./jars/sleuthkit-4.10.1.jar -DgroupId='org.sleuthkit' -DartifactId=sleuthkit -Dversion='4.10.1' -Dpackaging=jar \
#     && cd / \
#     && rm -rf /tmp/install_jars \
#     && apt-get purge -y --auto-remove maven \
#     && rm -rf /root/.m2

USER $UID_GID

EXPOSE 9998
ENTRYPOINT [ "/bin/sh", "-c", "exec java  -Djava.library.path=/opt/tika/lib/ -cp \"/tika-server-standard-${TIKA_VERSION}.jar:/tika-extras/*\" org.apache.tika.server.core.TikaServerCli -h 0.0.0.0 $0 $@" ]
