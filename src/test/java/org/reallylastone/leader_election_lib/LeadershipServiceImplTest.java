package org.reallylastone.leader_election_lib;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadershipServiceImplTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    private LeadershipServiceImpl leadershipService;

    @BeforeEach
    void setup() {
        leadershipService = new LeadershipServiceImpl(dataSource, 1, 30_000);
    }

    private void stubStatement() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void amILeaderIsFalseUntilLeadershipAcquired() {
        assertFalse(leadershipService.amILeader());
    }

    @Test
    void tryAcquireLeadershipSucceedsWhenUpdateAffectsRow() throws SQLException {
        stubStatement();
        when(preparedStatement.executeUpdate()).thenReturn(1);

        assertTrue(leadershipService.tryAcquireLeadership("node-42"));
        assertTrue(leadershipService.amILeader());
    }

    @Test
    void tryAcquireLeadershipFailsWhenNoRowAffected() throws SQLException {
        stubStatement();
        when(preparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(leadershipService.tryAcquireLeadership("node-42"));
        assertFalse(leadershipService.amILeader());
    }

    @Test
    void updateHeartbeatFailsWhenNoLongerLeader() throws SQLException {
        stubStatement();
        when(preparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(leadershipService.updateHeartbeat("node-42"));
        assertFalse(leadershipService.amILeader());
    }

    @Test
    void usesConfiguredAppIdInQuery() throws SQLException {
        leadershipService = new LeadershipServiceImpl(dataSource, 7, 30_000);
        stubStatement();
        when(preparedStatement.executeUpdate()).thenReturn(1);

        leadershipService.tryAcquireLeadership("node-42");

        verify(preparedStatement).setInt(2, 7);
    }

    @Test
    void failsClosedWhenSqlExceptionIsThrown() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        assertFalse(leadershipService.tryAcquireLeadership("node-42"));
        assertFalse(leadershipService.amILeader());
    }
}
