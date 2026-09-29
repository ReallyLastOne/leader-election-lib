package org.reallylastone.leader_election_lib;

public interface LeadershipService {

    boolean amILeader();

    boolean updateHeartbeat(String nodeId);

    boolean tryAcquireLeadership(String nodeId);
}
