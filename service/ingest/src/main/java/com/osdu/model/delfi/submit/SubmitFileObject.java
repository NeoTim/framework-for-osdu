package com.osdu.model.delfi.submit;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitFileObject {

  String ingestorRoutines;
  String kind;
  String filePath;
  @JsonAlias("legaltags")
  String legalTags;
  String acl;
  FileInput fileInput;
  String additionalProperties;

}
