package com.tiktokinsight.watchlist.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tiktokinsight.common.exception.ApiException;
import com.tiktokinsight.watchlist.domain.WatchlistRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class WatchlistServiceTest {
    private final WatchlistRepository repository = mock(WatchlistRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC);
    private final WatchlistService service = new WatchlistService(repository, clock);

    @Test
    void addsOnlyForTheAuthenticatedUsersScope() {
        when(repository.productExists(101L)).thenReturn(true);
        when(repository.add(9L, 101L, clock.instant())).thenReturn(true);

        service.add(9L, 101L);

        verify(repository).add(9L, 101L, clock.instant());
    }

    @Test
    void rejectsDuplicateUserProductPairs() {
        when(repository.productExists(101L)).thenReturn(true);
        when(repository.add(9L, 101L, clock.instant())).thenReturn(false);

        assertThrows(ApiException.class, () -> service.add(9L, 101L));
    }

    @Test
    void doesNotAllowInvalidPaging() {
        assertThrows(ApiException.class, () -> service.list(9L, null, 0, 20));
    }
}
