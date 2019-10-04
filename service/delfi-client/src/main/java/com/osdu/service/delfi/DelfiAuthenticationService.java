package com.osdu.service.delfi;

import com.osdu.client.DelfiEntitlementsClient;
import com.osdu.model.property.DelfiPortalProperties;
import com.osdu.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DelfiAuthenticationService implements AuthenticationService {

  final DelfiEntitlementsClient delfiEntitlementsClient;

  final DelfiPortalProperties portalProperties;

  @Override
  public void checkCredentials(String authorizationToken, String partition) {
    log.debug("Start authentication : {}, {}, {}", authorizationToken, portalProperties.getAppKey(),
        partition);

    delfiEntitlementsClient
        .getUserGroups(authorizationToken, portalProperties.getAppKey(), partition);

    log.debug("Authentication finished");
  }
}

