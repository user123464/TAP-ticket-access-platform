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

import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.theme.dto.request.StatusUpdateRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeCreateRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeUpdateRequest;
import tw.com.ispan.backend.theme.entity.Theme;
import tw.com.ispan.backend.theme.enums.Status;
import tw.com.ispan.backend.theme.repository.ThemeRepository;
import tw.com.ispan.backend.theme.security.OrgRowSecurityGuard;

/**
 * [Jason] 跨組織 row-level 隔離（RBAC 缺口 #1）單元測試 —— ThemeServiceImpl。
 *
 * 重點驗證兩件事：
 *   1. 每個寫/後台讀方法都以「資料所屬組織 + 正確權限碼」呼叫 {@link OrgRowSecurityGuard}。
 *   2. 當 guard 拒絕（拋 SecurityException，模擬「A 廠商動 B 廠商的資料」）時，操作被擋下、不落地。
 */
@ExtendWith(MockitoExtension.class)
class ThemeServiceImplSecurityTest {

    private static final String OWNING_ORG = "ORG0000002";

    @Mock
    private ThemeRepository themeRepository;
    @Mock
    private OrganizerRepository organizerRepository;
    @Mock
    private OrgRowSecurityGuard orgGuard;

    @InjectMocks
    private ThemeServiceImpl themeService;

    private Theme owningTheme(Integer themeId) {
        Organizer org = Organizer.builder().organizerId(OWNING_ORG).build();
        Theme theme = new Theme();
        theme.setThemeId(themeId);
        theme.setOrganizer(org);
        theme.setStatus(Status.DRAFT);
        theme.setTitle("原標題");
        return theme;
    }

    // ---- updateTheme：EVENT_EDIT ----

    @Test
    void updateTheme_checksEventEditAgainstOwningOrg() {
        when(themeRepository.findById(10)).thenReturn(Optional.of(owningTheme(10)));

        themeService.updateTheme(10, new ThemeUpdateRequest("新標題", "內容", "img"));

        verify(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_EDIT");
        verify(themeRepository).save(any(Theme.class));
    }

    @Test
    void updateTheme_deniedBlocksSave() {
        when(themeRepository.findById(10)).thenReturn(Optional.of(owningTheme(10)));
        doThrow(new SecurityException("跨組織越權"))
                .when(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_EDIT");

        assertThrows(SecurityException.class,
                () -> themeService.updateTheme(10, new ThemeUpdateRequest("新標題", "內容", "img")));

        verify(themeRepository, never()).save(any());
    }

    // ---- updateStatus：EVENT_PUBLISH ----

    @Test
    void updateStatus_checksEventPublishAgainstOwningOrg() {
        when(themeRepository.findById(10)).thenReturn(Optional.of(owningTheme(10)));

        themeService.updateStatus(10, new StatusUpdateRequest(Status.ACTIVE));

        verify(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_PUBLISH");
        verify(themeRepository).save(any(Theme.class));
    }

    @Test
    void updateStatus_deniedBlocksStatusChangeAndSave() {
        Theme theme = owningTheme(10);
        when(themeRepository.findById(10)).thenReturn(Optional.of(theme));
        doThrow(new SecurityException("跨組織越權"))
                .when(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_PUBLISH");

        assertThrows(SecurityException.class,
                () -> themeService.updateStatus(10, new StatusUpdateRequest(Status.ACTIVE)));

        assertEquals(Status.DRAFT, theme.getStatus(), "被擋下時狀態不應變更");
        verify(themeRepository, never()).save(any());
    }

    // ---- createTheme：EVENT_CREATE（組織來自前端帶入的 request.organizerId）----

    @Test
    void createTheme_checksEventCreateAgainstRequestedOrg() {
        Organizer org = Organizer.builder().organizerId(OWNING_ORG).build();
        when(organizerRepository.findById(OWNING_ORG)).thenReturn(Optional.of(org));

        themeService.createTheme(new ThemeCreateRequest("活動", "內容", "img", OWNING_ORG));

        verify(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_CREATE");
        verify(themeRepository).save(any(Theme.class));
    }

    @Test
    void createTheme_deniedBlocksSaveAndDoesNotLoadOrg() {
        doThrow(new SecurityException("跨組織越權"))
                .when(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_CREATE");

        assertThrows(SecurityException.class,
                () -> themeService.createTheme(new ThemeCreateRequest("活動", "內容", "img", OWNING_ORG)));

        verify(organizerRepository, never()).findById(any());
        verify(themeRepository, never()).save(any());
    }

    @Test
    void createTheme_missingOrganizerId_throwsBeforeGuard() {
        assertThrows(IllegalArgumentException.class,
                () -> themeService.createTheme(new ThemeCreateRequest("活動", "內容", "img", null)));

        verify(orgGuard, never()).requireOrgPermission(any(), any());
        verify(themeRepository, never()).save(any());
    }

    // ---- getThemesByOrganizerId：EVENT_VIEW ----

    @Test
    void getThemesByOrganizerId_checksEventViewAgainstRequestedOrg() {
        when(themeRepository.findByOrganizerOrganizerId(OWNING_ORG)).thenReturn(List.of());

        themeService.getThemesByOrganizerId(OWNING_ORG);

        verify(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_VIEW");
    }

    @Test
    void getThemesByOrganizerId_deniedBlocksQuery() {
        doThrow(new SecurityException("跨組織越權"))
                .when(orgGuard).requireOrgPermission(OWNING_ORG, "EVENT_VIEW");

        assertThrows(SecurityException.class,
                () -> themeService.getThemesByOrganizerId(OWNING_ORG));

        verify(themeRepository, never()).findByOrganizerOrganizerId(any());
    }
}
