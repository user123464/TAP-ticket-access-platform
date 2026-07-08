import { defineStore } from "pinia";
import { ref, computed } from "vue";
import * as themeSessionApi from "@/api/themeSessionApi";
import * as auctionApi from "@/api/auctionApi";

export const useThemeStore = defineStore("theme", () => {

  // 1. State（資料）
  const themes = ref([]);
  const sessions = ref([]);
  const locations = ref([]);
  const auctions = ref([]);

  const selectedTheme = ref(null);
  const selectedSession = ref(null);

  const themeFilter = ref("ALL");
  const sessionFilter = ref("ALL");
  const auctionFilter = ref("ALL");

  const loadingThemes = ref(false);
  const loadingSessions = ref(false);
  const loadingLocations = ref(false);
  const loadingAuctions = ref(false);

  // 2. Getters（computed）
  const filteredThemes = computed(() => {
    if (themeFilter.value === "ALL") return themes.value;
    return themes.value.filter((t) => t.status === themeFilter.value);
  });

  const filteredSessions = computed(() => {
    if (sessionFilter.value === "ALL") return sessions.value;
    return sessions.value.filter((s) => s.status === sessionFilter.value);
  });

  const filteredAuctions = computed(() => {
    if (auctionFilter.value === "ALL") return auctions.value;
    return auctions.value.filter((a) => a.status === auctionFilter.value);
  });

  // 3. Actions（直接打 API）

  //Modal Theme ================================================
  async function publishTheme(themeId, organizerId) {
    await themeSessionApi.updateThemeStatus(themeId, "ACTIVE");
    await fetchThemes(organizerId);
  }

  async function confirmDeleteTheme(themeId, organizerId) {
    if (window.confirm("確定要刪除此活動嗎？此操作無法復原。")) {
      await deleteTheme(themeId, organizerId);
      // 如果刪除的是當前選取的活動，則清空選取狀態
      if (selectedTheme.value?.themeId === themeId) {
        selectedTheme.value = null;
        sessions.value = [];
      }
    }
  }

  async function deleteTheme(themeId, organizerId) {
    try {
      await themeSessionApi.updateThemeStatus(themeId, "DELETED");
      await fetchThemes(organizerId);
    } catch (e) {
      // 如果 API 回傳錯誤訊息，則拋出錯誤
      // 先從後端回傳的錯誤訊息中取出 message，如果沒有則使用 axios 的 message
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function updateTheme(data, organizerId) {
    try {
      const { pendingImageFile, ...payload } = data;
      const updatedTheme = await themeSessionApi.updateTheme(payload.themeId, payload);

      await fetchThemes(organizerId);
      return updatedTheme;
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function createTheme(data, organizerId) {
    try {
      const { pendingImageFile, ...payload } = data;
      const createdTheme = await themeSessionApi.createTheme({ ...payload, organizerId });
      await fetchThemes(organizerId);
      return createdTheme;
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function uploadThemeImage(themeId, file) {
    const res = await themeSessionApi.uploadThemeImage(themeId, file);

    if (!res?.success) {
      throw new Error(res?.message);
    }

    return res?.data;
  }

  async function uploadAuctionImage(auctionId, file) {
    const res = await auctionApi.uploadAuctionImage(auctionId, file);

    if (!res?.success) {
      throw new Error(res?.message);
    }

    return res?.data;
  }

  async function clearThemeImage(themeId, payload) {
    const res = await themeSessionApi.clearThemeImage(themeId, payload);

    if (!res?.success) {
      throw new Error(res?.message);
    }

    return res?.data;
  }

  //Modal Session ======================================================
  async function publishSession(sessionId) {
    try {
      if (selectedTheme.value?.status !== "ACTIVE") {
        throw new Error("請先將活動公開後，再公開場次");
      }
      await themeSessionApi.updateSessionStatus(sessionId, "ACTIVE");
      await fetchSessions(selectedTheme.value.themeId);
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function confirmDeleteSession(sessionId) {
    if (window.confirm("確定要刪除此場次嗎？")) {
      await deleteSession(sessionId);
      // 如果刪除的是當前選取的場次
      if (selectedSession.value?.sessionId === sessionId) {
        selectedSession.value = null;
      }
    }
  }

  async function deleteSession(sessionId) {
    try {
      await themeSessionApi.updateSessionStatus(sessionId, "DELETED");
      await fetchSessions(selectedTheme.value.themeId);
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function updateSession(data, sessionId) {
    try {
      await themeSessionApi.updateSession(sessionId, data);
      await fetchSessions(selectedTheme.value.themeId);
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function createSession(data, themeId) {
    try {
      await themeSessionApi.createSession(themeId, data);
      await fetchSessions(themeId);
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  //Modal Auction ======================================================
  async function publishAuction(auctionId) {
    try {
      if (selectedTheme.value?.status !== "ACTIVE") {
        throw new Error("請先將活動公開後，再公開競標");
      }
      await auctionApi.publishAuction(auctionId);
      if (selectedTheme.value) {
        await fetchAuctions(selectedTheme.value.themeId);
      }
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function endAuction(auctionId) {
    try {
      await auctionApi.endAuction(auctionId);
      if (selectedTheme.value) {
        await fetchAuctions(selectedTheme.value.themeId);
      }
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function confirmDeleteAuction(auctionId) {
    if (window.confirm("確定要刪除此競標嗎？")) {
      await deleteAuction(auctionId);
    }
  }

  async function deleteAuction(auctionId) {
    try {
      await auctionApi.deleteAuction(auctionId);
      if (selectedTheme.value) {
        await fetchAuctions(selectedTheme.value.themeId);
      }
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function updateAuction(data, auctionId) {
    try {
      await auctionApi.updateAuction(auctionId, data);
      await fetchAuctions(selectedTheme.value.themeId);
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }

  async function createAuction(data, themeId) {
    try {
      const createdAuction = await auctionApi.createAuction(themeId, data);
      await fetchAuctions(themeId);
      return createdAuction;
    } catch (e) {
      throw new Error(e?.response?.data?.message ?? e?.message);
    }
  }


  //====fetch=========================================================

  async function selectTheme(theme) {
    selectedTheme.value = theme;
    selectedSession.value = null;
    await fetchSessions(theme.themeId);
    await fetchAuctions(theme.themeId);
  }


  async function fetchThemes(organizerId) {
    loadingThemes.value = true;
    try {
      themes.value = await themeSessionApi.getThemesByOrganizer(organizerId);

      if (selectedTheme.value) {
        const latestSelectedTheme = themes.value.find(
          (theme) => theme.themeId === selectedTheme.value.themeId
        );

        if (latestSelectedTheme) {
          selectedTheme.value = latestSelectedTheme;
        }
      }
    } finally {
      loadingThemes.value = false;
    }
  }

  async function fetchSessions(themeId) {
    loadingSessions.value = true;
    try {
      sessions.value = await themeSessionApi.getSessionsByTheme(themeId);
    } finally {
      loadingSessions.value = false;
    }
  }

    async function fetchAuctions(themeId) {
    loadingAuctions.value = true;
    try {
      auctions.value = await auctionApi.getOrgAuctions(themeId) ?? [];
    } finally {
      loadingAuctions.value = false;
    }
  }

  // 別人的api
  async function fetchLocations() {
    loadingLocations.value = true;
    try {
      locations.value = await themeSessionApi.getLocations();
      return locations.value;
    } finally {
      loadingLocations.value = false;
    }
  }

  function selectSession(session) {
    selectedSession.value = session;
  }

  // 4. return
  return {
    // state
    themes,
    sessions,
    locations,
    auctions,
    selectedTheme,
    selectedSession,

    themeFilter,
    sessionFilter,
    auctionFilter,

    loadingThemes,
    loadingSessions,
    loadingLocations,
    loadingAuctions,

    // getters
    filteredThemes,
    filteredSessions,
    filteredAuctions,

    // actions
    fetchThemes,
    fetchSessions,
    fetchLocations,
    fetchAuctions,
    selectTheme,
    selectSession,
    createTheme,
    uploadThemeImage,
    uploadAuctionImage,
    clearThemeImage,
    deleteTheme,
    confirmDeleteTheme,
    updateTheme,
    publishTheme,
    createSession,
    updateSession,
    deleteSession,
    confirmDeleteSession,
    publishSession,
    createAuction,
    updateAuction,
    deleteAuction,
    confirmDeleteAuction,
    publishAuction,
    endAuction
  };
});

