package tw.com.ispan.backend.theme.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tw.com.ispan.backend.location.entity.Location;
import tw.com.ispan.backend.location.repository.LocationRepository;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.theme.dto.request.SessionCreateRequest;
import tw.com.ispan.backend.theme.dto.request.SessionUpdateRequest;
import tw.com.ispan.backend.theme.dto.request.StatusUpdateRequest;
import tw.com.ispan.backend.theme.entity.Session;
import tw.com.ispan.backend.theme.entity.Theme;
import tw.com.ispan.backend.theme.enums.Status;
import tw.com.ispan.backend.theme.repository.SessionRepository;
import tw.com.ispan.backend.theme.repository.ThemeRepository;
import tw.com.ispan.backend.theme.security.OrgRowSecurityGuard;

/**
 * [Jason] 跨組織 row-level 隔離（RBAC 缺口 #1）單元測試 —— SessionServiceImpl。
 *
 * 場次的組織歸屬經 session → theme → organizer 回推；驗證寫/後台讀方法都以
 * 「資料所屬組織 + 正確權限碼」呼叫 {@link OrgRowSecurityGuard}，且被拒時不落地。
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceImplSecurityTest {

    private static final String OWNING_ORG = "ORG0000002";

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

    private Theme owningTheme() {
        Organizer org = Organizer.builder().organizerId(OWNING_ORG).build();
        Theme theme = new Theme();
        theme.setThemeId(5);
        theme.setOrganizer(org);
        return theme;
    }

    private Location aLocation() {
        Location loc = new Location();
        loc.setLocationId(1);
        loc.setName("台北小巨蛋");
        return loc;
    }

    private Session owningSession() {
        Session session = new Session();
        session.setSessionId(9);
        session.setTheme(owningTheme());
        session.setLocation(aLocation());
        session.setStatus(Status.DRAFT);
        session.setTitle("原場次");
        return session;
    }

    // ---- createSession：EVENT_EDIT（組織來自所屬活動）----

    @Test
    void createSession_checksEventEditAgainstThemeOrg() {
        when(themeRepository.findById(5)).thenReturn(Optional.of(owningTheme()));
        when(locationRepository.findById(1)).thenReturn(Optional.of(aLocation()));

        sessionService.createSession(5,
                new SessionCreateRequest(1, "場次", "內容", null, null, null, null));

        verify(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_EDIT");
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void createSession_deniedBlocksSaveAndLocationLookup() {
        when(themeRepository.findById(5)).thenReturn(Optional.of(owningTheme()));
        doThrow(new SecurityException("跨組織越權"))
                .when(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_EDIT");

        assertThrows(SecurityException.class, () -> sessionService.createSession(5,
                new SessionCreateRequest(1, "場次", "內容", null, null, null, null)));

        verify(locationRepository, never()).findById(any());
        verify(sessionRepository, never()).save(any());
    }

    // ---- updateSession：EVENT_EDIT ----

    @Test
    void updateSession_checksEventEditAgainstSessionOrg() {
        when(sessionRepository.findById(9)).thenReturn(Optional.of(owningSession()));

        sessionService.updateSession(9,
                new SessionUpdateRequest("新場次", "內容", null, null, null, null));

        verify(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_EDIT");
    }

    @Test
    void updateSession_deniedDoesNotMutate() {
        Session session = owningSession();
        when(sessionRepository.findById(9)).thenReturn(Optional.of(session));
        doThrow(new SecurityException("跨組織越權"))
                .when(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_EDIT");

        assertThrows(SecurityException.class, () -> sessionService.updateSession(9,
                new SessionUpdateRequest("新場次", "內容", null, null, null, null)));

        assertEquals("原場次", session.getTitle(), "被擋下時欄位不應變更（dirty checking 不會生效）");
    }

    // ---- updateStatus：EVENT_PUBLISH ----

    @Test
    void updateStatus_checksEventPublishAgainstSessionOrg() {
        when(sessionRepository.findById(9)).thenReturn(Optional.of(owningSession()));

        sessionService.updateStatus(9, new StatusUpdateRequest(Status.ACTIVE));

        verify(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_PUBLISH");
    }

    @Test
    void updateStatus_deniedDoesNotChangeStatus() {
        Session session = owningSession();
        when(sessionRepository.findById(9)).thenReturn(Optional.of(session));
        doThrow(new SecurityException("跨組織越權"))
                .when(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_PUBLISH");

        assertThrows(SecurityException.class,
                () -> sessionService.updateStatus(9, new StatusUpdateRequest(Status.ACTIVE)));

        assertEquals(Status.DRAFT, session.getStatus(), "被擋下時狀態不應變更");
    }

    @Test
    void updateStatus_nullRequest_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> sessionService.updateStatus(9, null));
    }

    @Test
    void updateStatus_nullStatus_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> sessionService.updateStatus(9, new StatusUpdateRequest(null)));
    }

    // ---- getSessionsByThemeId（後台讀）：EVENT_VIEW ----

    @Test
    void getSessionsByThemeId_checksEventViewAgainstThemeOrg() {
        when(themeRepository.findById(5)).thenReturn(Optional.of(owningTheme()));
        when(sessionRepository.findByThemeThemeId(5)).thenReturn(List.of());

        sessionService.getSessionsByThemeId(5);

        verify(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_VIEW");
    }

    @Test
    void getSessionsByThemeId_deniedBlocksQuery() {
        when(themeRepository.findById(5)).thenReturn(Optional.of(owningTheme()));
        doThrow(new SecurityException("跨組織越權"))
                .when(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_VIEW");

        assertThrows(SecurityException.class, () -> sessionService.getSessionsByThemeId(5));

        verify(sessionRepository, never()).findByThemeThemeId(any());
    }
}
