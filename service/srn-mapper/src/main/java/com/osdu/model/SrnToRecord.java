package com.osdu.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SrnToRecord {

  String recordId;
  String srn;

}
