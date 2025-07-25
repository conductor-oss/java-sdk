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

import lombok.*;

@EqualsAndHashCode
@ToString
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TargetRef {

    @Builder.Default
    private String id = null;

    public enum TypeEnum {
        WORKFLOW_DEF("WORKFLOW_DEF"),
        TASK_DEF("TASK_DEF"),
        APPLICATION("APPLICATION"),
        USER("USER"),
        SECRET("SECRET_NAME"),
        TAG("TAG"),
        DOMAIN("DOMAIN");

        private final String value;

        TypeEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static TypeEnum fromValue(String input) {
            for (TypeEnum b : TypeEnum.values()) {
                if (b.value.equals(input)) {
                    return b;
                }
            }
            return null;
        }
    }

    @Builder.Default
    private TypeEnum type = null;

    public TargetRef id(String id) {
        this.id = id;
        return this;
    }

    public TargetRef type(TypeEnum type) {
        this.type = type;
        return this;
    }

}