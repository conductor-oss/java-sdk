/*
 * Copyright 2026 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.conductor.example.mediatranscoder.workers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.netflix.conductor.sdk.workflow.task.OutputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import org.conductoross.conductor.sdk.file.FileHandler;

public class TranscodeWorker {

    public static class TranscodeInput {
        public FileHandler primary_video;
        public String resolution;
    }

    @WorkerTask("transcode_video")
    public @OutputParam("output_file") FileHandler transcode(TranscodeInput input) throws IOException {
        Path transcoded = Files.createTempFile("transcoded-" + input.resolution + "-", ".mp4");
        try (InputStream in = input.primary_video.getInputStream()) {
            Files.write(transcoded, ("TRANSCODED:" + input.resolution + "\n").getBytes());
            Files.write(transcoded, in.readAllBytes(), StandardOpenOption.APPEND);
        }
        System.out.println("[transcode_video] Transcoded to " + transcoded);
        return FileHandler.fromLocalFile(transcoded, "video/mp4");
    }
}
