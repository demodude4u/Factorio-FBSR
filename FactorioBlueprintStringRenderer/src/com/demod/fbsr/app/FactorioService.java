package com.demod.fbsr.app;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.demod.fbsr.FBSR;
import com.demod.fbsr.Profile;
import com.google.common.util.concurrent.AbstractIdleService;

public class FactorioService extends AbstractIdleService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FactorioService.class);

  private List<String> requestedProfiles;

  public FactorioService(List<String> requestedProfiles) {
    this.requestedProfiles = requestedProfiles;
  }

  @Override
  protected void startUp() throws Exception {
    LOGGER.info("Waiting on RPC service to start...");
    ServiceFinder.findService(RPCService.class).get().awaitRunning();

    if (requestedProfiles == null || requestedProfiles.isEmpty()) {
      if (!FBSR.load()) {
        throw new RuntimeException("Failed to initialize FBSR.");
      }

    } else {
      List<Profile> profiles = new ArrayList<>();
      requestedProfiles.stream().map(Profile::byName).forEach(profiles::add);
      if (!profiles.stream().anyMatch(Profile::isVanilla)) {
        profiles.add(0, Profile.vanilla());
      }

      if (!FBSR.load(profiles)) {
        throw new RuntimeException("Failed to initialize FBSR with requested profiles: " 
            + requestedProfiles.stream().collect(Collectors.joining(", ")));
      }
    }
  }

  @Override
  protected void shutDown() throws Exception {
    if (!FBSR.unload()) {
      throw new RuntimeException("Failed to shutdown FBSR.");
    }
  }
}
