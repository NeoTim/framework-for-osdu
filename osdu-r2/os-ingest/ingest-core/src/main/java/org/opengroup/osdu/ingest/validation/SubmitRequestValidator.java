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

package org.opengroup.osdu.ingest.validation;

import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.model.DataType;
import org.opengroup.osdu.ingest.model.SubmitRequest;
import org.opengroup.osdu.ingest.property.DataTypeValidationProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmitRequestValidator
    implements ConstraintValidator<ValidSubmitRequest, SubmitRequest> {

  private static final String DATA_TYPE_FIELD = "DataType";
  private static final String FILE_ID_FIELD = "FileID";
  private static final String DATA_TYPE_ERROR_TEMPLATE = "DataType %s is not allowed for ingest. Allowed data types - %s";
  final DataTypeValidationProperties dataTypeValidationProperties;

  @Override
  public boolean isValid(SubmitRequest request,
      ConstraintValidatorContext constraintValidatorContext) {
    String fileID = request.getFileId();
    DataType dataType = request.getDataType();

    if (StringUtils.isBlank(fileID)) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext
          .buildConstraintViolationWithTemplate("{javax.validation.constraints.NotBlank.message}")
          .addPropertyNode(FILE_ID_FIELD)
          .addConstraintViolation();
      return false;
    }

    if (dataType == null) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext
          .buildConstraintViolationWithTemplate("{javax.validation.constraints.NotNull.message}")
          .addPropertyNode(DATA_TYPE_FIELD)
          .addConstraintViolation();
      return false;
    }

    List<DataType> allowedDataTypes = dataTypeValidationProperties.getAllowedDataTypes();
    if (!allowedDataTypes.contains(dataType)) {
      constraintValidatorContext.disableDefaultConstraintViolation();
      constraintValidatorContext
          .buildConstraintViolationWithTemplate(
              String.format(DATA_TYPE_ERROR_TEMPLATE, dataType, allowedDataTypes))
          .addPropertyNode(DATA_TYPE_FIELD)
          .addConstraintViolation();
      return false;
    }

    return true;
  }

}
