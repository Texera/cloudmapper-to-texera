/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package edu.uci.ics.amber.operator.machineLearning.sklearnAdvanced.KNNTrainer;

import edu.uci.ics.amber.operator.machineLearning.sklearnAdvanced.base.ParamClass;

public enum SklearnAdvancedKNNParameters implements ParamClass {
    n_neighbors("n_neighbors", "int"),
    p("p", "int"),
    weights("weights", "str"),
    algorithm("algorithm", "str"),
    leaf_size("leaf_size", "int"),
    metric("metric", "int"),
    metric_params("metric_params", "str");

    private final String name;
    private final String type;

    SklearnAdvancedKNNParameters(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }
}
