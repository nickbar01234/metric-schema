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

import com.fasterxml.jackson.core.type.TypeReference;
import com.palantir.metric.schema.MetricSchema;
import com.palantir.metric.schema.markdown.MarkdownRenderer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

@CacheableTask
public class GenerateMetricMarkdownTask extends DefaultTask {
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    private final RegularFileProperty manifestFile = getProject().getObjects().fileProperty();
    private final Property<String> localCoordinates = getProject()
            .getObjects()
            .property(String.class)
            .value(getProject()
                    .provider(() ->
                            "" + getProject().getGroup() + ':' + getProject().getName()));

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public final RegularFileProperty getManifestFile() {
        return manifestFile;
    }

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public final Provider<RegularFile> getMarkdownFile() {
        return ProviderUtils.filterNonExistentFile(getProject(), outputFile);
    }

    @Input
    public final Property<String> getLocalCoordinates() {
        return localCoordinates;
    }

    @OutputFile
    public final RegularFileProperty getOutputFile() {
        return outputFile;
    }

    @TaskAction
    public final void generate() throws IOException {
        File markdown = outputFile.get().getAsFile();
        File manifest = getManifestFile().getAsFile().get();

        Map<String, List<MetricSchema>> schemas =
                ObjectMappers.mapper.readValue(manifest, new TypeReference<Map<String, List<MetricSchema>>>() {});
        if (isEmpty(schemas)) {
            if (markdown.exists()) {
                markdown.delete();
            }
            return;
        }

        String upToDateContents = MarkdownRenderer.render(localCoordinates.get(), schemas);
        Files.writeString(markdown.toPath(), upToDateContents);
    }

    private static boolean isEmpty(Map<String, List<MetricSchema>> schemas) {
        return schemas.isEmpty() || schemas.values().stream().allMatch(List::isEmpty);
    }
}
