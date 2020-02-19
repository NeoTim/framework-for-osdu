/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.ingest.model.type.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.opengroup.osdu.ingest.serializer.LocalDateTimeSerializer;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OsduObject {

  @JsonProperty("ResourceID")
  String resourceID;

  @JsonProperty("ResourceTypeID")
  String resourceTypeID;

  @JsonProperty("ResourceHomeRegionID")
  String resourceHomeRegionID;

  @JsonProperty("ResourceHostRegionIDs")
  List<String> resourceHostRegionIDs;

  @JsonProperty("ResourceObjectCreationDateTime")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  LocalDateTime resourceObjectCreationDatetime;

  @JsonProperty("ResourceVersionCreationDateTime")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  LocalDateTime resourceVersionCreationDatetime;

  @JsonProperty("ResourceCurationStatus")
  String resourceCurationStatus;

  @JsonProperty("ResourceLifecycleStatus")
  String resourceLifecycleStatus;

  @JsonProperty("ResourceSecurityClassification")
  String resourceSecurityClassification;

  @JsonIgnore
  OsduObjectData data;

  @JsonProperty("Data")
  public OsduObjectData getData() {
    return data;
  }

}