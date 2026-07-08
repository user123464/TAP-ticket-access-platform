package tw.com.ispan.backend.theme.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tw.com.ispan.backend.location.repository.LocationRepository;
import tw.com.ispan.backend.theme.repository.SessionRepository;
import tw.com.ispan.backend.theme.repository.ThemeRepository;
import tw.com.ispan.backend.theme.security.OrgRowSecurityGuard;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplSchedulerTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ThemeRepository themeRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private OrgRowSecurityGuard orgGuard;

    @InjectMocks
    private SessionServiceImpl sessionService;

    @Test
    void archiveExpiredSessionsJob_callsRepositoryBulkUpdate() {
        when(sessionRepository.archiveExpired(any())).thenReturn(3);

        sessionService.archiveExpiredSessionsJob();

        verify(sessionRepository).archiveExpired(any());
    }
}
