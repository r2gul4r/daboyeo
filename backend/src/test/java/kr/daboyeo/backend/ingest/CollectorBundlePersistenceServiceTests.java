package kr.daboyeo.backend.ingest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import kr.daboyeo.backend.config.CollectorSyncProperties;
import org.junit.jupiter.api.Test;

class CollectorBundlePersistenceServiceTests {

    @Test
    void retriesTransientConnectionFailuresAndEventuallySucceeds() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection firstConnection = mock(Connection.class);
        Connection secondConnection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(firstConnection, secondConnection);
        when(firstConnection.getAutoCommit()).thenReturn(true);
        when(secondConnection.getAutoCommit()).thenReturn(true);
        when(firstConnection.isClosed()).thenReturn(false);
        when(secondConnection.isClosed()).thenReturn(false);
        when(firstConnection.prepareStatement(anyString())).thenReturn(statement);
        when(secondConnection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeBatch()).thenReturn(new int[0]);
        when(statement.executeUpdate()).thenReturn(0);
        doThrow(new SQLTransientConnectionException("Communications link failure")).when(firstConnection).setAutoCommit(false);

        CollectorSyncProperties properties = new CollectorSyncProperties();
        properties.getShowtimes().setPersistenceMaxRetries(1);
        properties.getShowtimes().setPersistenceRetryBackoffMillis(0);

        TheaterLocationEnricher enricher = mock(TheaterLocationEnricher.class);
        when(enricher.enrich(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CollectorBundlePersistenceService service = new CollectorBundlePersistenceService(
            dataSource,
            properties,
            new ObjectMapper(),
            enricher
        );

        service.persist("LOTTE_CINEMA", Map.of(), false);

        verify(dataSource, times(2)).getConnection();
        verify(secondConnection, times(1)).commit();
    }

    @Test
    void doesNotRetryNonTransientSqlFailures() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        doThrow(new SQLException("syntax error", "42000")).when(connection).setAutoCommit(false);

        CollectorSyncProperties properties = new CollectorSyncProperties();
        properties.getShowtimes().setPersistenceMaxRetries(2);
        properties.getShowtimes().setPersistenceRetryBackoffMillis(0);

        TheaterLocationEnricher enricher = mock(TheaterLocationEnricher.class);
        when(enricher.enrich(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CollectorBundlePersistenceService service = new CollectorBundlePersistenceService(
            dataSource,
            properties,
            new ObjectMapper(),
            enricher
        );

        assertThatThrownBy(() -> service.persist("LOTTE_CINEMA", Map.of(), false))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to persist collector bundle");

        verify(dataSource, times(1)).getConnection();
    }
}
