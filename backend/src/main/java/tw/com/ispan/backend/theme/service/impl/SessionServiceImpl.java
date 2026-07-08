package tw.com.ispan.backend.theme.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.location.entity.Location;
import tw.com.ispan.backend.location.repository.LocationRepository;
import tw.com.ispan.backend.theme.dto.request.SessionCreateRequest;
import tw.com.ispan.backend.theme.dto.request.SessionUpdateRequest;
import tw.com.ispan.backend.theme.dto.request.StatusUpdateRequest;
import tw.com.ispan.backend.theme.dto.response.SessionResponse;
import tw.com.ispan.backend.theme.entity.Session;
import tw.com.ispan.backend.theme.entity.Theme;
import tw.com.ispan.backend.theme.enums.Status;
import tw.com.ispan.backend.theme.repository.SessionRepository;
import tw.com.ispan.backend.theme.repository.ThemeRepository;
import tw.com.ispan.backend.theme.service.SessionService;
// [Jason] 跨組織 row-level 隔離查核（RBAC 缺口 #1）
import tw.com.ispan.backend.theme.security.OrgRowSecurityGuard;

@RequiredArgsConstructor
@Service
public class SessionServiceImpl implements SessionService {

	private final SessionRepository sessionRepository;
	private final ThemeRepository themeRepository;
	private final LocationRepository locationRepository;
	// [Jason] 跨組織隔離：場次的組織歸屬透過 session→theme→organizer 回推後反查呼叫者權限
	private final OrgRowSecurityGuard orgGuard;

	@Override
	public List<SessionResponse> getActiveSessionsByThemeId(Integer themeId) {
		List<Session> sessions = sessionRepository.findByThemeThemeIdAndStatus(
				themeId,
				Status.ACTIVE);
		List<SessionResponse> sessionResponse = sessions.stream()
				.map(session -> SessionResponse.fromEntity(session))
				.toList();
		return sessionResponse;
	}

	@Override
	public List<SessionResponse> getSessionsByThemeId(Integer themeId) {
		// [Jason] 跨組織隔離：後台讀取某活動的全部場次（含草稿），須為該活動所屬組織具 EVENT_VIEW 的成員，
		// 否則 A 廠商可帶 B 廠商的 themeId 偷看 B 的草稿場次。
		Theme theme = themeRepository.findById(themeId)
				.orElseThrow(() -> new RuntimeException("Theme not found"));
		orgGuard.requireOrgPermission(theme.getOrganizer().getOrganizerId(), "EVENT_VIEW");
		List<Session> sessions = sessionRepository.findByThemeThemeId(
				themeId);
		List<SessionResponse> sessionResponse = sessions.stream()
				.map(session -> SessionResponse.fromEntity(session))
				.toList();
		return sessionResponse;
	}

	@Override
	@Transactional
	public SessionResponse createSession(Integer themeId, SessionCreateRequest request) {

		if (themeId == null) {
			throw new IllegalArgumentException("themeId 不可為空");
		}
		Theme theme = themeRepository.findById(themeId)
				.orElseThrow(() -> new RuntimeException("Theme not found"));

		// [Jason] 跨組織隔離：只能在「自己組織」的活動下新增場次，須具 EVENT_EDIT（防 A 在 B 的活動塞場次）。
		orgGuard.requireOrgPermission(theme.getOrganizer().getOrganizerId(), "EVENT_EDIT");

		Integer locationId = request.locationId();
		if (locationId == null) {
			throw new IllegalArgumentException("locationId 不可為空");
		}
		Location location = locationRepository.findById(locationId)
				.orElseThrow(() -> new RuntimeException("Location not found"));

		Session session = new Session();

		session.setTheme(theme);
		session.setLocation(location);

		session.setTitle(request.title());
		session.setDetail(request.detail());
		session.setStartTime(request.startTime());
		session.setEndTime(request.endTime());
		session.setSellingStartTime(request.sellingStartTime());
		session.setSellingEndTime(request.sellingEndTime());

		session.setStatus(Status.DRAFT);

		sessionRepository.save(session);

		return SessionResponse.fromEntity(session);
	}

	// update session
	@Override
	@Transactional
	public SessionResponse updateSession(Integer sessionId, SessionUpdateRequest request) {
		if (sessionId == null) {
			throw new IllegalArgumentException("sessionId 不可為空");
		}
		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("Session not found"));

		// [Jason] 跨組織隔離：只能編輯「自己組織」活動下的場次，須具 EVENT_EDIT（防 A 改 B 的場次）。
		orgGuard.requireOrgPermission(session.getTheme().getOrganizer().getOrganizerId(), "EVENT_EDIT");

		session.setTitle(request.title());
		session.setDetail(request.detail());
		session.setStartTime(request.startTime());
		session.setEndTime(request.endTime());
		session.setSellingStartTime(request.sellingStartTime());
		session.setSellingEndTime(request.sellingEndTime());

		// Dirty checking handles updates
		return SessionResponse.fromEntity(session);
	}

	@Override
	@Transactional // 讓session還活著，避免LazyInitializationException
	public SessionResponse updateStatus(Integer sessionId, StatusUpdateRequest request) {
		if (sessionId == null) {
			throw new IllegalArgumentException("sessionId 不可為空");
		}
		if (request == null || request.status() == null) {
			throw new IllegalArgumentException("狀態不可為空");
		}
		Session session = sessionRepository.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("Session not found"));
		// [Jason] 跨組織隔離：場次狀態流轉（公開/軟刪除）須具 EVENT_PUBLISH 且為該場次所屬組織成員（防 A 動 B 的場次）。
		orgGuard.requireOrgPermission(session.getTheme().getOrganizer().getOrganizerId(), "EVENT_PUBLISH");
		session.setStatus(request.status());
		return SessionResponse.fromEntity(session);
	}

	@Override
	@Transactional
	public void archiveExpiredSessionsJob() {
		sessionRepository.archiveExpired(LocalDateTime.now());
	}

}