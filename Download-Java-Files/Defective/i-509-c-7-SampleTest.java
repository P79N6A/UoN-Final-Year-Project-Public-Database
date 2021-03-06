/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.docker.test.samples;

import org.apache.commons.io.FilenameUtils;
import org.ballerinax.docker.exceptions.DockerPluginException;
import org.ballerinax.docker.test.utils.DockerTestException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base class for test cases written to test samples.
 */
public interface SampleTest {
    Path SAMPLE_DIR = Paths.get(FilenameUtils.separatorsToSystem(System.getProperty("sample.dir")));
    Path CLIENT_BAL_FOLDER = Paths.get("src").resolve("test").resolve("resources").resolve("sample-clients")
            .toAbsolutePath();

    @BeforeClass
    void compileSample() throws IOException, InterruptedException;

    @AfterClass
    void cleanUp() throws DockerPluginException, InterruptedException, DockerTestException;
}
