package com.osdu.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osdu.client.delfi.DelfiDeliveryClient;
import com.osdu.client.delfi.DelfiFileClient;
import com.osdu.model.osdu.delivery.delfi.DelfiFileRecord;
import com.osdu.model.osdu.delivery.delfi.DelfiRecord;
import com.osdu.model.osdu.delivery.dto.DeliveryResponse;
import com.osdu.model.osdu.delivery.dto.ResponseItem;
import com.osdu.model.osdu.delivery.input.InputPayload;
import com.osdu.service.SrnMappingService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.osdu.service.delfi.DelfiDeliveryService.AUTHORIZATION_HEADER_KEY;
import static com.osdu.service.delfi.DelfiDeliveryService.PARTITION_HEADER_KEY;
import static com.osdu.service.processing.delfi.DelfiDataProcessingJob.FILE_LOCATION_KEY;
import static com.osdu.service.processing.delfi.DelfiDataProcessingJob.LOCATION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DeliveryFlowIntegrationTest {

  @MockBean
  private DelfiDeliveryClient deliveryClient;

  @MockBean
  private DelfiFileClient delfiFileClient;

  @MockBean
  private SrnMappingService srnMappingService;

  @Autowired
  private MockMvc mockMvc;

  private ObjectMapper mapper = new ObjectMapper();

  static final String TARGET_REGION = "targetRegion";
  static final String SIGNED_URL = "http://signed-url";
  static final String FILE_LOCATION = "fileLocation";
  static final String AUTHENTICATION = "authentication";
  static final String PARTITION = "partition";
  static final String NO_MAPPING_EXAMPLE = "no-mapping-example";
  static final String NO_LOCATION_EXAMPLE = "no-location-example";
  static final String LOCATION_EXAMPLE = "location-example";
  static final String ODES_ID_LOCATION = "odesId:location";

  @Test
  public void test() throws Exception {
    // given
    // no mapping record
    when(srnMappingService.mapSrnToKind(eq(NO_MAPPING_EXAMPLE))).thenReturn(null);

    // no file record
    DelfiRecord recordNoLocation = new DelfiRecord() {
    };
    Map<String, Object> data = new HashMap<>();
    data.put("test", "test");
    recordNoLocation.setData(data);
    when(srnMappingService.mapSrnToKind(eq(NO_LOCATION_EXAMPLE))).thenReturn("odesId:no:location");
    when(deliveryClient.getRecord(eq("odesId:no:location"), any(), any(), any()))
        .thenReturn(recordNoLocation);

    // file record
    DelfiRecord recordWithLocation = new DelfiRecord() {
    };
    Map<String, Object> dataLocation = new HashMap<>();
    dataLocation.put("one", "test");
    dataLocation.put(LOCATION_KEY, FILE_LOCATION);
    recordWithLocation.setData(dataLocation);
    when(srnMappingService.mapSrnToKind(eq(LOCATION_EXAMPLE))).thenReturn(ODES_ID_LOCATION);
    when(deliveryClient.getRecord(eq(ODES_ID_LOCATION), any(), any(), any()))
        .thenReturn(recordWithLocation);
    DelfiFileRecord fileRecord = new DelfiFileRecord() {
    };
    Map<String, Object> fileRecordDetails = new HashMap<>();
    fileRecordDetails.put("data", "data");
    fileRecordDetails.put(FILE_LOCATION_KEY, SIGNED_URL);
    fileRecord.setDetails(fileRecordDetails);
    when(delfiFileClient.getSignedUrlForLocation(eq(FILE_LOCATION), eq(AUTHENTICATION), eq(
        PARTITION), eq(PARTITION), anyString()))
        .thenReturn(fileRecord);

    // when
    List<String> srns = Arrays.asList(NO_LOCATION_EXAMPLE, NO_MAPPING_EXAMPLE, LOCATION_EXAMPLE);
    InputPayload payload = new InputPayload(srns, TARGET_REGION);
    HttpHeaders headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER_KEY, AUTHENTICATION);
    headers.add(PARTITION_HEADER_KEY, PARTITION);

    ResponseEntity responseEntity = (ResponseEntity) mockMvc
        .perform(MockMvcRequestBuilders.post("/")
            .headers(headers)
            .content(mapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andReturn().getAsyncResult();

    // then
    DeliveryResponse response = (DeliveryResponse) responseEntity.getBody();

    assertNotNull(response);
    assertEquals(Collections.singletonList(NO_MAPPING_EXAMPLE), response.getUnprocessedSrns());
    assertThat(response.getResult()).hasSize(2);
    List<ResponseItem> items = response.getResult();

    assertThat(items.get(0).getSrn()).isEqualTo(NO_LOCATION_EXAMPLE);
    assertThat(items.get(0).getFileLocation()).isNull();
    Map<String, Object> noLocationData = ((DelfiRecord) items.get(0).getData()).getData();
    assertThat(noLocationData.get("test")).isEqualTo("test");

    assertThat(items.get(1).getSrn()).isEqualTo(LOCATION_EXAMPLE);
    assertThat(items.get(1).getFileLocation()).isEqualTo(SIGNED_URL);
    Map<String, Object> locationData = ((DelfiFileRecord) items.get(1).getData()).getDetails();
    assertThat(locationData.get("data")).isEqualTo("data");
  }

}
