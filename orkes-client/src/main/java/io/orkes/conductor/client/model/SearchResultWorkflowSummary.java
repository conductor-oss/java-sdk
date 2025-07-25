/*
 * Copyright 2022 Conductor Authors.
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
package io.orkes.conductor.client.model;

import java.util.ArrayList;
import java.util.List;

import com.netflix.conductor.common.run.WorkflowSummary;

import lombok.*;

@EqualsAndHashCode
@ToString
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class SearchResultWorkflowSummary {

    @Builder.Default
    private List<WorkflowSummary> results = null;

    @Builder.Default
    private Long totalHits = null;

    public SearchResultWorkflowSummary results(List<WorkflowSummary> results) {
        this.results = results;
        return this;
    }

    public SearchResultWorkflowSummary addResultsItem(WorkflowSummary resultsItem) {
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(resultsItem);
        return this;
    }

    public List<WorkflowSummary> getResults() {
        return results;
    }

    public void setResults(List<WorkflowSummary> results) {
        this.results = results;
    }

    public SearchResultWorkflowSummary totalHits(Long totalHits) {
        this.totalHits = totalHits;
        return this;
    }

    public Long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(Long totalHits) {
        this.totalHits = totalHits;
    }
}
