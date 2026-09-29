package org.reallylastone.leader_election_lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

public class LeadershipServiceImpl implements LeadershipService {

    private static final Logger log = LoggerFactory.getLogger(LeadershipServiceImpl.class);

    private static final String UPDATE_HEARTBEAT_SQL =
            "UPDATE leader SET renew_time = now() WHERE id = ? AND node_id = ?";
    private static final String TRY_ACQUIRE_SQL =
            "UPDATE leader SET renew_time = now(), acquire_time = now(), node_id = ? "
                    + "WHERE id = ? AND (node_id IS NULL OR renew_time < ?)";

    private final DataSource dataSource;
    private final int appId;
    private final long leaderTimeoutMs;
    private boolean amILeader;
    private Instant renewTime;

    public LeadershipServiceImpl(DataSource dataSource, int appId, long leaderTimeoutMs) {
        this.dataSource = dataSource;
        this.appId = appId;
        this.leaderTimeoutMs = leaderTimeoutMs;
    }

    @Override
    public boolean amILeader() {
        if (renewTime == null || renewTime.isBefore(Instant.now().minusMillis(leaderTimeoutMs))) {
            amILeader = false;
        }
        return amILeader;
    }

    @Override
    public boolean updateHeartbeat(String nodeId) {
        return executeUpdate(UPDATE_HEARTBEAT_SQL, stmt -> {
            stmt.setInt(1, appId);
            stmt.setString(2, nodeId);
        });
    }

    @Override
    public boolean tryAcquireLeadership(String nodeId) {
        return executeUpdate(TRY_ACQUIRE_SQL, stmt -> {
            stmt.setString(1, nodeId);
            stmt.setInt(2, appId);
            stmt.setTimestamp(3, Timestamp.from(Instant.now().minusMillis(leaderTimeoutMs)));
        });
    }

    private boolean executeUpdate(String sql, SQLConsumer<PreparedStatement> paramSetter) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            paramSetter.accept(stmt);
            boolean success = stmt.executeUpdate() > 0;
            amILeader = success;
            if (success) {
                renewTime = Instant.now();
            }
            return success;
        } catch (Exception e) {
            log.error("Error executing SQL: {}", sql, e);
            amILeader = false;
            return false;
        }
    }

    @FunctionalInterface
    private interface SQLConsumer<T> {
        void accept(T t) throws Exception;
    }
}
