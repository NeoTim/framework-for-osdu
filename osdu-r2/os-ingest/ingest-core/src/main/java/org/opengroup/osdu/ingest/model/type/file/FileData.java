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

package org.opengroup.osdu.ingest.model.type.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.opengroup.osdu.ingest.model.type.base.IndividualTypeProperties;
import org.opengroup.osdu.ingest.model.type.base.OsduObjectData;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class FileData extends OsduObjectData {

  @JsonIgnore
  FileGroupTypeProperties groupTypeProperties;

  @JsonIgnore
  IndividualTypeProperties individualTypeProperties;

  @Override
  @JsonProperty("GroupTypeProperties")
  public FileGroupTypeProperties getGroupTypeProperties() {
    return groupTypeProperties;
  }

  @Override
  @JsonProperty("IndividualTypeProperties")
  public IndividualTypeProperties getIndividualTypeProperties() {
    return individualTypeProperties;
  }

}
