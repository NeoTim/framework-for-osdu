package com.osdu.model.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.osdu.model.BaseJsonObject;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkProduct extends BaseJsonObject {

  @JsonProperty(value = "Data")
  Map<String, Object> data;

}