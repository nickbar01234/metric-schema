/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.metric.schema.gradle;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeRuntimeException;
import com.palantir.metric.schema.JavaGenerator;
import com.palantir.metric.schema.JavaGeneratorArgs;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public abstract class GenerateMetricSchemaTask extends DefaultTask {
    private final Property<String> libraryName =
            getProject().getObjects().property(String.class).value(defaultLibraryName());

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputFile();

    @Input
    public final Property<String> getLibraryName() {
        return libraryName;
    }

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public final void generate() {
        File output = getOutputDir().getAsFile().get();
        clearOutput(output.toPath());
        getProject().mkdir(output);

        JavaGenerator.generate(JavaGeneratorArgs.builder()
                .input(getInputFile().getAsFile().get().toPath())
                .output(output.toPath())
                .libraryName(Optional.ofNullable(libraryName.getOrNull()))
                // TODO(forozco): probably want something better
                .defaultPackageName(getProject().getGroup().toString())
                .build());
    }

    private static void clearOutput(Path outputPath) {
        try {
            MoreFiles.deleteRecursively(outputPath, RecursiveDeleteOption.ALLOW_INSECURE);
        } catch (IOException e) {
            throw new SafeRuntimeException("Unable to clean output directory", SafeArg.of("output", outputPath));
        }
    }

    private String defaultLibraryName() {
        String rootProjectName = getProject().getRootProject().getName();
        return rootProjectName.replaceAll("-root$", "");
    }
}
