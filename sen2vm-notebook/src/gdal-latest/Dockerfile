FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive


RUN apt-get update && apt-get install -y \
    build-essential \
    git \
    cmake \
    pkg-config \
    curl \
    wget \
    zip \
    libcurl4-openssl-dev \
    libproj-dev \
    proj-bin \
    libgeos-dev \
    libsqlite3-dev \
    libtiff-dev \
    libjpeg-dev \
    libpng-dev \
    libzstd-dev \
    liblzma-dev \
    libopenjp2-7-dev \
    libboost-dev \
    python3 \
    python3-pip \
    python3-dev \
    python3-numpy \
    swig \
    ninja-build \
    libhdf5-dev \
    libnetcdf-dev \
    libpq-dev \
    libxml2-dev


RUN git clone https://github.com/OSGeo/gdal.git /gdal && \
    cd /gdal && \
    git fetch --tags && \
    LATEST_TAG=$(git describe --tags `git rev-list --tags --max-count=1`) && \
    echo "Using GDAL version: $LATEST_TAG" && \
    git checkout $LATEST_TAG


RUN mkdir /gdal/build

RUN cd /gdal/build && \
    cmake .. \
    -GNinja \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_PYTHON_BINDINGS=ON \
    -DGDAL_USE_OPENJPEG=ON \
    -DGDAL_USE_TIFF_INTERNAL=ON \
    -DGDAL_USE_GEOTIFF_INTERNAL=ON \
    -DGDAL_USE_JPEG_INTERNAL=ON


RUN cd /gdal/build && ninja && ninja install

RUN ldconfig

RUN gdalinfo --version

ENTRYPOINT ["/bin/bash"]
