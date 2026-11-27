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
package io.orkes.conductor.harness.e2e;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Names and environment settings shared by the end-to-end simulator, workers and driver. */
final class E2eConfig {

    static final String FETCH_PROFILE = "e2e_fetch_profile";
    static final String SCORE = "e2e_score";
    static final String PERSIST = "e2e_persist";
    static final String NOTIFY_PARTNER = "e2e_notify_partner";
    static final List<String> TASK_TYPES = List.of(FETCH_PROFILE, SCORE, PERSIST, NOTIFY_PARTNER);

    static final String PARTNER_DOMAIN = "partner_a";

    static final String ORDER_FULFILLMENT = "e2e_order_fulfillment";
    static final String RISK_REVIEW = "e2e_risk_review";
    static final String PARTNER_SYNC = "e2e_partner_sync";
    static final String NIGHTLY_RECONCILE = "e2e_nightly_reconcile";

    /** Queue names as the server sizes them: task type, or {@code domain:taskType}. */
    static final List<String> QUEUE_NAMES =
            List.of(FETCH_PROFILE, SCORE, PERSIST, PARTNER_DOMAIN + ":" + NOTIFY_PARTNER, NOTIFY_PARTNER);

    private E2eConfig() {}

    static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    static int envInt(String name, int defaultValue) {
        return Integer.parseInt(env(name, Integer.toString(defaultValue)));
    }

    static double envDouble(String name, double defaultValue) {
        return Double.parseDouble(env(name, Double.toString(defaultValue)));
    }

    static List<String> envList(String name, String defaultValue) {
        return Arrays.stream(env(name, defaultValue).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Parses {@code a=x,b=y}. */
    static Map<String, String> envStringMap(String name, String defaultValue) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : envList(name, defaultValue)) {
            String[] parts = pair.split("=");
            values.put(parts[0].trim(), parts[1].trim());
        }
        return values;
    }

    /** Parses {@code a=1,b=2}. */
    static Map<String, Integer> envIntMap(String name, String defaultValue) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String pair : envList(name, defaultValue)) {
            String[] parts = pair.split("=");
            values.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        }
        return values;
    }
}
