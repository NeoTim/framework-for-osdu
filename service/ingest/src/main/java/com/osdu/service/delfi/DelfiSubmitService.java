package com.osdu.service.delfi;

import static com.osdu.model.delfi.status.MasterJobStatus.COMPLETED;
import static com.osdu.model.delfi.status.MasterJobStatus.FAILED;
import static com.osdu.model.delfi.status.MasterJobStatus.RUNNING;
import static com.osdu.service.JsonUtils.toJson;
import static com.osdu.service.JsonUtils.toObject;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.osdu.client.DelfiIngestionClient;
import com.osdu.exception.IngestException;
import com.osdu.model.ResourceType;
import com.osdu.model.ResourceTypeId;
import com.osdu.model.delfi.Acl;
import com.osdu.model.delfi.DelfiFile;
import com.osdu.model.delfi.IngestedFile;
import com.osdu.model.delfi.RequestMeta;
import com.osdu.model.delfi.SaveRecordsResult;
import com.osdu.model.delfi.SuccessMetadata;
import com.osdu.model.delfi.status.JobStatusResponse;
import com.osdu.model.delfi.status.JobsPullingResult;
import com.osdu.model.delfi.status.MasterJobStatus;
import com.osdu.model.delfi.submit.AclObject;
import com.osdu.model.delfi.submit.FileInput;
import com.osdu.model.delfi.submit.SubmitFileObject;
import com.osdu.model.delfi.submit.SubmitFileResult;
import com.osdu.model.delfi.submit.SubmitJobResult;
import com.osdu.model.delfi.submit.SubmittedFile;
import com.osdu.model.delfi.submit.ingestor.LasIngestor;
import com.osdu.model.delfi.submit.ingestor.LasIngestorObject;
import com.osdu.model.property.DelfiPortalProperties;
import com.osdu.service.SubmitService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class DelfiSubmitService implements SubmitService {

  private static final String GCS_PROTOCOL = "gs:/";
  private static final String SUCCESS_METADATA_JSON_PATH = "successrecords/success-metadata.json";

  final RestTemplate restTemplate;
  final DelfiPortalProperties portalProperties;
  final DelfiIngestionClient delfiIngestionClient;
  final DelfiPortalService portalService;

  @Override
  public JobsPullingResult awaitSubmitJobs(List<String> jobIds, RequestMeta requestMeta) {
    List<String> runningJobs = jobIds;
    List<JobStatusResponse> failedJobs = new ArrayList<>(runningJobs.size());
    List<JobStatusResponse> completedJobs = new ArrayList<>(runningJobs.size());

    while (!runningJobs.isEmpty()) {
      Map<MasterJobStatus, List<JobStatusResponse>> submittedJobsByStatus = runningJobs.stream()
          .map(jobId -> delfiIngestionClient.getJobStatus(jobId,
              requestMeta.getAuthorizationToken(),
              portalProperties.getAppKey(),
              requestMeta.getPartition()))
          .collect(groupingBy(response -> response.getJobInfo().getMasterJobStatus(),
              mapping(Function.identity(), toList())));

      runningJobs = MapUtils.getObject(submittedJobsByStatus, RUNNING, emptyList()).stream()
          .map(response -> response.getJobInfo().getJobId())
          .collect(toList());
      failedJobs.addAll(MapUtils.getObject(submittedJobsByStatus, FAILED, emptyList()));
      completedJobs.addAll(MapUtils.getObject(submittedJobsByStatus, COMPLETED, emptyList()));

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IngestException("Pulling submitted jobs was unexpected interrupted. JobIds = "
            + jobIds, e);
      }
    }

    return JobsPullingResult.builder()
        .runningJobs(jobIds)
        .failedJobs(failedJobs)
        .completedJobs(completedJobs)
        .build();
  }

  @Override
  public SubmitJobResult submitFile(String relativeFilePath, RequestMeta requestMeta) {
    String srn = generateSrn(requestMeta.getResourceTypeId());
    SubmitFileResult submitFileResult = delfiIngestionClient
        .submitFile(requestMeta.getAuthorizationToken(),
            portalProperties.getAppKey(),
            requestMeta.getPartition(),
            SubmitFileObject.builder()
                .kind(requestMeta.getSchemaData().getKind())
                .acl(getAcl(requestMeta.getUserGroupEmailByName()))
                .legalTags(requestMeta.getLegalTags())
                .filePath(GCS_PROTOCOL + relativeFilePath)
                .fileInput(FileInput.FILE_PATH)
                .ingestorRoutines(getIngestorRoutines(requestMeta.getResourceTypeId()))
                .additionalProperties(getAdditionalProperties(srn))
                .build());

    return SubmitJobResult.builder()
        .jobId(submitFileResult.getJobId())
        .srn(srn)
        .build();
  }

  @Override
  public List<IngestedFile> getIngestionResult(JobsPullingResult jobsPullingResult,
      Map<String, SubmittedFile> jobIdToFile, RequestMeta requestMeta) {
    return jobsPullingResult.getCompletedJobs().stream()
        .map(response -> getIngestedFile(jobIdToFile, requestMeta, response))
        .collect(Collectors.toList());
  }

  private String generateSrn(ResourceTypeId resourceTypeId) {
    // FIXME: srn should be generated by standard
    String uuid = UUID.randomUUID().toString().replace("-", "");
    return resourceTypeId.getResourceType() == ResourceType.WPC_WELL_LOG ? "srn:file/las2:" + uuid
        + ":" : StringUtils.removeEnd(resourceTypeId.getRaw(), ":") + uuid + ":";
  }

  private String getAcl(Map<String, String> groupEmailByName) {
    return toJson(AclObject.builder()
        .acl(Acl.builder()
            .owner(groupEmailByName.get("data.default.owners"))
            .viewer(groupEmailByName.get("data.default.viewers"))
            .build())
        .build());
  }

  private String getIngestorRoutines(ResourceTypeId resourceTypeId) {
    return resourceTypeId.getResourceType() == ResourceType.WPC_WELL_LOG ? toJson(
        Collections.singletonList(LasIngestorObject.builder()
            .lasIngestor(LasIngestor.builder()
                .build())
            .build())) : null;
  }

  private String getAdditionalProperties(String srn) {
    HashMap<String, String> properties = new HashMap<>();
    properties.put("srn", srn);
    HashMap<String, Object> additionalProperties = new HashMap<>();
    additionalProperties.put("additionalProperties", properties);

    return toJson(additionalProperties);
  }

  private IngestedFile getIngestedFile(Map<String, SubmittedFile> jobIdToFile,
      RequestMeta requestMeta, JobStatusResponse response) {
    String location = GCS_PROTOCOL + response.getSummary().getOutputLocation()
        + SUCCESS_METADATA_JSON_PATH;
    log.debug("File location of ingestion job success metadata: {}", location);
    DelfiFile file = portalService.getFile(location, requestMeta.getAuthorizationToken(),
        requestMeta.getPartition());
    String fileContent = restTemplate.getForObject(file.getSignedUrl(), String.class);
    log.debug("Success metadata: {}", fileContent);
    SuccessMetadata metadata = toObject(fileContent, SuccessMetadata.class);
    SaveRecordsResult saveResult = toObject(metadata.getMessage(), SaveRecordsResult.class);
    log.debug("Save records result: {}", saveResult);

    return IngestedFile.builder()
        .submittedFile(jobIdToFile.get(response.getJobInfo().getJobId()))
        .recordId(saveResult.getRecordIds().get(0))
        .build();
  }

}
