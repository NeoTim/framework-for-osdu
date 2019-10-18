package com.osdu.model.job;

import com.osdu.model.BaseRecord;
import java.util.List;
import lombok.Data;

@Data
public class IngestJobStatusDto {

  String id;
  IngestJobStatus status;
  List<BaseRecord> records;

  /**
   * Method for create IngestJobStatusDto.
   *
   * @param job to construct IngestJobStatusDto
   * @return IngestJobStatusDto
   */
  public static IngestJobStatusDto fromIngestJob(IngestJob job) {
    IngestJobStatusDto dto = new IngestJobStatusDto();
    dto.setId(job.getId());
    dto.setStatus(job.getStatus());
    dto.setRecords(job.getRecords());

    return dto;
  }
}