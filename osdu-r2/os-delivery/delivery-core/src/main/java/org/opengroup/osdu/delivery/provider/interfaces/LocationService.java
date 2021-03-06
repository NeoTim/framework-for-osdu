/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.delivery.provider.interfaces;

import javax.validation.ConstraintViolationException;
import org.opengroup.osdu.core.common.model.file.FileLocationRequest;
import org.opengroup.osdu.core.common.model.file.FileLocationResponse;
import org.opengroup.osdu.core.common.model.file.LocationRequest;
import org.opengroup.osdu.core.common.model.file.LocationResponse;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;

public interface LocationService {

  /**
   * GetLocation creates a new location (e.g. bucket) in a landing zone for a new file
   * which will be uploaded by calling party.
   *
   * @param request location request
   * @param headers request headers
   * @return location response that contains fileID and location
   * @throws ConstraintViolationException if request is invalid
   */
  LocationResponse getLocation(LocationRequest request, DpsHeaders headers);

  /**
   * GetFileLocation returns internal information about particular file, including Driver.
   *
   * @param request location request
   * @param headers request headers
   * @return file location response that contains driver and location.
   */
  FileLocationResponse getFileLocation(FileLocationRequest request, DpsHeaders headers);

}
