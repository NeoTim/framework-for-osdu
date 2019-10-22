package com.osdu.service.delfi;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.osdu.exception.IngestException;
import com.osdu.model.IngestResult;
import com.osdu.model.manifest.LoadManifest;
import com.osdu.service.JobStatusService;
import com.osdu.service.processing.InnerInjectionProcess;
import com.osdu.service.validation.LoadManifestValidationService;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.MessageHeaders;

@RunWith(MockitoJUnitRunner.class)
public class DelfiIngestServiceTest {

  private static final String JOB_ID = "jobId";

  @Mock
  JobStatusService jobStatusService;
  @Mock
  InnerInjectionProcess innerInjectionProcess;
  @Mock
  private ProcessingReport processingReport;
  @Mock
  LoadManifestValidationService loadManifestValidationService;

  @InjectMocks
  private DelfiIngestService delfiIngestService;

  @Test
  public void shouldIngestManifest() {

    // given
    LoadManifest manifest = LoadManifest.builder().build();
    MessageHeaders headers = new MessageHeaders(new HashMap<>());

    when(loadManifestValidationService
        .validateManifest(eq(manifest))).thenReturn(processingReport);
    when(processingReport.isSuccess()).thenReturn(true);
    when(jobStatusService.initInjectJob()).thenReturn(JOB_ID);

    // when
    IngestResult ingestResult = delfiIngestService.ingestManifest(manifest, headers);

    // then
    verify(innerInjectionProcess).process(eq(JOB_ID), eq(manifest), eq(headers));
    assertThat(ingestResult.getJobId()).isEqualTo(JOB_ID);
  }

  @Test(expected = IngestException.class)
  public void shouldNotInitiateJobIfManifestValidationFail() {

    // given
    LoadManifest manifest = LoadManifest.builder().build();
    MessageHeaders headers = new MessageHeaders(new HashMap<>());

    when(loadManifestValidationService.validateManifest(eq(manifest))).thenReturn(processingReport);
    when(processingReport.isSuccess()).thenReturn(false);

    // when
    delfiIngestService.ingestManifest(manifest, headers);

    // then
    verify(jobStatusService, never()).initInjectJob();
    verify(innerInjectionProcess, never()).process(any(), any(), any());
  }
}