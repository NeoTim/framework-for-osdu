package com.osdu.service.delfi;

import com.osdu.client.delfi.DelfiDeliveryClient;
import com.osdu.client.delfi.DelfiFileClient;
import com.osdu.exception.OSDUException;
import com.osdu.model.osdu.delivery.FileRecord;
import com.osdu.model.osdu.delivery.Record;
import com.osdu.model.osdu.delivery.delfi.ProcessingResultStatus;
import com.osdu.model.osdu.delivery.dto.ResponseItem;
import com.osdu.model.osdu.delivery.input.Srns;
import com.osdu.model.osdu.delivery.dto.DeliveryResponse;
import com.osdu.service.DeliveryService;
import com.osdu.service.PortalService;
import com.osdu.service.SRNMappingService;
import com.osdu.service.processing.DataProcessingJob;
import com.osdu.service.processing.delfi.DelfiDataProcessingJob;
import com.osdu.model.osdu.delivery.delfi.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class DelfiDeliveryService implements DeliveryService {

    private final static Logger log = LoggerFactory.getLogger(DelfiDeliveryService.class);
    private static final int THREAD_POOL_CAPACITY = 3;
    private static final String PARTITION_HEADER_KEY = "partition";
    private static final String AUTHORIZATION_HEADER_KEY = "authorization";

    @Inject
    private SRNMappingService srnMappingService;

    @Inject
    private PortalService portalService;

    @Override
    public DeliveryResponse getResources(Srns srns, MessageHeaders headers) {
        log.debug("Getting resources for following SRNs and headers : {}, {}", srns, headers);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_CAPACITY);

        String authorizationToken = extractHeaders(headers, AUTHORIZATION_HEADER_KEY);
        String partition = extractHeaders(headers, PARTITION_HEADER_KEY);


        List<DataProcessingJob> jobs = srns.getSrns().stream()
                .map(srn -> new DelfiDataProcessingJob(srn, srnMappingService, portalService, authorizationToken, partition))
                .collect(Collectors.toList());

        List<Future<ProcessingResult>> futures = new ArrayList<>();
        for (DataProcessingJob job : jobs) {
            futures.add(executor.submit(job));
        }

        List<ProcessingResult> results = new ArrayList<>();
        for (Future<ProcessingResult> job : futures) {
            try {
                results.add(job.get());
            } catch (ExecutionException | InterruptedException e) {
                log.error("Error execution srn", e);
                throw new OSDUException("Error execution srn");
            }
        }
        executor.shutdown();

        return processResults(results);
    }

    private DeliveryResponse processResults(List<ProcessingResult> results) {
        log.debug("Processing results : {}", results);
        List<ResponseItem> responseItems = new ArrayList<>();
        List<String> unprocessedSrns = new ArrayList<>();
        results.stream().forEach(result -> {
            if (!result.getProcessingResultStatus().equals(ProcessingResultStatus.NO_MAPPING)) {
                responseItems.add(ResponseItem.builder()
                        .fileLocation(result.getFileLocation())
                        .data(result.getData())
                        .srn(result.getSrn()).build());
            } else {
                unprocessedSrns.add(result.getSrn());
            }
        });

        DeliveryResponse response = new DeliveryResponse();
        response.setResult(responseItems);
        response.setUnprocessedSRNs(unprocessedSrns);

        return response;
    }

    private String extractHeaders(MessageHeaders headers, String headerKey) {
        log.debug("Extracting header with name : {} from map : {}", headerKey, headers);
        if (headers.containsKey(headerKey)) {
            String result = (String) headers.get(headerKey);
            log.debug("Found header in the request with following key:value pair : {}:{}", headerKey, result);
            return result;
        }
        return null;
    }
}

