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

package org.apache.doris.kafka.connector.decode;

import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;

public class DorisJsonSchema implements Schema {
    static String NAME = "DORIS_JSON_SCHEMA";
    static int VERSION = 1;

    @Override
    public Schema.Type type() {
        return Type.STRUCT;
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    public Object defaultValue() {
        return null;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Integer version() {
        return VERSION;
    }

    @Override
    public String doc() {
        return null;
    }

    @Override
    public Map<String, String> parameters() {
        return null;
    }

    @Override
    public Schema keySchema() {
        return null;
    }

    @Override
    public Schema valueSchema() {
        return null;
    }

    @Override
    public List<Field> fields() {
        return null;
    }

    @Override
    public Field field(final String s) {
        return null;
    }

    @Override
    public Schema schema() {
        return null;
    }
}
