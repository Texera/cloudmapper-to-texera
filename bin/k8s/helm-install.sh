#!/bin/bash
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

# Helm install/upgrade script for Texera with custom images from bobbai2000 registry.
# Usage: ./helm-install.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

helm upgrade --install texera "$SCRIPT_DIR" --namespace texera --create-namespace --set-string texera.imageRegistry=bobbai2000 --set-string texera.imageTag=ssh --set texeraImages.pullPolicy=IfNotPresent --set metrics-server.enabled=false --set exampleDataLoader.enabled=false
