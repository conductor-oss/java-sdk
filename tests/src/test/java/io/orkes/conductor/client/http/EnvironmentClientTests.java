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
package io.orkes.conductor.client.http;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.netflix.conductor.client.exception.ConductorClientException;

import io.orkes.conductor.client.EnvironmentClient;
import io.orkes.conductor.client.model.Tag;
import io.orkes.conductor.client.model.environment.EnvironmentVariable;
import io.orkes.conductor.client.util.ClientTestUtil;

public class EnvironmentClientTests {

    private static EnvironmentClient envClient;

    @BeforeAll
    public static void setup() {
        envClient = ClientTestUtil.getOrkesClients().getEnvironmentClient();
    }

    @Test
    void testMethods() {
        // Use a unique name per test run to avoid collisions across CI runs
        String varName = "test-sdk-java-env-var-" + UUID.randomUUID();
        String value = "value-" + UUID.randomUUID();

        try {
            envClient.deleteEnvironmentVariable(varName);
        } catch (ConductorClientException ignore) {
            // ignore if not found
        }

        // create/update
        envClient.createOrUpdateEnvironmentVariable(varName, value);

        // get
        String fetched = envClient.getEnvironmentVariable(varName);
        Assertions.assertEquals(value, fetched);

        // list all
        List<EnvironmentVariable> all = Optional.ofNullable(envClient.getAllEnvironmentVariables()).orElse(List.of());
        Assertions.assertTrue(
                all.stream().anyMatch(ev -> varName.equals(ev.getName()) && value.equals(ev.getValue())));

        // tags put/get/delete
        List<Tag> tags = List.of(getTag());
        envClient.setEnvironmentVariableTags(varName, tags);
        List<Tag> currentTags = envClient.getEnvironmentVariableTags(varName);
        Assertions.assertEquals(1, currentTags.size());
        Assertions.assertEquals(tags.get(0), currentTags.get(0));

        envClient.deleteEnvironmentVariableTags(varName, tags);
        Assertions.assertEquals(0, envClient.getEnvironmentVariableTags(varName).size());

        // delete
        String oldValue = envClient.deleteEnvironmentVariable(varName);
        Assertions.assertEquals(value, oldValue);
    }

    @Test
    void testNullArgumentsValidation() {
        // Methods should defensively throw NPE on null arguments
        Assertions.assertThrows(NullPointerException.class, () -> envClient.getEnvironmentVariable(null));
        Assertions.assertThrows(NullPointerException.class, () -> envClient.createOrUpdateEnvironmentVariable(null, "v"));
        Assertions.assertThrows(NullPointerException.class, () -> envClient.createOrUpdateEnvironmentVariable("K", null));
        Assertions.assertThrows(NullPointerException.class, () -> envClient.deleteEnvironmentVariable(null));
        Assertions.assertThrows(NullPointerException.class, () -> envClient.getEnvironmentVariableTags(null));
        Assertions.assertThrows(NullPointerException.class, () -> envClient.setEnvironmentVariableTags(null, List.of()));
        Assertions.assertThrows(NullPointerException.class, () -> envClient.setEnvironmentVariableTags("K", null));
        Assertions.assertThrows(NullPointerException.class, () -> envClient.deleteEnvironmentVariableTags(null, List.of()));
        Assertions.assertThrows(NullPointerException.class, () -> envClient.deleteEnvironmentVariableTags("K", null));
    }

    private Tag getTag() {
        return Tag.builder().key("department").value("accounts").build();
    }
}
