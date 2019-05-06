/*
 * Copyright 2019 ConsenSys AG.
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

package tech.pegasys.errorpronechecks;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DoNotReturnNullOptionalsTest {

  private CompilationTestHelper compilationHelper;

  @BeforeEach
  void setup() {
    compilationHelper =
        CompilationTestHelper.newInstance(DoNotReturnNullOptionals.class, getClass());
  }

  @Test
  void doNotReturnNullPositiveCases() {
    compilationHelper.addSourceFile("DoNotReturnNullOptionalsPositiveCases.java").doTest();
  }

  @Test
  void doNotReturnNullNegativeCases() {
    compilationHelper.addSourceFile("DoNotReturnNullOptionalsNegativeCases.java").doTest();
  }
}
