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
WORKDIR /texera

# Copy modules for building the service
COPY common/ common/
COPY amber/ amber/
COPY project/ project/
COPY build.sbt build.sbt

# Update system and install dependencies
RUN apt-get update && apt-get install -y \
    netcat \
    unzip \
    libpq-dev \
    && apt-get clean

# Add .git for runtime calls to jgit from OPversion
COPY .git .git

RUN sbt clean WorkflowExecutionService/dist

# Unzip the texera binary
RUN unzip amber/target/universal/amber-*.zip -d amber/target/

FROM eclipse-temurin:11-jdk-jammy AS runtime

WORKDIR /texera/amber

COPY --from=build /texera/amber/requirements.txt /tmp/requirements.txt
COPY --from=build /texera/amber/operator-requirements.txt /tmp/operator-requirements.txt

# Install Python runtime dependencies
RUN apt-get update && apt-get install -y \
    python3-pip \
    python3-dev \
    python3-venv \
    libpq-dev \
    curl \
    unzip \
    $(if [ "$WITH_R_SUPPORT" = "true" ]; then echo "\
    gfortran \
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
    openssh-client"; fi) \
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
    
# Install R and needed libraries (conditional)
ENV R_VERSION=4.3.3
RUN if [ "$WITH_R_SUPPORT" = "true" ]; then \
        curl -O https://cran.r-project.org/src/base/R-4/R-${R_VERSION}.tar.gz && \
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
        pip3 install -r /tmp/r-requirements.txt; \
    else \
        pip3 install --upgrade pip setuptools wheel && \
        pip3 install -r /tmp/requirements.txt && \
        pip3 install -r /tmp/operator-requirements.txt; \
    fi
# Install R packages, pinning arrow to 14.0.2.1 explicitly (conditional)
RUN if [ "$WITH_R_SUPPORT" = "true" ]; then \
        Rscript -e "options(repos = c(CRAN = 'https://cran.r-project.org')); \
                    install.packages(c('coro', 'dplyr', 'aws.s3'), \
                                     Ncpus = parallel::detectCores())" && \
        Rscript -e "options(repos = c(CRAN = 'https://cran.r-project.org')); \
                    if (!requireNamespace('remotes', quietly=TRUE)) \
                      install.packages('remotes'); \
                    remotes::install_version('arrow', version='14.0.2.1', \
                      repos='https://cran.r-project.org', upgrade='never'); \
                    cat('R arrow version: ', as.character(packageVersion('arrow')), '\n')"; \
    fi

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

# Copy the built texera binary from the build phase
COPY --from=build /texera/.git /texera/amber/.git
COPY --from=build /texera/amber/target/amber-* /texera/amber/
# Copy resources directories from build phase
COPY --from=build /texera/common/config/src/main/resources /texera/amber/common/config/src/main/resources
COPY --from=build /texera/amber/src/main/resources /texera/amber/src/main/resources
# Copy code for python & R UDF
COPY --from=build /texera/amber/src/main/python /texera/amber/src/main/python

CMD ["bin/computing-unit-master"]

EXPOSE 8085
