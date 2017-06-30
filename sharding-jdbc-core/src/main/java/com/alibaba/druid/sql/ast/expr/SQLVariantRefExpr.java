/*
 * Copyright 1999-2101 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.druid.sql.ast.expr;

import com.alibaba.druid.sql.ast.SQLExprImpl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = "name")
public class SQLVariantRefExpr extends SQLExprImpl {
    
    private String name;
    
    private boolean global;
    
    private int index  = -1;
    
    public SQLVariantRefExpr(final String name) {
        this.name = name;
    }
    
    public SQLVariantRefExpr(final String name, final boolean global) {
        this.name = name;
        this.global = global;
    }
    
    @Override
    public void output(final StringBuffer buffer) {
        buffer.append(this.name);
    }
}
