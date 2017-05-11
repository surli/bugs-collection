/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.restassured.internal

import static io.restassured.internal.assertion.AssertParameter.notNull

class MapCreator {

  static enum CollisionStrategy {
    MERGE, OVERWRITE
  }

  def static Map<String, Object> createMapFromParams(CollisionStrategy collisionStrategy,
                                                     String firstParam, Object firstValue, ... parameters) {
    return createMapFromObjects(collisionStrategy, createArgumentArrayFromKeyAndValue(firstParam, firstValue, parameters));
  }

  def static Map<String, Object> createMapFromParams(CollisionStrategy collisionStrategy, String firstParam, ... parameters) {
    return createMapFromObjects(collisionStrategy, createArgumentArray(firstParam, parameters));
  }

  def static Map<String, Object> createMapFromObjects(CollisionStrategy collisionStrategy, ... parameters) {
    if (parameters == null || parameters.length < 2) {
      throw new IllegalArgumentException("You must supply at least one key and one value.");
    } else if (parameters.length % 2 != 0) {
      throw new IllegalArgumentException("You must supply the same number of keys as values.")
    }

    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (int i = 0; i < parameters.length; i += 2) {
      def key = parameters[i]
      def val = parameters[i + 1]
      if (map.containsKey(key) && collisionStrategy == CollisionStrategy.MERGE) {
        def currentValue = map.get(key)
        if (currentValue instanceof List) {
          currentValue << val
        } else {
          map.put(key, [currentValue, val])
        }
      } else {
        map.put(key, val)
      }
    }
    return map;
  }

  private static Object[] createArgumentArray(String firstParam, ... parameters) {
    notNull firstParam, "firstParam"
    if (parameters == null || parameters.length == 0) {
      return [firstParam: new NoParameterValue()] as Object[]
    }

    def params = [firstParam, parameters[0]]
    if (parameters.length > 1) {
      parameters[1..-1].each {
        params << it
      }
    }
    return params as Object[]
  }

  private static Object[] createArgumentArrayFromKeyAndValue(String firstParam, Object firstValue, ... parameters) {
    notNull firstParam, "firstParam"
    notNull firstValue, "firstValue"
    def params = [firstParam, firstValue]
    parameters.each {
      params << it
    }
    return params as Object[]
  }
}