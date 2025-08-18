# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

FROM sbtscala/scala-sbt:eclipse-temurin-jammy-11.0.17_8_1.9.3_2.13.11 AS build

# Set working directory
WORKDIR /core

# Copy all projects under core to /core
COPY core/ .

# Update system and install dependencies
RUN apt-get update && apt-get install -y \
    netcat \
    unzip \
    libpq-dev \
    && apt-get clean

WORKDIR /core
# Add .git for runtime calls to jgit from OPversion
COPY .git ../.git

RUN sbt clean WorkflowExecutionService/dist

# Unzip the texera binary
RUN unzip amber/target/universal/texera-*.zip -d amber/target/

FROM eclipse-temurin:11-jdk-jammy AS runtime

WORKDIR /core/amber

COPY --from=build /core/amber/r-requirements.txt /tmp/r-requirements.txt
COPY --from=build /core/amber/requirements.txt /tmp/requirements.txt
COPY --from=build /core/amber/operator-requirements.txt /tmp/operator-requirements.txt

# Install Python & R runtime dependencies
RUN apt-get update && apt-get install -y \
    python3-pip \
    python3-dev \
    python3-venv \
    libpq-dev \
    gfortran \
    curl \
    build-essential \
    libreadline-dev \
    libncurses-dev \
    libssl-dev \
    libxml2-dev \
    xorg-dev \
    libbz2-dev \
    liblzma-dev \
    libpcre++-dev \
    libpango1.0-dev \
     libcurl4-openssl-dev \
libfreetype6-dev \
libpng-dev \
libtiff5-dev \
libjpeg-dev \
    unzip \
    openssh-client \
    && apt-get clean

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget software-properties-common && \
    wget https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2204/x86_64/cuda-keyring_1.0-1_all.deb && \
    dpkg -i cuda-keyring_1.0-1_all.deb && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    cuda-toolkit-12-1 && \
    rm -rf /var/lib/apt/lists/*

ENV CUDA_HOME=/usr/local/cuda
ENV PATH="$CUDA_HOME/bin:$PATH"
ENV LD_LIBRARY_PATH="$CUDA_HOME/lib64:$LD_LIBRARY_PATH"

# Install R and needed libraries
ENV R_VERSION=4.3.3
RUN curl -O https://cran.r-project.org/src/base/R-4/R-${R_VERSION}.tar.gz && \
    tar -xf R-${R_VERSION}.tar.gz && \
    cd R-${R_VERSION} && \
    ./configure --prefix=/usr/local \
                --enable-R-shlib \
                --with-blas \
                --with-lapack && \
    make -j 4 && \
    make install && \
    cd .. && \
    rm -rf R-${R_VERSION}* && R --version && pip3 install --upgrade pip setuptools wheel && \
    pip3 install -r /tmp/requirements.txt && \
    pip3 install -r /tmp/operator-requirements.txt && \
    pip3 install -r /tmp/r-requirements.txt && \
    pip3 install \
        scanpy==1.11.1 \
        wandb==0.19.11 \
        ipython==8.36.0 \
        torch==2.2.2 \
        torchtext==0.17.2 \
        scgpt==0.2.4
RUN Rscript -e "options(repos = c(CRAN = 'https://cran.r-project.org')); \
                install.packages(c('coro', 'arrow', 'dplyr'), \
                                 Ncpus = parallel::detectCores())"
# Install remotes package
RUN R -e "install.packages('remotes', repos = 'https://cloud.r-project.org')"

# Set CRAN repository and install all necessary CRAN packages in parallel
RUN Rscript -e "options(repos = c(CRAN = 'https://cran.r-project.org')); \
                install.packages(c('BiocManager', 'R.utils', 'ggplotify', 'bench', 'reticulate', 'scSorter', 'igraph', 'leiden'), \
                                 Ncpus = parallel::detectCores())"

# Install R packages with specific versions and prevent upgrade prompts
RUN R -e "options(ask = FALSE); \
          remotes::install_github('satijalab/seurat', ref = 'v5.2.1', upgrade = 'never'); \
          remotes::install_github('immunogenomics/harmony', upgrade = 'never'); \
          remotes::install_version('ggplot2', version = '3.5.1', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('future', version = '1.34.0', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('jsonlite', version = '1.9.1', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('later', version = '1.4.1', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('cluster', version = '2.1.4', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('zoo', version = '1.8-13', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('httpuv', version = '1.6.15', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('Matrix', version = '1.6-4', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('miniUI', version = '0.1.1.1', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('lattice', version = '0.21-8', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('survival', version = '3.5-5', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('KernSmooth', version = '2.23-21', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('globals', version = '0.16.3', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('nlme', version = '3.1-162', upgrade = 'never', repos = 'https://cran.r-project.org'); \
          remotes::install_version('MASS', version = '7.3-60', upgrade = 'never', repos = 'https://cran.r-project.org')"

RUN R -e "options(ask = FALSE); \
	  remotes::install_version('SeuratObject', version = '5.0.2', repos = 'https://cran.r-project.org')"

# Install Bioconductor packages in parallel
RUN R -e "BiocManager::install(c('SingleCellExperiment', 'scDblFinder', 'glmGamPoi'), \
                                     Ncpus = parallel::detectCores())"

ENV LD_LIBRARY_PATH=/usr/local/lib/R/lib:$LD_LIBRARY_PATH

RUN pip3 install --upgrade pip setuptools wheel && \
    pip3 install https://github.com/Dao-AILab/flash-attention/releases/download/v2.7.4.post1/flash_attn-2.7.4.post1+cu12torch2.2cxx11abiTRUE-cp310-cp310-linux_x86_64.whl && \
    pip3 install esm-efficient && \
    pip3 install gReLU==1.0.4.post1.dev0

# Copy the built texera binary from the build phase
COPY --from=build /.git /.git
COPY --from=build /core/amber/target/texera-* /core/amber
# Copy resources directories under /core from build phase
COPY --from=build /core/config/src/main/resources /core/config/src/main/resources
COPY --from=build /core/amber/src/main/resources /core/amber/src/main/resources
# Copy code for python & R UDF
COPY --from=build /core/amber/src/main/python /core/amber/src/main/python

CMD ["bin/computing-unit-master"]

EXPOSE 8085
