package org.reallylastone.leader_election_lib;

import java.net.InetAddress;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class AcquireLeadershipJob implements Runnable {
    private final LeadershipService leadershipService;
    private final String nodeId;

    @SneakyThrows
    public AcquireLeadershipJob(LeadershipService leadershipService) {
        this.leadershipService = leadershipService;
        this.nodeId = String.format("%s-%s-%s", InetAddress.getLocalHost().getHostAddress(),
                ProcessHandle.current().pid(), UUID.randomUUID());
    }

    @Override
    public void run() {
        if (leadershipService.amILeader()) {
            leadershipService.updateHeartbeat(nodeId);
        } else if (leadershipService.tryAcquireLeadership(nodeId)) {
            log.info("{} acquired leadership", nodeId);
        } else {
            log.trace("{} not a leader", nodeId);
        }
    }
}
