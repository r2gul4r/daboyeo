const DEFAULT_API_BASE_URL = "http://localhost:8080";

export function getApiBaseUrl() {
  return window.DABOYEO_API_BASE_URL || DEFAULT_API_BASE_URL;
}

export async function fetchHealth() {
  const response = await fetch(`${getApiBaseUrl()}/api/health`, {
    headers: {
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    throw new Error("백엔드 상태 확인 실패");
  }

  return response.json();
}
