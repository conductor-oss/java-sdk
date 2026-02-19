/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.client.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenRequest extends LLMWorkerInput {

    // Basic parameters
    private String prompt;
    private String inputImage;
    @Builder.Default private Integer duration = 5;
    @Builder.Default private Integer width = 1280;
    @Builder.Default private Integer height = 720;
    @Builder.Default private Integer fps = 24;
    @Builder.Default private String outputFormat = "mp4";

    // Advanced parameters
    private String style;
    private String motion;
    private Integer seed;
    private Float guidanceScale;
    private String aspectRatio;

    // Provider-specific advanced parameters
    private String negativePrompt;
    private String personGeneration;
    private String resolution;
    private Boolean generateAudio;
    private String size;

    // Preview/thumbnail generation
    @Builder.Default private Boolean generateThumbnail = true;
    private Integer thumbnailTimestamp;

    // Cost control
    private Integer maxDurationSeconds;
    private Float maxCostDollars;

    // Polling state
    private String jobId;
    private String status;
    private Integer pollCount;

    @Builder.Default private int n = 1;
}
