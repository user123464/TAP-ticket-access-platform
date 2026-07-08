import axios from "@/plugins/axios";

// 前台
export async function getThemes(page, size, keyword, filter) {
  const res = await axios.get('/api/themes', {
    params: { page, size, keyword, filter }
  });
  return res.data?.data;
}

export async function getThemesByOrganizer(organizerId) {
  const res = await axios.get(`/api/org/organizers/${organizerId}/themes`);
  return res.data?.data;
}

export async function updateThemeStatus(themeId, status) {
  const res = await axios.put(`/api/org/themes/${themeId}/status`, { status });
  return res.data;
}

export async function updateTheme(themeId, payload) {
  const res = await axios.put(`/api/org/themes/${themeId}`, payload);
  return res.data?.data;
}

export async function createTheme(payload) {
  const res = await axios.post("/api/org/themes", payload);
  return res.data?.data;
}

export async function uploadThemeImage(themeId, file) {
  const formData = new FormData();
  formData.append("file", file);

  const res = await axios.post("/api/org/themes/upload-image", formData, {
    params: { themeId },
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });

  return res.data;
}

export async function clearThemeImage(themeId, payload) {
  const res = await axios.put(`/api/org/themes/${themeId}/image`, payload);
  return res.data;
}

export async function getSessionsByTheme(themeId) {
  const res = await axios.get(`/api/org/themes/${themeId}/sessions`);
  return res.data?.data;
}

export async function updateSessionStatus(sessionId, status) {
  const res = await axios.put(`/api/org/sessions/${sessionId}/status`, { status });
  return res.data;
}

export async function updateSession(sessionId, payload) {
  const res = await axios.put(`/api/org/sessions/${sessionId}`, payload);
  return res.data?.data;
}

export async function createSession(themeId, payload) {
  const res = await axios.post(`/api/org/themes/${themeId}/sessions`, payload);
  return res.data?.data;
}

// 別人的==============================================================



export async function getLocations() {
  const res = await axios.get(`/api/locations`);
  return res.data?.data;
}