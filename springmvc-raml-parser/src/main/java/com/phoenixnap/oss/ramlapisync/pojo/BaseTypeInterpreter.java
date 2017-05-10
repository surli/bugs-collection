/*
 * Copyright 2002-2017 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.phoenixnap.oss.ramlapisync.pojo;

import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

/**
 * Interpreter for Object types.
 * 
 * @author kurtpa
 * @since 0.10.0
 *
 */
public abstract class BaseTypeInterpreter implements RamlTypeInterpreter {

	protected void typeCheck(TypeDeclaration type) {
		boolean found = false;
		for (Class<?> typeClass : getSupportedTypes()) {
			if (typeClass.isAssignableFrom(type.getClass())) {
				found = true;
				break;
			}
		}
		if (!found) {
			throw new IllegalStateException("This type is not supported by this interpreter");
		}
	}

}
