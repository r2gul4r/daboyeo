const LOCAL_API_BASE_URL = "http://localhost:8080";

function readApiBaseOverride() {
  if (typeof window !== "undefined" && typeof window.DABOYEO_API_BASE_URL === "string") {
    return window.DABOYEO_API_BASE_URL.trim();
  }

  if (typeof document !== "undefined") {
    const metaValue = document
      .querySelector('meta[name="daboyeo-api-base-url"]')
      ?.getAttribute("content");
    if (typeof metaValue === "string" && metaValue.trim()) {
      return metaValue.trim();
    }
  }

  return "";
}

function isLocalHost(hostname) {
  const normalized = String(hostname || "").toLowerCase();
  return normalized === "localhost" || normalized === "127.0.0.1" || normalized === "::1";
}

function normalizeBaseUrl(value) {
  return String(value || "").replace(/\/+$/, "");
}

function resolveDefaultApiBaseUrl() {
  if (typeof window === "undefined" || !window.location) {
    return LOCAL_API_BASE_URL;
  }

  if (window.location.protocol === "file:") {
    return LOCAL_API_BASE_URL;
  }

  if (isLocalHost(window.location.hostname)) {
    return LOCAL_API_BASE_URL;
  }

  return window.location.origin;
}

export function getApiBaseUrl() {
  const override = readApiBaseOverride();
  return normalizeBaseUrl(override || resolveDefaultApiBaseUrl());
}

async function requestJson(path, options = {}) {
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    let message = "요청을 처리하지 못했어.";

    try {
      const errorBody = await response.json();
      if (typeof errorBody?.message === "string" && errorBody.message.trim()) {
        message = errorBody.message;
      }
    } catch {
      // 응답이 JSON이 아니어도 상태 코드 기준으로 실패 처리한다.
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export async function fetchHealth() {
  return requestJson("/api/health");
}

export async function createRecommendationSession(existingAnonymousId) {
  const body = existingAnonymousId ? { anonymousId: existingAnonymousId } : {};

  return requestJson("/api/recommendation/sessions", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function deleteRecommendationSession(anonymousId) {
  return requestJson(`/api/recommendation/sessions/${encodeURIComponent(anonymousId)}`, {
    method: "DELETE",
  });
}

export async function getPosterSeed(limit = 16) {
  return requestJson(`/api/recommendation/poster-seed?limit=${encodeURIComponent(limit)}`);
}

export async function requestRecommendations(payload) {
  return requestJson("/api/recommendations", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function sendRecommendationFeedback(runId, payload) {
  return requestJson(`/api/recommendations/${encodeURIComponent(runId)}/feedback`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function fetchMovieEvents(filters = {}) {
  const params = new URLSearchParams();

  if (filters.source) {
    params.set("source", filters.source);
  }
  if (filters.category) {
    params.set("category", filters.category);
  }

  const query = params.toString();
  return requestJson(`/api/events${query ? `?${query}` : ""}`);
}
