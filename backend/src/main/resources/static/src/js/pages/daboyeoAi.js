import {
  createRecommendationSession,
  deleteRecommendationSession,
  getRecommendationProviderHealth,
  getPosterSeed,
  requestRecommendations,
  sendRecommendationFeedback,
} from "../api/client.js";

const STORAGE_KEY = "daboyeoAnonymousId";
const AI_PROVIDER_STORAGE_KEY = "daboyeoAiProvider";
const SEARCH_CONTEXT_KEY = "daboyeoSearchContext";
const MAIN_PAGE_URL = "../../index.html";
const SEAT_RECOMMEND_URL = "./seatRecommendMbti.html?flow=ai-result";
const POSTER_LIMIT = 12;
const POSTER_BATCH_SIZE = 6;
const LIKE_LIMIT = 5;
const AUTO_ADVANCE_MS = 320;
const DEFAULT_AI_PROVIDER = "local";

const audienceOptions = [
  { value: "alone", label: "혼자", hint: "혼자서도 몰입할 수 있는 영화로 볼래." },
  { value: "friends", label: "친구", hint: "대화도 되고 텐션도 사는 영화로 볼래" },
  { value: "date", label: "연인", hint: "분위기 살리고 함께 즐길 수 있는 영화로 볼래." },
  { value: "family", label: "가족", hint: "온 가족이 함께 보기 좋은 영화로 볼래." },
  { value: "child", label: "아이와 함께", hint: "아이와 함께 즐겁게 볼 수 있는 영화로 볼래." },
];

const moodOptions = [
  { value: "light", label: "가볍게 즐기기", hint: "부담 없이 편하게 볼 수 있는 작품 위주로." },
  { value: "immersive", label: "깊게 몰입하기", hint: "서사와 여운이 있는 작품 위주로." },
  { value: "exciting", label: "신나게 즐기기", hint: "속도감 있고 재미로 몰아치는 작품 위주로." },
  { value: "calm", label: "잔잔하게 보기", hint: "편안하게 흘러가는 감성적인 작품 위주로." },
  { value: "tense", label: "긴장감 있게 보기", hint: "손에 땀을 쥐게 하는 긴장감 있는 작품 위주로." },
];

const avoidOptions = [
  { value: "too_long", label: "긴 상영시간" },
  { value: "complex", label: "복잡한 이야기" },
  { value: "romance", label: "로맨스 중심 전개" },
  { value: "violence", label: "잔인한 장면" },
  { value: "sad_ending", label: "슬픈 결말" },
  { value: "reality", label: "현실감 없는 설정" },
  { value: "loud", label: "시끄러운 연출" },
  { value: "none", label: "해당 없음" },
];

const modeOptions = [
  {
    value: "fast",
    label: "빠른 추천",
    model: "E2B Q4",
    description: "짧은 이유와 함께, 바로 볼 수 있는\n영화를 빠르게 추천해 드립니다.",
    tags: ["E2B Q4", "빠름", "간단한 이유", "상위 후보"],
  },
  {
    value: "precise",
    label: "정밀 추천",
    model: "E4B Q4",
    description: "후보를 꼼꼼히 비교하고, 포스터 취향까지\n반영해 더 정확하게 추천해 드립니다.",
    tags: ["E4B Q4", "정밀 분석", "포스터 취향", "후보 비교"],
    recommended: true,
  },
];

const providerOptions = [
  {
    value: "local",
    label: "로컬",
    title: "로컬 Gemma",
    description: "학원/작업 컴퓨터의 로컬 OpenAI 호환 서버로 추천을 돌립니다.",
  },
  {
    value: "gpt",
    label: "GPT",
    title: "GPT-5.5",
    description: "GPT-5.5 모델은 고정하고 빠른/정밀 카드별 추론 레벨만 다르게 보냅니다.",
  },
];

const providerModeProfiles = {
  local: {
    fast: {
      model: "Gemma 4 E2B Q4",
      description: "로컬 Gemma E2B로 짧은 이유와 함께\n바로 볼 수 있는 영화를 빠르게 추천해 드립니다.",
      tags: ["Gemma E2B Q4", "로컬", "빠름", "상위 후보"],
    },
    precise: {
      model: "Gemma 4 E4B Q4",
      description: "로컬 Gemma E4B로 후보를 꼼꼼히 비교하고,\n포스터 취향까지 반영해 추천해 드립니다.",
      tags: ["Gemma E4B Q4", "로컬", "정밀 분석", "후보 비교"],
    },
  },
  gpt: {
    fast: {
      model: "GPT-5.5 · reasoning low",
      description: "GPT-5.5는 고정하고 낮은 추론 레벨로\n상영 후보를 빠르게 추려 추천해 드립니다.",
      tags: ["GPT-5.5", "reasoning low", "빠른 판단", "상위 후보"],
    },
    precise: {
      model: "GPT-5.5 · reasoning high",
      description: "GPT-5.5는 고정하고 높은 추론 레벨로\n후보와 취향 근거를 더 꼼꼼하게 비교합니다.",
      tags: ["GPT-5.5", "reasoning high", "정밀 분석", "후보 비교"],
    },
  },
};

const progressText = {
  audience: "1 / 5 상황",
  mood: "2 / 5 컨디션",
  avoid: "3 / 5 상황",
  posters: "4 / 5 포스터",
  mode: "5 / 5 방식",
  loading: "분석 중",
  results: "추천 완료",
  empty: "후보 없음",
  error: "오류",
  sessionError: "연결 필요",
};

const stepBackMap = {
  mood: "audience",
  avoid: "mood",
  posters: "avoid",
  mode: "posters",
};

const timeRangeLabels = {
  morning: "조조",
  brunch: "브런치",
  night: "야간",
};

const screen = document.getElementById("aiScreen");
const backButton = document.getElementById("aiBackButton");
const progress = document.getElementById("aiProgress");
const toast = document.getElementById("aiToast");
const topbarLeft = document.querySelector(".ai-topbar-left");

const state = {
  step: "audience",
  sessionStatus: "pending",
  anonymousId: localStorage.getItem(STORAGE_KEY),
  aiProvider: readStoredAiProvider(),
  survey: {
    audience: null,
    mood: null,
    avoid: [],
  },
  searchContext: null,
  posters: {
    status: "idle",
    items: [],
    error: null,
    activeBatchIndex: 0,
  },
  posterChoices: {
    likedSeedMovieIds: [],
    dislikedSeedMovieIds: [],
  },
  run: {
    status: "idle",
    mode: null,
    aiProvider: null,
    response: null,
    error: null,
  },
  providerHealth: {
    status: "idle",
    items: {},
    error: null,
  },
  feedback: new Map(),
  stepTimer: null,
  toastTimer: null,
};

state.searchContext = readSearchContext();

function readStoredAiProvider() {
  const stored = localStorage.getItem(AI_PROVIDER_STORAGE_KEY);
  return providerOptions.some((option) => option.value === stored) ? stored : DEFAULT_AI_PROVIDER;
}

function activeProvider(value = state.aiProvider) {
  return providerOptions.find((option) => option.value === value) || providerOptions[0];
}

function modeOption(value) {
  return modeOptions.find((option) => option.value === value) || modeOptions[0];
}

function modeProfile(modeValue, providerValue = state.aiProvider) {
  const option = modeOption(modeValue);
  const providerProfiles = providerModeProfiles[providerValue] || providerModeProfiles[DEFAULT_AI_PROVIDER];
  return {
    ...option,
    ...(providerProfiles?.[option.value] || {}),
  };
}

function setAiProvider(value) {
  if (!providerOptions.some((option) => option.value === value)) {
    return;
  }

  state.aiProvider = value;
  localStorage.setItem(AI_PROVIDER_STORAGE_KEY, value);
  render();
}

function normalizeProviderHealth(items) {
  if (!Array.isArray(items)) {
    return {};
  }

  return items.reduce((acc, item) => {
    const provider = typeof item?.provider === "string" ? item.provider.trim().toLowerCase() : "";
    if (provider) {
      acc[provider] = item;
    }
    return acc;
  }, {});
}

function providerHealthFor(providerValue) {
  return state.providerHealth.items[providerValue] || null;
}

function providerHealthMeta(providerValue) {
  const health = providerHealthFor(providerValue);
  if (state.providerHealth.status === "loading" || state.providerHealth.status === "idle") {
    return { key: "checking", label: "확인 중", note: "모델 서버 연결 상태를 확인하고 있어." };
  }
  if (state.providerHealth.status === "error") {
    return { key: "unknown", label: "확인 실패", note: "상태 확인은 실패했지만 추천 요청은 계속 보낼 수 있어." };
  }
  if (health?.available) {
    return { key: "ready", label: "연결됨", note: health.message || "모델 서버가 응답 중이야." };
  }
  return { key: "offline", label: "오프라인", note: health?.message || "모델 서버 연결이 없어 fallback 추천으로 이어질 수 있어." };
}

async function ensureProviderHealthLoaded(force = false) {
  if (!force && ["loading", "ready"].includes(state.providerHealth.status)) {
    return;
  }

  state.providerHealth = { ...state.providerHealth, status: "loading", error: null };
  if (state.step === "mode") {
    render();
  }

  try {
    const items = await getRecommendationProviderHealth();
    state.providerHealth = {
      status: "ready",
      items: normalizeProviderHealth(items),
      error: null,
    };
  } catch (error) {
    state.providerHealth = {
      status: "error",
      items: {},
      error,
    };
  }

  if (state.step === "mode") {
    render();
  }
}

function goToMainPage() {
  window.location.href = MAIN_PAGE_URL;
}

function goToPreviousPage() {
  if (window.history.length > 1) {
    window.history.back();
    return;
  }

  goToMainPage();
}

function resetBackButtonToTopbar() {
  backButton.hidden = false;
  backButton.style.position = "";
  backButton.style.top = "";
  backButton.style.left = "";
  backButton.style.marginBottom = "";

  if (topbarLeft && backButton.parentElement !== topbarLeft) {
    topbarLeft.insertBefore(backButton, topbarLeft.firstChild);
  }
}

function moveBackButtonToStepPane(leftPane) {
  backButton.hidden = false;
  backButton.style.position = "absolute";
  backButton.style.top = "-90px";
  backButton.style.left = "0";
  backButton.style.marginBottom = "0";
  leftPane.appendChild(backButton);
}

function shuffleItems(items) {
  const copy = [...items];

  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    const current = copy[i];
    copy[i] = copy[j];
    copy[j] = current;
  }

  return copy;
}

function createElement(tagName, className, text) {
  const element = document.createElement(tagName);

  if (Array.isArray(className)) {
    element.classList.add(...className.filter(Boolean));
  } else if (className) {
    element.className = className;
  }

  if (text !== undefined && text !== null) {
    element.textContent = text;
  }

  return element;
}

function clearChildren(element) {
  while (element.firstChild) {
    element.removeChild(element.firstChild);
  }
}

function normalizeSearchContext(raw) {
  if (!raw || typeof raw !== "object") {
    return null;
  }

  const personCount = Number(raw.personCount);
  const normalized = {
    region: typeof raw.region === "string" ? raw.region.trim() : "",
    date: typeof raw.date === "string" ? raw.date.trim() : "",
    timeRange: typeof raw.timeRange === "string" ? raw.timeRange.trim().toLowerCase() : "",
    personCount: Number.isFinite(personCount) && personCount > 0 ? Math.floor(personCount) : null,
  };

  return hasSearchContext(normalized) ? normalized : null;
}

function hasSearchContext(context) {
  return Boolean(
    context
      && (context.region
        || context.date
        || context.timeRange
        || context.personCount),
  );
}

function readSearchContext() {
  try {
    const raw = sessionStorage.getItem(SEARCH_CONTEXT_KEY);
    return raw ? normalizeSearchContext(JSON.parse(raw)) : null;
  } catch {
    return null;
  }
}

function searchContextRegion(context) {
  const region = context?.region?.trim() || "";
  return region && region !== "전체" ? region : "";
}

function searchContextParts(context) {
  if (!hasSearchContext(context)) {
    return [];
  }

  const parts = [];
  const region = searchContextRegion(context);
  if (region) {
    parts.push(region);
  }
  if (context.date) {
    parts.push(context.date);
  }
  if (context.timeRange && timeRangeLabels[context.timeRange]) {
    parts.push(timeRangeLabels[context.timeRange]);
  }
  if (context.personCount) {
    parts.push(`${context.personCount}명`);
  }
  return parts;
}

function searchContextText(context) {
  const parts = searchContextParts(context);
  return parts.length > 0 ? parts.join(" · ") : "";
}

function buildSearchFiltersPayload() {
  if (!hasSearchContext(state.searchContext)) {
    return null;
  }

  return {
    region: searchContextRegion(state.searchContext),
    date: state.searchContext.date || null,
    timeRange: state.searchContext.timeRange || null,
    personCount: state.searchContext.personCount || null,
  };
}

function optionLabel(options, value) {
  return options.find((option) => option.value === value)?.label || "아직 선택 안 함";
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("is-visible");

  if (state.toastTimer) {
    window.clearTimeout(state.toastTimer);
  }

  state.toastTimer = window.setTimeout(() => {
    toast.classList.remove("is-visible");
  }, 2400);
}

function scheduleStep(nextStep, delay = AUTO_ADVANCE_MS) {
  if (state.stepTimer) {
    window.clearTimeout(state.stepTimer);
  }

  state.stepTimer = window.setTimeout(() => {
    setStep(nextStep);
  }, delay);
}

function clearStateFromStep(stepToClear) {
  const steps = ["audience", "mood", "avoid", "posters", "mode"];
  const startIndex = steps.indexOf(stepToClear);
  if (startIndex === -1) return;

  for (let i = startIndex; i < steps.length; i++) {
    switch (steps[i]) {
      case "audience":
        state.survey.audience = null;
        break;
      case "mood":
        state.survey.mood = null;
        break;
      case "avoid":
        state.survey.avoid = [];
        break;
      case "posters":
        state.posterChoices.likedSeedMovieIds = [];
        break;
    }
  }
}

function setStep(nextStep) {
  if (state.stepTimer) {
    window.clearTimeout(state.stepTimer);
    state.stepTimer = null;
  }

  const performStepChange = () => {
    state.step = nextStep;
    if (nextStep === "posters") {
      ensurePostersLoaded();
    }
    if (nextStep === "mode") {
      ensureProviderHealthLoaded();
    }
    render();
    screen.classList.add("is-entering");
    setTimeout(() => screen.classList.remove("is-entering"), 400);
    screen.focus({ preventScroll: true });
  };

  if (state.step && screen.firstChild) {
    screen.classList.add("is-exiting");
    setTimeout(() => {
      screen.classList.remove("is-exiting");
      performStepChange();
    }, 350);
  } else {
    performStepChange();
  }
}

function resetInputs(keepSession = true) {
  state.step = "audience";
  state.survey = { audience: null, mood: null, avoid: [] };
  state.posterChoices = { likedSeedMovieIds: [], dislikedSeedMovieIds: [] };
  state.posters.activeBatchIndex = 0;
  state.run = { status: "idle", mode: null, aiProvider: null, response: null, error: null };
  state.feedback = new Map();

  if (!keepSession) {
    state.anonymousId = null;
    state.sessionStatus = "pending";
  }
}

async function ensureSession() {
  state.sessionStatus = "pending";

  try {
    const session = await createRecommendationSession(state.anonymousId);
    if (!session?.anonymousId) {
      throw new Error("익명 세션을 만들지 못했어.");
    }

    state.anonymousId = session.anonymousId;
    localStorage.setItem(STORAGE_KEY, session.anonymousId);
    state.sessionStatus = "ready";

    if (state.step === "sessionError") {
      state.step = "audience";
    }
  } catch (error) {
    state.sessionStatus = "error";
    state.anonymousId = null;
    state.run.error = error;
    state.step = "sessionError";
    showToast(error.message || "백엔드 세션 연결에 실패했어.");
  }

  render();
}

async function ensurePostersLoaded(force = false) {
  if (!force && ["loading", "ready"].includes(state.posters.status)) {
    return;
  }

  state.posters = { ...state.posters, status: "loading", error: null };
  render();

  try {
    const posters = await getPosterSeed(POSTER_LIMIT);
    const items = Array.isArray(posters) ? posters : [];
    state.posters = {
      status: items.length > 0 ? "ready" : "empty",
      items,
      error: null,
      activeBatchIndex: 0,
    };
  } catch (error) {
    state.posters = {
      status: "error",
      items: [],
      error,
      activeBatchIndex: 0,
    };
    showToast(error.message || "포스터 API 연결에 실패했어.");
  }

  render();
}

function renderTitle(parts) {
  const title = createElement("h2", "ai-title");

  parts.forEach((part) => {
    const node = createElement("span", part.highlight ? "ai-highlight" : null);
    if (part.style) {
      node.style.cssText = part.style;
    }

    const textParts = part.text.split('\n');
    textParts.forEach((textPart, index) => {
      if (textPart) {
        node.appendChild(document.createTextNode(textPart));
      }
      if (index < textParts.length - 1) {
        node.appendChild(document.createElement('br'));
      }
    });

    title.appendChild(node);
  });

  return title;
}

function renderSplitLayout({ kicker, titleParts, description, extraLeft, content }) {
  const section = createElement("section", "ai-split-layout");

  const leftPane = createElement("div", "ai-split-left");

  if (stepBackMap[state.step]) {
    moveBackButtonToStepPane(leftPane);
  } else {
    resetBackButtonToTopbar();
  }

  leftPane.appendChild(renderTitle(titleParts));

  if (description) {
    leftPane.appendChild(createElement("p", "ai-description", description));
  }
  if (extraLeft) {
    leftPane.appendChild(extraLeft);
  }

  const summary = renderSummary();
  if (summary) {
    leftPane.appendChild(summary);
  }

  const rightPane = createElement("div", "ai-split-right");
  rightPane.appendChild(content);

  section.appendChild(leftPane);
  section.appendChild(rightPane);

  return section;
}

function renderSummary() {
  const rows = [];
  const searchText = searchContextText(state.searchContext);

  if (searchText) {
    rows.push(["상영 조건", searchText]);
  }

  if (state.survey.audience) {
    rows.push(["함께 볼 사람", optionLabel(audienceOptions, state.survey.audience)]);
  }
  if (state.survey.mood) {
    rows.push(["오늘 컨디션", optionLabel(moodOptions, state.survey.mood)]);
  }
  if (state.survey.avoid.length > 0) {
    const avoidLabel = state.survey.avoid.map((value) => optionLabel(avoidOptions, value)).join(", ");
    rows.push(["보고 싶지 않은 요소", avoidLabel]);
  }
  if (state.posterChoices.likedSeedMovieIds.length) {
    rows.push(["포스터 취향", `${state.posterChoices.likedSeedMovieIds.length} / ${LIKE_LIMIT}`]);
  }

  if (rows.length === 0) return null;

  const summary = createElement("div", "ai-summary");
  rows.forEach(([label, value]) => {
    const row = createElement("div", "ai-summary-row");
    row.appendChild(createElement("strong", null, label));
    row.appendChild(document.createTextNode(` ${value}`));
    summary.appendChild(row);
  });
  return summary;
}

function renderOptionList(options, selectedValueOrArray, onSelect, isMulti = false) {
  const list = createElement("div", "ai-option-list");
  list.style.display = 'flex';
  list.style.flexDirection = 'column';
  list.style.alignItems = 'flex-end';
  list.style.gap = '14px';
  list.style.width = '100%';

  options.forEach((option) => {
    const isSelected = isMulti
      ? selectedValueOrArray.includes(option.value)
      : selectedValueOrArray === option.value;

    const button = createElement("button", ["ai-glass-btn", "can-hover", isSelected ? "is-selected" : null]);
    button.type = "button";

    button.addEventListener("click", () => {
      onSelect(option.value);
    });

    button.appendChild(createElement("span", "ai-glass-btn-title", option.label));
    list.appendChild(button);
  });

  return list;
}

function renderAudienceStep() {
  const content = renderOptionList(audienceOptions, state.survey.audience, (value) => {
    state.survey.audience = value;
    render();
    scheduleStep("mood");
  });

  return renderSplitLayout({
    kicker: "AI GUIDE 01",
    titleParts: [
      { text: "반가워요, 다보예요!\n", style: "font-size: 40px;" },
      { text: "누구랑 보실 건가요?" },
    ],
    description: "같이 볼 사람부터 골라볼까요? 지금 상황을 기준으로 추천 기준을 잡을게요.",
    content,
  });
}

function renderMoodStep() {
  const content = renderOptionList(moodOptions, state.survey.mood, (value) => {
    state.survey.mood = value;
    render();
    scheduleStep("avoid");
  });

  return renderSplitLayout({
    kicker: "AI GUIDE 02",
    titleParts: [
      { text: "오늘 하루,\n컨디션은 어때요?" },
    ],
    description: "지금 보고 싶은 감정의 온도를 고르면 추천 방향이 바로 좁혀져요.",
    content,
  });
}

function toggleAvoid(value) {
  const exists = state.survey.avoid.includes(value);
  state.survey.avoid = exists
    ? state.survey.avoid.filter((item) => item !== value)
    : [...state.survey.avoid, value];
  render();
}

function posterBatchCount() {
  return Math.max(1, Math.ceil(state.posters.items.length / POSTER_BATCH_SIZE));
}

function currentPosterBatch() {
  const batchIndex = Math.min(state.posters.activeBatchIndex, posterBatchCount() - 1);
  const start = batchIndex * POSTER_BATCH_SIZE;

  return state.posters.items.slice(start, start + POSTER_BATCH_SIZE);
}

function posterChoiceStatus(seedId) {
  const id = String(seedId);

  if (state.posterChoices.likedSeedMovieIds.includes(id)) {
    return "like";
  }

  return null;
}

function isPosterBatchComplete(batch = currentPosterBatch()) {
  return batch.length > 0 && batch.some((movie) => posterChoiceStatus(movie.id));
}

function canContinuePosterDiagnosis() {
  return state.posterChoices.likedSeedMovieIds.length === LIKE_LIMIT;
}

function showPosterBatch(batchIndex) {
  if (state.stepTimer) {
    window.clearTimeout(state.stepTimer);
    state.stepTimer = null;
  }

  state.posters.activeBatchIndex = Math.min(Math.max(batchIndex, 0), posterBatchCount() - 1);
  render();
}

function renderAvoidStep() {
  const panel = createElement("div", "ai-avoid-panel");
  panel.style.width = '100%';

  const grid = createElement("div", "ai-avoid-grid");

  avoidOptions.forEach((option) => {
    const isSelected = state.survey.avoid.includes(option.value);
    const btn = createElement("button", ["ai-avoid-btn", isSelected ? "is-selected" : null], option.label);
    btn.type = "button";
    btn.addEventListener("click", () => {
      if (option.value === "none") {
        state.survey.avoid = ["none"];
        render();
        setStep("posters");
      } else {
        state.survey.avoid = state.survey.avoid.filter(v => v !== "none");
        toggleAvoid(option.value);
      }
    });
    grid.appendChild(btn);
  });

  panel.appendChild(grid);

  const ctaRow = createElement("div", "ai-cta-row");
  const completeBtn = createElement("button", "ai-primary-cta", "선택 완료");
  completeBtn.type = "button";
  completeBtn.addEventListener("click", () => {
    setStep("posters");
  });
  ctaRow.appendChild(completeBtn);

  const hint = createElement("p", "ai-avoid-cta-hint", "여러 개 선택하실 수 있습니다. 해당 없음을 선택하시면 바로 다음 단계로 넘어갑니다.");
  ctaRow.appendChild(hint);

  panel.appendChild(ctaRow);

  return renderSplitLayout({
    kicker: "AI GUIDE 03",
    titleParts: [
      { text: "보고 싶지 않은\n요소가 있으신가요?" }
    ],
    description: "선택하신 요소는 추천에 최대한 반영해 걸러드립니다.\n아이와 함께라면 더 안전한 기준으로 추천해 드릴게요.",
    content: panel,
  });
}

function selectPoster(seedId) {
  if (state.stepTimer) {
    window.clearTimeout(state.stepTimer);
    state.stepTimer = null;
  }

  const id = String(seedId);
  const liked = state.posterChoices.likedSeedMovieIds;

  if (liked.includes(id)) {
    state.posterChoices.likedSeedMovieIds = liked.filter((item) => item !== id);
    render();
    return;
  }

  if (liked.length >= LIKE_LIMIT) {
    showToast(`끌리는 포스터는 ${LIKE_LIMIT}개까지야.`);
    return;
  }

  state.posterChoices.likedSeedMovieIds = [...liked, id];

  render();
}

function renderPosterCount(className, label, value, target) {
  const pill = createElement("div", ["ai-count-pill", className]);
  pill.appendChild(createElement("strong", null, label));
  pill.appendChild(createElement("span", null, `${value} / ${target}`));
  return pill;
}

function renderPosterRounds() {
  const roundRow = createElement("div", "ai-poster-rounds");
  const totalBatches = posterBatchCount();

  for (let index = 0; index < totalBatches; index += 1) {
    const start = index * POSTER_BATCH_SIZE;
    const batch = state.posters.items.slice(start, start + POSTER_BATCH_SIZE);
    const button = createElement("button", [
      "ai-round-button",
      index === state.posters.activeBatchIndex ? "is-active" : null,
      isPosterBatchComplete(batch) ? "is-complete" : null,
    ], `${index + 1}라운드`);
    button.type = "button";
    button.addEventListener("click", () => showPosterBatch(index));
    roundRow.appendChild(button);
  }

  return roundRow;
}

function renderPosterCard(movie) {
  const id = String(movie.id);
  const isLiked = state.posterChoices.likedSeedMovieIds.includes(id);
  const card = createElement("button", ["ai-poster-card", isLiked ? "is-selected" : null]);
  card.type = "button";
  card.addEventListener("click", () => selectPoster(id));

  const title = movie.title || "이름 없는 포스터";
  const fallback = createElement("div", "ai-poster-image-fallback");
  fallback.appendChild(createElement("span", null, title));
  fallback.appendChild(createElement("small", null, "포스터 준비 중"));
  card.appendChild(fallback);

  if (movie.posterUrl) {
    const image = createElement("img");
    image.alt = `${title} 포스터`;
    image.loading = "lazy";
    image.src = movie.posterUrl;
    image.addEventListener("load", () => card.classList.add("has-image"));
    image.addEventListener("error", () => {
      image.remove();
      card.classList.add("is-image-missing");
    });
    card.appendChild(image);
  } else {
    card.classList.add("is-image-missing");
  }

  const overlay = createElement("div", "ai-poster-overlay");
  overlay.appendChild(createElement("span", null, title));
  card.appendChild(overlay);

  return card;
}

function renderPosterStep() {
  if (state.posters.status === "loading" || state.posters.status === "idle") {
    return renderLoadingMessage("포스터를 가져오는 중", "잠깐만 기다려줘.");
  }
  if (state.posters.status === "error") {
    return renderErrorPanel("포스터를 가져오지 못했어", state.posters.error?.message,
      [{ label: "다시 시도", onClick: () => ensurePostersLoaded(true) }, { label: "처음부터", onClick: () => setStep("audience"), secondary: true }]);
  }
  if (state.posters.status === "empty") {
    return renderErrorPanel("포스터 seed가 비어 있어", "서버에 포스터 진단용 데이터가 아직 없어.",
      [{ label: "다시 시도", onClick: () => ensurePostersLoaded(true) }]);
  }

  const activeBatch = currentPosterBatch();
  const grid = createElement("div", "ai-poster-grid");
  activeBatch.forEach((movie) => {
    grid.appendChild(renderPosterCard(movie));
  });

  const posterStage = createElement("div", "ai-poster-stage");
  posterStage.appendChild(grid);

  const prevBatchBtn = createElement("button", ["ai-batch-button", "ai-poster-nav-button", "ai-poster-prev-button"], "<");
  prevBatchBtn.type = "button";
  prevBatchBtn.setAttribute("aria-label", "Previous poster batch");
  prevBatchBtn.addEventListener("click", () => {
    const totalBatches = posterBatchCount();
    const previousIndex = (state.posters.activeBatchIndex - 1 + totalBatches) % totalBatches;
    showPosterBatch(previousIndex);
  });
  posterStage.appendChild(prevBatchBtn);

  const nextBatchBtn = createElement("button", ["ai-batch-button", "ai-poster-nav-button", "ai-poster-next-button"], ">");
  nextBatchBtn.type = "button";
  nextBatchBtn.setAttribute("aria-label", "Next poster batch");
  nextBatchBtn.addEventListener("click", () => {
    const nextIndex = (state.posters.activeBatchIndex + 1) % posterBatchCount();
    showPosterBatch(nextIndex);
  });
  posterStage.appendChild(nextBatchBtn);

  const ctaRow = createElement("div", ["ai-cta-row", "ai-poster-cta-row"]);
  const isSatisfied = canContinuePosterDiagnosis();
  const completeBtn = createElement("button", ["ai-primary-cta", isSatisfied ? "is-active" : null], "선택 완료");
  completeBtn.type = "button";
  completeBtn.disabled = !isSatisfied;
  completeBtn.setAttribute("aria-disabled", String(!isSatisfied));
  completeBtn.addEventListener("click", () => {
    if (isSatisfied) {
      setStep("mode");
    } else {
      showToast(`포스터 ${LIKE_LIMIT}개를 모두 선택해주세요.`);
    }
  });
  ctaRow.appendChild(completeBtn);

  const content = createElement("div", "ai-poster-pane");
  content.style.position = "relative";
  content.style.width = "100%";

  const fakeKicker = createElement("p", "ai-kicker", "AI GUIDE 04");
  fakeKicker.style.visibility = "hidden";
  fakeKicker.setAttribute("aria-hidden", "true");

  content.appendChild(fakeKicker);
  content.appendChild(posterStage);
  content.appendChild(ctaRow);

  const extraLeft = createElement("div", "ai-poster-count-left");
  const span = createElement("span", null, "선택");
  extraLeft.appendChild(span);
  extraLeft.appendChild(document.createTextNode(` ${state.posterChoices.likedSeedMovieIds.length} / ${LIKE_LIMIT}`));

  return renderSplitLayout({
    kicker: "AI GUIDE 04",
    titleParts: [{ text: "영화 포스터만 보고,\n끌리는 작품을 선택해 주세요.", style: "font-size: 30px;" }],
    description: null,
    extraLeft,
    content,
  });
}

function renderModeCard(option) {
  const profile = modeProfile(option.value);
  const card = createElement("div", "ai-mode-card");

  if (option.recommended) {
    const tag = createElement("div", "ai-mode-recommend-tag", "추천");
    card.appendChild(tag);
  }

  const top = createElement("div");
  top.appendChild(createElement("h3", null, option.label));
  top.appendChild(createElement("div", "ai-mode-model", profile.model));

  top.appendChild(createElement("div", "ai-mode-divider"));

  const desc = createElement("p", null, profile.description);
  desc.innerHTML = profile.description.replace(/\n/g, '<br/>');
  top.appendChild(desc);

  const badges = createElement("div", "ai-mode-badges");
  profile.tags.forEach(t => {
    badges.appendChild(createElement("span", "ai-mode-badge", t));
  });
  top.appendChild(badges);

  card.appendChild(top);

  const btn = createElement("button", "ai-mode-action", "보러가기");
  btn.type = "button";
  btn.addEventListener("click", () => runRecommendation(option.value));
  card.appendChild(btn);

  return card;
}

function renderProviderSwitch() {
  const panel = createElement("section", "ai-provider-panel");
  panel.setAttribute("aria-label", "AI 추천 엔진 선택");

  const header = createElement("div", "ai-provider-header");
  header.appendChild(createElement("span", "ai-provider-kicker", "AI ROUTE"));
  header.appendChild(createElement("strong", null, "추천 엔진 선택"));
  panel.appendChild(header);

  const switcher = createElement("div", "ai-provider-switch");
  providerOptions.forEach((option) => {
    const button = createElement("button", "ai-provider-option");
    button.type = "button";
    button.classList.toggle("is-selected", state.aiProvider === option.value);
    const healthMeta = providerHealthMeta(option.value);
    button.classList.add(`is-${healthMeta.key}`);
    button.setAttribute("aria-pressed", String(state.aiProvider === option.value));

    const label = createElement("span", null, option.label);
    const title = createElement("strong", null, option.title);
    const copy = createElement("small", null, option.description);
    const status = createElement("em", "ai-provider-status", healthMeta.label);
    button.appendChild(label);
    button.appendChild(title);
    button.appendChild(status);
    button.appendChild(copy);
    button.addEventListener("click", () => setAiProvider(option.value));
    switcher.appendChild(button);
  });
  panel.appendChild(switcher);

  const active = activeProvider();
  const activeHealth = providerHealthMeta(active.value);
  panel.appendChild(createElement("p", "ai-provider-note", `${active.title} 라우팅 · ${activeHealth.note}`));

  return panel;
}

function renderModeStep() {
  const content = createElement("div", "ai-mode-panel");
  const grid = createElement("div", "ai-mode-grid");
  modeOptions.forEach(opt => {
    grid.appendChild(renderModeCard(opt));
  });
  content.appendChild(renderProviderSwitch());
  content.appendChild(grid);

  return renderSplitLayout({
    kicker: "AI GUIDE 05",
    titleParts: [{ text: "어떤 방식으로\n추천해 드릴까요?" }],
    description: "로컬과 GPT 중 하나를 고르면, 빠른/정밀 추천 카드의 모델 라우팅도 함께 바뀝니다.",
    content,
  });
}

async function runRecommendation(mode) {
  if (state.sessionStatus !== "ready" || !state.anonymousId) {
    showToast("익명 세션을 먼저 확인할게.");
    await ensureSession();
  }

  if (!state.anonymousId) {
    return;
  }

  const aiProvider = state.aiProvider || DEFAULT_AI_PROVIDER;
  state.run = { status: "loading", mode, aiProvider, response: null, error: null };
  setStep("loading");

  const payload = {
    anonymousId: state.anonymousId,
    mode,
    aiProvider,
    survey: {
      audience: state.survey.audience,
      mood: state.survey.mood,
      avoid: state.survey.avoid,
    },
    posterChoices: {
      likedSeedMovieIds: state.posterChoices.likedSeedMovieIds,
      dislikedSeedMovieIds: [],
    },
  };
  const searchFilters = buildSearchFiltersPayload();
  if (searchFilters) {
    payload.searchFilters = searchFilters;
  }

  try {
    const response = await requestRecommendations(payload);
    state.run = { status: "success", mode, aiProvider, response, error: null };

    const recommendations = Array.isArray(response?.recommendations) ? response.recommendations : [];
    setStep(response?.status === "no_candidates" || recommendations.length === 0 ? "empty" : "results");
  } catch (error) {
    state.run = { status: "error", mode, aiProvider, response: null, error };
    setStep("error");
  }
}

function renderLoadingMessage(title, description) {
  const card = createElement("section", "ai-loading-card");
  card.appendChild(createElement("div", "ai-loader"));
  card.appendChild(createElement("h2", null, title));
  card.appendChild(createElement("p", null, description));
  return card;
}

function renderLoadingStep() {
  const mode = modeProfile(state.run.mode, state.run.aiProvider || state.aiProvider);
  const title = mode ? `${mode.label} 분석 중` : "분석 중";
  const description = mode
    ? `${mode.model} 모델에 상위 후보만 보내고 있어. 결과가 없으면 가짜 추천 없이 알려줄게.`
    : "추천 후보를 계산하고 있어.";

  return renderLoadingMessage(title, description);
}

function renderErrorPanel(title, description, actions = []) {
  const panel = createElement("section", "ai-error-panel");
  panel.appendChild(createElement("h2", null, title));
  panel.appendChild(createElement("p", null, description));

  if (actions.length > 0) {
    const actionRow = createElement("div", "ai-panel-actions");
    actions.forEach((action) => {
      const button = createElement("button", action.secondary ? "ai-secondary-button" : "ai-complete-button", action.label);
      button.type = "button";
      button.addEventListener("click", action.onClick);
      actionRow.appendChild(button);
    });
    panel.appendChild(actionRow);
  }

  return panel;
}

function renderEmptyStep() {
  const message = state.run.response?.message || "현재 상영 DB에서 추천할 후보를 찾지 못했어. 수집 데이터가 들어오면 바로 추천할 수 있어.";

  return renderErrorPanel(
    "추천 후보가 아직 없어",
    message,
    [
      { label: "다시 추천", onClick: () => setStep("mode") },
      { label: "처음부터", onClick: () => setStep("audience"), secondary: true },
      { label: "메인으로", onClick: goToMainPage, secondary: true },
    ],
  );
}

function renderRecommendationErrorStep() {
  return renderErrorPanel(
    "추천 요청이 실패했어",
    state.run.error?.message || "백엔드 API 또는 선택한 AI 라우팅 연결을 확인해줘.",
    [
      { label: "다시 시도", onClick: () => runRecommendation(state.run.mode || "fast") },
      { label: "방식 다시 고르기", onClick: () => setStep("mode"), secondary: true },
      { label: "메인으로", onClick: goToMainPage, secondary: true },
    ],
  );
}

function renderSessionErrorStep() {
  return renderErrorPanel(
    "익명 세션을 만들지 못했어",
    state.run.error?.message || "Spring API가 localhost:8080에서 실행 중인지 확인해줘.",
    [
      { label: "다시 연결", onClick: ensureSession },
      { label: "홈으로", onClick: goToMainPage, secondary: true },
    ],
  );
}

function safeUrl(value) {
  if (!value || typeof value !== "string") {
    return null;
  }

  try {
    const url = new URL(value, window.location.href);
    return ["http:", "https:"].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}

function formatPrice(amount, currencyCode) {
  if (amount === null || amount === undefined || amount === "") {
    return "가격 정보 없음";
  }

  const numberAmount = Number(amount);
  if (Number.isNaN(numberAmount)) {
    return String(amount);
  }

  return new Intl.NumberFormat("ko-KR", {
    style: "currency",
    currency: currencyCode || "KRW",
    maximumFractionDigits: 0,
  }).format(numberAmount);
}

function formatShowtime(item) {
  const parts = [item.showDate, item.startsAt].filter(Boolean);
  return parts.length > 0 ? parts.join(" ") : "시간 정보 없음";
}

function feedbackKey(item) {
  return `${item.movieId || "movie"}:${item.showtimeId || "showtime"}`;
}

async function sendFeedback(item, action, { quiet = false } = {}) {
  if (!state.run.response?.runId || !state.anonymousId) {
    if (!quiet) {
      showToast("추천 실행 정보가 없어 피드백을 저장하지 못했어.");
    }
    return;
  }

  try {
    await sendRecommendationFeedback(state.run.response.runId, {
      anonymousId: state.anonymousId,
      movieId: item.movieId,
      showtimeId: item.showtimeId,
      action,
    });

    if (action === "like" || action === "dislike") {
      state.feedback.set(feedbackKey(item), action);
      render();
    }

    if (!quiet) {
      showToast("다음 추천에 반영할게.");
    }
  } catch (error) {
    if (!quiet) {
      showToast(error.message || "피드백 저장에 실패했어.");
    }
  }
}

function openBooking(item) {
  const bookingUrl = safeUrl(item.bookingUrl);

  if (!bookingUrl) {
    showToast("예매 링크가 아직 없어.");
    return;
  }

  sendFeedback(item, "booking_view", { quiet: true });
  window.open(bookingUrl, "_blank", "noopener,noreferrer");
}

function openSeatMap() {
  window.location.href = SEAT_RECOMMEND_URL;
}

function renderShowtimeItem(label, value) {
  const item = createElement("div", "ai-showtime-item");
  item.appendChild(createElement("strong", null, label));
  item.appendChild(createElement("span", null, value || "정보 없음"));
  return item;
}

function renderResultCard(item, index, context = {}) {
  const card = createElement("article", "ai-result-card");
  const isAiBacked = context.isAiBacked !== false;
  const isGptResult = isAiBacked && context.providerValue === "gpt";
  const isFallbackResult = !isAiBacked;
  const isPreciseResult = context.mode === "precise";
  card.classList.toggle("is-gpt-result", isGptResult);
  card.classList.toggle("is-fallback-result", isFallbackResult);
  card.classList.toggle("is-precise-result", isPreciseResult);
  const inner = createElement("div", "ai-result-card-inner");
  const poster = createElement("div", "ai-result-poster");
  const imageUrl = safeUrl(item.posterUrl);

  if (imageUrl) {
    const image = createElement("img");
    image.src = imageUrl;
    image.alt = `${item.title || "영화"} 포스터`;
    image.loading = "lazy";
    poster.appendChild(image);
  } else {
    poster.appendChild(createElement("div", "ai-poster-fallback", item.title || "포스터 없음"));
  }

  poster.appendChild(createElement("div", "ai-rank", String(index + 1)));

  const body = createElement("div", "ai-result-body");
  const heading = createElement("div", "ai-result-heading");
  heading.appendChild(createElement("h2", "ai-movie-title", item.title || "제목 미상"));
  heading.appendChild(createElement("div", "ai-score", `${Math.round(Number(item.score) || 0)}점`));
  body.appendChild(heading);

  if (item.reason) {
    body.appendChild(createElement("p", isGptResult ? "ai-result-reason" : "ai-result-tags", item.reason));
  }

  if (item.analysisPoint) {
    const analysisPoint = createElement("p", isGptResult ? "ai-result-analysis" : "ai-result-tags");
    const labelText = isGptResult ? "GPT 분석" : isFallbackResult ? "Fallback 근거" : "분석 포인트";
    const label = createElement("strong", "ai-analysis-label", labelText);
    analysisPoint.appendChild(label);
    analysisPoint.appendChild(document.createTextNode(` ${item.analysisPoint}`));
    body.appendChild(analysisPoint);
  }

  if (item.caution) {
    const caution = createElement("p", "ai-result-caution");
    caution.appendChild(createElement("strong", null, "주의 포인트"));
    caution.appendChild(document.createTextNode(` ${item.caution}`));
    body.appendChild(caution);
  }

  if (item.valuePoint) {
    body.appendChild(createElement("p", ["ai-result-tags", "ai-result-tags-white"], item.valuePoint));
  }

  const showtimeGrid = createElement("div", "ai-showtime-grid");
  showtimeGrid.appendChild(renderShowtimeItem("극장", [item.providerCode, item.theaterName].filter(Boolean).join(" ")));
  showtimeGrid.appendChild(renderShowtimeItem("지역/상영관", [item.regionName, item.screenName].filter(Boolean).join(" ")));
  showtimeGrid.appendChild(renderShowtimeItem("상영 시간", formatShowtime(item)));
  showtimeGrid.appendChild(renderShowtimeItem("최저가", formatPrice(item.minPriceAmount, item.currencyCode)));
  body.appendChild(showtimeGrid);

  const actions = createElement("div", "ai-result-actions");
  const bookingButton = createElement("button", "ai-booking-button", "예매하기");
  bookingButton.type = "button";
  bookingButton.addEventListener("click", () => openBooking(item));
  actions.appendChild(bookingButton);

  const seatMapButton = createElement("button", "ai-seatmap-button", "좌석표보기");
  seatMapButton.type = "button";
  seatMapButton.addEventListener("click", openSeatMap);
  actions.appendChild(seatMapButton);

  body.appendChild(actions);
  inner.appendChild(poster);
  inner.appendChild(body);
  card.appendChild(inner);

  return card;
}

function renderResultsStep() {
  const response = state.run.response || {};
  const recommendations = Array.isArray(response.recommendations) ? response.recommendations : [];
  const modeLabel = optionLabel(modeOptions, response.mode || state.run.mode);
  const providerValue = state.run.aiProvider || state.aiProvider;
  const provider = activeProvider(providerValue);
  const model = response.model || modeProfile(response.mode || state.run.mode, providerValue).model;
  const isAiBacked = response.status === "ok";
  const resultSource = isAiBacked
    ? `${provider.title} · ${modeLabel}`
    : "코드 점수 기반 fallback";
  const layout = createElement("section", "ai-result-layout");
  const side = createElement("aside", "ai-result-side");

  side.appendChild(createElement("p", "ai-kicker", "AI RESULT"));
  side.appendChild(renderTitle([
    { text: "다보의\n영화 추천은!" },
  ]));
  const resultNote = response.message || "지금 상황과 취향을 반영해, 바로 볼 수 있는 최적의 영화를 골랐어요.";
  side.appendChild(createElement("p", "ai-result-note", resultNote));

  const summary = createElement("div", "ai-result-summary");

  const addSummaryRow = (label, value) => {
    const row = createElement("div", "ai-result-summary-row");
    row.appendChild(createElement("strong", null, label));
    row.appendChild(createElement("span", null, value));
    summary.appendChild(row);
  };

  const searchText = searchContextText(state.searchContext);
  if (searchText) {
    addSummaryRow("상영 조건", searchText);
  }
  if (state.survey.audience) {
    addSummaryRow("함께 볼 사람", optionLabel(audienceOptions, state.survey.audience));
  }
  if (state.survey.mood) {
    addSummaryRow("오늘 컨디션", optionLabel(moodOptions, state.survey.mood));
  }
  if (state.survey.avoid.length > 0) {
    const avoidLabel = state.survey.avoid.map((value) => optionLabel(avoidOptions, value)).join(", ");
    addSummaryRow("보고 싶지 않은 요소", avoidLabel);
  }
  if (state.posterChoices.likedSeedMovieIds.length) {
    addSummaryRow("포스터 취향", `${state.posterChoices.likedSeedMovieIds.length} / ${LIKE_LIMIT}`);
  }
  addSummaryRow("추천 방식", modeLabel);
  addSummaryRow("요청 엔진", `${provider.title} · ${model}`);
  addSummaryRow("실제 처리", resultSource);
  addSummaryRow("분석 깊이", isAiBacked
    ? providerValue === "gpt"
      ? (response.mode || state.run.mode) === "precise" ? "GPT 정밀 분석" : "GPT 빠른 분석"
      : (response.mode || state.run.mode) === "precise" ? "로컬 정밀 추천" : "로컬 빠른 추천"
    : "fallback 추천");

  side.appendChild(summary);

  const actions = createElement("div", "ai-result-side-actions");

  const topRow = createElement("div", "ai-result-side-actions-row");
  const retryButton = createElement("button", "ai-secondary-button", "다시 추천받기");
  retryButton.type = "button";
  retryButton.addEventListener("click", () => setStep("mode"));
  topRow.appendChild(retryButton);

  const resetButton = createElement("button", "ai-secondary-button", "초기화 후 메인으로");
  resetButton.type = "button";
  resetButton.addEventListener("click", resetProfile);
  topRow.appendChild(resetButton);

  actions.appendChild(topRow);

  const homeButton = createElement("button", "ai-secondary-button", "메인으로 이동");
  homeButton.type = "button";
  homeButton.style.width = "100%";
  homeButton.addEventListener("click", goToMainPage);
  actions.appendChild(homeButton);

  side.appendChild(actions);

  const list = createElement("div", "ai-result-list");
  recommendations.forEach((item, index) => {
    list.appendChild(renderResultCard(item, index, {
      providerValue,
      mode: response.mode || state.run.mode,
      isAiBacked,
    }));
  });

  layout.appendChild(side);
  layout.appendChild(list);

  return layout;
}

async function resetProfile() {
  if (!window.confirm("브라우저 ID와 서버 추천 이력을 초기화하고 메인으로 돌아갈까?")) {
    return;
  }

  const currentId = state.anonymousId;

  try {
    if (currentId) {
      await deleteRecommendationSession(currentId);
    }
    showToast("초기화했어.");
  } catch (error) {
    showToast(error.message || "서버 이력 삭제는 실패했지만 브라우저 ID는 지울게.");
  }

  localStorage.removeItem(STORAGE_KEY);
  resetInputs(false);
  goToMainPage();
}

function render() {
  clearChildren(screen);
  screen.classList.toggle("is-poster-screen", state.step === "posters");
  progress.textContent = progressText[state.step] || "";

  const subHeader = document.querySelector(".ai-sub-header");
  if (subHeader) {
    const showBatch = state.step === "posters" && posterBatchCount() > 1;
    subHeader.style.display = showBatch ? "flex" : "none";
  }

  const batchBtn = document.getElementById("aiNextBatchButton");
  if (batchBtn) {
    if (state.step === "posters" && posterBatchCount() > 1) {
      batchBtn.style.display = "flex";
      batchBtn.onclick = () => showPosterBatch((state.posters.activeBatchIndex + 1) % posterBatchCount());
    } else {
      batchBtn.style.display = "none";
      batchBtn.onclick = null;
    }
  }

  const progressLine = document.getElementById("aiProgressLine");
  if (progressLine) {
    const stepOrder = ["audience", "mood", "avoid", "posters", "mode"];
    let idx = stepOrder.indexOf(state.step);
    if (idx !== -1) {
      progressLine.style.width = `${((idx + 1) / 5) * 100}%`;
    } else {
      progressLine.style.width = "100%";
    }
  }

  switch (state.step) {
    case "audience":
      screen.appendChild(renderAudienceStep());
      break;
    case "mood":
      screen.appendChild(renderMoodStep());
      break;
    case "avoid":
      screen.appendChild(renderAvoidStep());
      break;
    case "posters":
      screen.appendChild(renderPosterStep());
      break;
    case "mode":
      screen.appendChild(renderModeStep());
      break;
    case "loading":
      screen.appendChild(renderLoadingStep());
      break;
    case "results":
      screen.appendChild(renderResultsStep());
      break;
    case "empty":
      screen.appendChild(renderEmptyStep());
      break;
    case "error":
      screen.appendChild(renderRecommendationErrorStep());
      break;
    case "sessionError":
      screen.appendChild(renderSessionErrorStep());
      break;
    default:
      screen.appendChild(renderAudienceStep());
      break;
  }
}

backButton.addEventListener("click", () => {
  const previousStep = stepBackMap[state.step];
  if (previousStep) {
    clearStateFromStep(previousStep);
    setStep(previousStep);
    return;
  }

  goToPreviousPage();
});

render();
ensureSession();
