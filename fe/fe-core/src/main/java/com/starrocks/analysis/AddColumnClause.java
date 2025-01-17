// This file is made available under Elastic License 2.0.
// This file is based on code available under the Apache license here:
//   https://github.com/apache/incubator-doris/blob/master/fe/fe-core/src/main/java/org/apache/doris/analysis/AddColumnClause.java

// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.starrocks.analysis;

import com.google.common.base.Strings;
import com.starrocks.alter.AlterOpType;
import com.starrocks.catalog.Column;
import com.starrocks.common.AnalysisException;
import com.starrocks.common.ErrorCode;
import com.starrocks.common.ErrorReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

// clause which is used to add one column to
public class AddColumnClause extends AlterTableClause {
    private static final Logger LOG = LogManager.getLogger(AddColumnClause.class);
    private ColumnDef columnDef;
    // Column position
    private ColumnPosition colPos;
    // if rollupName is null, add to column to base index.
    private String rollupName;

    private Map<String, String> properties;
    // set in analyze
    private Column column;

    public Column getColumn() {
        return column;
    }

    public ColumnPosition getColPos() {
        return colPos;
    }

    public String getRollupName() {
        return rollupName;
    }

    public AddColumnClause(ColumnDef columnDef, ColumnPosition colPos, String rollupName,
                           Map<String, String> properties) {
        super(AlterOpType.SCHEMA_CHANGE);
        this.columnDef = columnDef;
        this.colPos = colPos;
        this.rollupName = rollupName;
        this.properties = properties;
    }

    @Override
    public void analyze(Analyzer analyzer) throws AnalysisException {
        if (columnDef == null) {
            throw new AnalysisException("No column definition in add column clause.");
        }
        columnDef.analyze(true);
        if (colPos != null) {
            colPos.analyze();
        }

        if (!columnDef.isAllowNull() && columnDef.defaultValueIsNull()) {
            ErrorReport.reportAnalysisException(ErrorCode.ERR_NO_DEFAULT_FOR_FIELD, columnDef.getName());
        }

        if (columnDef.getAggregateType() != null && colPos != null && colPos.isFirst()) {
            throw new AnalysisException("Cannot add value column[" + columnDef.getName() + "] at first");
        }

        if (Strings.isNullOrEmpty(rollupName)) {
            rollupName = null;
        }

        column = columnDef.toColumn();
    }

    @Override
    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override
    public String toSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("ADD COLUMN ").append(columnDef.toSql());
        if (colPos != null) {
            sb.append(" ").append(colPos.toSql());
        }
        if (rollupName != null) {
            sb.append(" IN `").append(rollupName).append("`");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toSql();
    }
}
