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
 * Handed to an Agent's {@code feedbackSink} after a conversation memory is saved.
 *
 * <p>Carries the distilled summary plus the signed capability URLs a human can click
 * to mark the memory good/bad. The integrator routes these out-of-band (e.g. posts
 * them into a Zendesk ticket). These URLs are <b>never</b> shown to the agent's LLM —
 * agents can only create and read memories; good/bad feedback is human-only.
 *
 * <p>Mirrors the Python ({@code FeedbackEvent}) dataclass in {@code ocg_memory.py}.
 */
public class FeedbackEvent {

    private final String memoryKey;
    private final String summary;
    private final List<String> facts;
    private final List<String> tags;
    private final String goodUrl;
    private final String badUrl;
    private final String expiresAt;
    private final String agent;
    private final String user;
    private final String sessionId;

    private FeedbackEvent(Builder b) {
        this.memoryKey = b.memoryKey;
        this.summary = b.summary;
        this.facts = b.facts != null ? new ArrayList<>(b.facts) : new ArrayList<>();
        this.tags = b.tags != null ? new ArrayList<>(b.tags) : new ArrayList<>();
        this.goodUrl = b.goodUrl;
        this.badUrl = b.badUrl;
        this.expiresAt = b.expiresAt;
        this.agent = b.agent;
        this.user = b.user;
        this.sessionId = b.sessionId;
    }

    public String getMemoryKey() {
        return memoryKey;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getFacts() {
        return facts;
    }

    public List<String> getTags() {
        return tags;
    }

    /** Signed capability URL to mark the memory helpful, or {@code null} if unavailable. */
    public String getGoodUrl() {
        return goodUrl;
    }

    /** Signed capability URL to mark the memory unhelpful, or {@code null} if unavailable. */
    public String getBadUrl() {
        return badUrl;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getAgent() {
        return agent;
    }

    public String getUser() {
        return user;
    }

    public String getSessionId() {
        return sessionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link FeedbackEvent}. */
    public static class Builder {
        private String memoryKey;
        private String summary;
        private List<String> facts;
        private List<String> tags;
        private String goodUrl;
        private String badUrl;
        private String expiresAt;
        private String agent;
        private String user;
        private String sessionId;

        public Builder memoryKey(String memoryKey) {
            this.memoryKey = memoryKey;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder facts(List<String> facts) {
            this.facts = facts;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder goodUrl(String goodUrl) {
            this.goodUrl = goodUrl;
            return this;
        }

        public Builder badUrl(String badUrl) {
            this.badUrl = badUrl;
            return this;
        }

        public Builder expiresAt(String expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder agent(String agent) {
            this.agent = agent;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public FeedbackEvent build() {
            return new FeedbackEvent(this);
        }
    }
}
