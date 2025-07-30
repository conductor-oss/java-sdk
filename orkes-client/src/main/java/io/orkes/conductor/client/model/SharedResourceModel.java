package io.orkes.conductor.client.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class SharedResourceModel {

    String resourceType;
    String resourceName;
    String sharedBy;
    String sharedWith;
}
