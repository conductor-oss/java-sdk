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
package org.conductoross.conductor.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured output for the conversation summarizer agent.
 *
 * <p>Used as the {@code outputType} of the internal memory summarizer (see
 * {@link OCGMemoryStore#buildMemorySummarizer(String)}). The summarizer distills a
 * conversation into durable facts — a one-paragraph {@link #summary}, a list of
 * reusable {@link #facts}, and a few topical {@link #tags} — which the post-run
 * save hook stores back to OCG as a {@code conversation:<session>} memory.
 *
 * <p>Mirrors the Python ({@code MemorySummary}) reference type in
 * {@code ocg_memory.py}.
 */
public class MemorySummary {

    /** One short paragraph: what happened / what was learned. */
    private String summary = "";
    /** Durable, reusable facts about the user or task (no chit-chat). */
    private List<String> facts = new ArrayList<>();
    /** Short topical tags. */
    private List<String> tags = new ArrayList<>();

    public MemorySummary() {}

    public MemorySummary(String summary, List<String> facts, List<String> tags) {
        this.summary = summary != null ? summary : "";
        this.facts = facts != null ? new ArrayList<>(facts) : new ArrayList<>();
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getFacts() {
        return facts;
    }

    public void setFacts(List<String> facts) {
        this.facts = facts;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
