FROM alpine:latest

RUN apk add --no-cache \
    openjdk8 \
    cmake \
    make \
    g++ \
    gcc \
    linux-headers \
    proj-dev \
    gdal-dev \
    git \
    curl \
    swig \
    unzip \
    bash \
    maven

ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk

ENV LD_LIBRARY_PATH /usr/lib:${LD_LIBRARY_PATH}

# Install GDAL
ARG GDAL_VERSION=3.6.2
RUN mkdir -p /opt/gdal \
 && cd /opt/gdal \
 && curl -LOs https://download.osgeo.org/gdal/${GDAL_VERSION}/gdal-${GDAL_VERSION}.tar.gz \
 && tar xzf gdal-${GDAL_VERSION}.tar.gz \
 && mkdir build \
 && cd build \
 && cmake \
    -DBUILD_JAVA_BINDINGS=ON \
    -DJAVA_AWT_INCLUDE_PATH=$JAVA_HOME/include \
    -DJAVA_AWT_LIBRARY=$JAVA_HOME/jre/lib/amd64 \
    -DJAVA_JVM_LIBRARY=$JAVA_HOME/jre/lib/amd64/server \
    -DJAVA_INCLUDE_PATH=$JAVA_HOME/include \
    -DJAVA_INCLUDE_PATH2=$JAVA_HOME/include/linux \
    -DJAVA_AWT_LIBRARY=$JAVA_HOME/lib/amd64 \
    -DJAVA_HOME=$JAVA_HOME \
    -DCMAKE_INSTALL_PREFIX:PATH=${INSTALL_PREFIX} \
    -DCMAKE_PREFIX_PATH:PATH=${INSTALL_PREFIX} \
    -DBUILD_SHARED_LIBS:BOOL=ON \
    -DCMAKE_BUILD_TYPE=Release \
    -DPython_ROOT_DIR=/usr/local \
    ../gdal-${GDAL_VERSION} \
 && make -j$(nproc) \
 && make install

COPY . /Sen2vm/sen2vm-core

# Copy all needed JAR :
# - orekit v11.3.2
# - rugged v4.0.1
# - SXGEO v0.1.13
# - gdal 3.6.2
RUN mkdir -p /root/.m2/repository/org/ \
    && cp -R /Sen2vm/sen2vm-core/jar/* /root/.m2/repository/org/

# Make a symlink in /usr/lib so we don't need LD_LIBRARY_PATH to load it dynamically from Java.
RUN ln -sf /root/.m2/repository/org/gdal/gdal/3.6.2/libgdalalljni.so  /usr/lib

WORKDIR /Sen2vm/sen2vm-core

CMD ["sh"]
