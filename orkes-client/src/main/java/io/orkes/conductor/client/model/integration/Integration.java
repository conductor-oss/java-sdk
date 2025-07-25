/*
 * Copyright 2024 Conductor Authors.
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
package io.orkes.conductor.client.model.integration;

import java.util.List;
import java.util.Map;

import com.netflix.conductor.common.metadata.Auditable;

import io.orkes.conductor.client.model.TagObject;

import lombok.*;

@Data
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Integration extends Auditable {

    private Category category;
    private Map<String, Object> configuration;
    private String description;
    private Boolean enabled;
    private long modelsCount;
    private String name;
    private List<TagObject> tags;
    private String type;
    @Deprecated
    private String updatedBy;
    @Deprecated
    private Long updatedOn;
    private List<IntegrationApi> apis;
}