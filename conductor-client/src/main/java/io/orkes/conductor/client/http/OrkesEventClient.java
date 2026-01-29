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
package io.orkes.conductor.client.http;

import java.util.List;
import java.util.Map;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.EventClient;
import com.netflix.conductor.common.metadata.events.EventHandler;

import io.orkes.conductor.client.model.OrkesEventHandler;
import io.orkes.conductor.client.model.Tag;
import io.orkes.conductor.client.model.event.QueueConfiguration;

public class OrkesEventClient extends EventClient {

    private final EventResource eventResource;

    public OrkesEventClient(ConductorClient client) {
        super(client);
        this.eventResource = new EventResource(client);
    }

    /**
     * @deprecated since 4.0.19, forRemoval in 4.1.0. Use {@link #getOrkesEventHandlers()} instead.
     */
    @Deprecated(since = "4.0.19", forRemoval = true)
    public List<EventHandler> getEventHandlers() {
        return eventResource.getEventHandlers();
    }

    public List<OrkesEventHandler> getOrkesEventHandlers() {
        return eventResource.getOrkesEventHandlers();
    }

    public Map<String, Object> getQueueConfig(QueueConfiguration queueConfiguration) {
        return eventResource.getQueueConfig(queueConfiguration.getQueueType(), queueConfiguration.getQueueName());
    }

    public void deleteQueueConfig(QueueConfiguration queueConfiguration) {
        eventResource.deleteQueueConfig(queueConfiguration.getQueueType(), queueConfiguration.getQueueName());
    }

    public void putQueueConfig(QueueConfiguration queueConfiguration) {
        eventResource.putQueueConfig(queueConfiguration.getQueueType(), queueConfiguration.getQueueName());
    }

    public List<OrkesEventHandler> getOrkesEventHandlers(String event, boolean activeOnly) {
        return eventResource.getOrkesEventHandlers(event, activeOnly);
    }

    public void putTagsForEventHandler(String name, List<Tag> tags) {
        eventResource.putTagsForEventHandler(name, tags);
    }

    public List<Tag> getTagsForEventHandler(String name) {
        return eventResource.getTagsForEventHandler(name);
    }
}
