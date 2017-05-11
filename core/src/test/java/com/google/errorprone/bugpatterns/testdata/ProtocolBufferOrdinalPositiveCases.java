/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.testdata;

import com.google.errorprone.bugpatterns.proto.TestEnum;

/** Positive test cases for {@link ProtocolBufferOrdinal} check. */
public class ProtocolBufferOrdinalPositiveCases {

  public static void checkCallOnOrdinal() {
    // BUG: Diagnostic contains: TestEnum.TEST_ENUM_VAL.getNumber()
    TestEnum.TEST_ENUM_VAL.ordinal();
  }
}
