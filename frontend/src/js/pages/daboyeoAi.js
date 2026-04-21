import {
  createRecommendationSession,
  deleteRecommendationSession,
  getPosterSeed,
  requestRecommendations,
  sendRecommendationFeedback,
} from "../api/client.js";

const STORAGE_KEY = "daboyeoAnonymousId";
const SEARCH_CONTEXT_KEY = "daboyeoSearchContext";
const MAIN_PAGE_URL = "../../index.html";
const LOCAL_PREVIEW_ID_PREFIX = "local-preview-";
const POSTER_POOL_SIZE = 30;
const POSTER_LIMIT = 10;
const POSTER_BATCH_SIZE = 5;
const MIN_LIKE_COUNT = 3;
const LIKE_LIMIT = 5;
const AUTO_ADVANCE_MS = 320;
const POSTER_AUTO_ADVANCE_MS = 650;

const audienceOptions = [
  { value: "alone", label: "혼자", hint: "내 취향만 선명하게 맞춰볼게." },
  { value: "friends", label: "친구", hint: "대화거리와 텐션을 같이 본다." },
  { value: "date", label: "연인", hint: "분위기와 몰입감을 우선한다." },
  { value: "family", label: "가족", hint: "호불호가 덜 갈리는 선택을 찾는다." },
  { value: "child", label: "아이와 함께", hint: "관람 등급과 자극 요소를 강하게 거른다." },
];

const moodOptions = [
  { value: "light", label: "가볍게", hint: "부담 없이 보기 좋은 쪽으로." },
  { value: "immersive", label: "깊게 몰입", hint: "서사와 여운이 있는 작품 위주로." },
  { value: "exciting", label: "신나는 것", hint: "속도감과 재미를 높게 본다." },
  { value: "calm", label: "잔잔한 것", hint: "편안한 리듬을 선호로 잡는다." },
  { value: "tense", label: "긴장감 있는 것", hint: "스릴과 집중감을 더 반영한다." },
];

const avoidOptions = [
  {
    value: "violence",
    label: "잔인한 장면",
    hint: "수위 높은 장면은 최대한 빼고 싶어.",
    imageUrl: "https://images.unsplash.com/photo-1764005677020-52e3266af0fd?auto=format&fit=crop&fm=jpg&ixlib=rb-4.1.0&q=80&w=1200",
  },
  {
    value: "too_long",
    label: "너무 긴 영화",
    hint: "늘어지는 러닝타임은 오늘 부담돼.",
    imageUrl: "https://images.unsplash.com/photo-1751630991322-f935847f16c3?auto=format&fit=crop&fm=jpg&ixlib=rb-4.1.0&q=80&w=1200",
  },
  {
    value: "complex",
    label: "어려운 이야기",
    hint: "해석이 너무 많이 필요한 건 피하고 싶어.",
    imageUrl: "https://images.unsplash.com/photo-1758951995614-1a223f1512e4?auto=format&fit=crop&fm=jpg&ixlib=rb-4.1.0&q=80&w=1200",
  },
  {
    value: "sad_ending",
    label: "슬픈 결말",
    hint: "보고 나서 처지는 여운은 오늘 말고.",
    imageUrl: "https://images.unsplash.com/photo-1740101957152-f5df81e3b60f?auto=format&fit=crop&fm=jpg&ixlib=rb-4.1.0&q=80&w=1200",
  },
  {
    value: "loud",
    label: "시끄러운 영화",
    hint: "볼륨이나 자극이 센 건 조금 부담돼.",
    imageUrl: "https://images.unsplash.com/photo-1759230766134-e3ff1c27d20e?auto=format&fit=crop&fm=jpg&ixlib=rb-4.1.0&q=80&w=1200",
  },
];

const avoidNoneOption = {
  value: "__none__",
  label: "딱히 없음",
  hint: "걸리는 요소 없이 넓게 추천받을래.",
  imageUrl: "https://images.unsplash.com/photo-1762541693135-fb989de961e1?auto=format&fit=crop&fm=jpg&ixlib=rb-4.1.0&q=80&w=1200",
};

const modeOptions = [
  {
    value: "fast",
    label: "빠른 추천",
    model: "E2B Q4",
    description: "짧은 이유와 바로 볼 후보가 필요할 때. 응답 속도를 우선한다.",
    tags: ["빠름", "간단한 이유", "상위 후보"],
  },
  {
    value: "precise",
    label: "정밀 추천",
    model: "E4B Q4",
    description: "후보를 더 꼼꼼히 재정렬하고 포스터 취향 분석까지 보고 싶을 때.",
    tags: ["정밀", "취향 분석", "재정렬"],
  },
];

const progressText = {
  audience: "1 / 5 상황",
  mood: "2 / 5 컨디션",
  avoid: "3 / 5 회피",
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

const state = {
  step: "audience",
  sessionStatus: "pending",
  anonymousId: localStorage.getItem(STORAGE_KEY),
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
    isPreview: false,
    activeBatchIndex: 0,
  },
  posterChoices: {
    likedSeedMovieIds: [],
    dislikedSeedMovieIds: [],
  },
  run: {
    status: "idle",
    mode: null,
    response: null,
    error: null,
  },
  feedback: new Map(),
  avoidNoneSelected: false,
  stepTimer: null,
  toastTimer: null,
};

state.searchContext = readSearchContext();

function createLocalPreviewId() {
  const randomId = window.crypto?.randomUUID
    ? window.crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

  return `${LOCAL_PREVIEW_ID_PREFIX}${randomId}`;
}

function isLocalPreviewSession() {
  return state.sessionStatus === "preview" || state.anonymousId?.startsWith(LOCAL_PREVIEW_ID_PREFIX);
}

function goToMainPage() {
  window.location.href = MAIN_PAGE_URL;
}

function createPreviewPosters() {
  const pool = Array.from({ length: POSTER_POOL_SIZE }, (_, index) => {
    const number = index + 1;

    return {
      id: `preview-${number}`,
      title: `프리뷰 포스터 ${number}`,
      posterUrl: `https://picsum.photos/seed/daboyeo-preview-${number}/480/720`,
      genres: [],
      moods: [],
      pace: null,
      audiences: [],
      avoid: [],
      ageRating: "preview",
    };
  });

  return shuffleItems(pool).slice(0, POSTER_LIMIT);
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

function setStep(nextStep) {
  if (state.stepTimer) {
    window.clearTimeout(state.stepTimer);
    state.stepTimer = null;
  }

  state.step = nextStep;

  if (nextStep === "posters") {
    ensurePostersLoaded();
  }

  render();
  screen.focus({ preventScroll: true });
}

function resetInputs(keepSession = true) {
  state.step = "audience";
  state.survey = { audience: null, mood: null, avoid: [] };
  state.posterChoices = { likedSeedMovieIds: [], dislikedSeedMovieIds: [] };
  state.posters.activeBatchIndex = 0;
  state.run = { status: "idle", mode: null, response: null, error: null };
  state.feedback = new Map();
  state.avoidNoneSelected = false;

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
    state.sessionStatus = "preview";
    state.anonymousId = createLocalPreviewId();
    state.run.error = null;
    if (state.step === "sessionError") {
      state.step = "audience";
    }
    showToast("백엔드 세션 없이 로컬 프리뷰로 볼게.");
  }

  render();
}

async function ensurePostersLoaded(force = false) {
  if (!force && ["loading", "ready"].includes(state.posters.status)) {
    return;
  }

  state.posters = { ...state.posters, status: "loading", error: null, isPreview: false };
  render();

  try {
    const posters = await getPosterSeed(POSTER_LIMIT);
    const items = Array.isArray(posters) ? posters : [];
    state.posters = {
      status: items.length > 0 ? "ready" : "empty",
      items,
      error: null,
      isPreview: false,
      activeBatchIndex: 0,
    };
  } catch (error) {
    state.posters = {
      status: "ready",
      items: createPreviewPosters(),
      error,
      isPreview: true,
      activeBatchIndex: 0,
    };
    showToast("포스터 API 대신 로컬 프리뷰 데이터를 보여줄게.");
  }

  render();
}

function renderTitle(parts) {
  const title = createElement("h1", "ai-title");

  parts.forEach((part) => {
    if (part.highlight) {
      title.appendChild(createElement("span", null, part.text));
    } else {
      title.append(document.createTextNode(part.text));
    }
  });

  return title;
}

function renderStepShell({ kicker, titleParts, description, content, wide = false }) {
  const section = createElement("section", ["ai-step", wide ? "ai-step-wide" : null]);
  const intro = createElement("div", "ai-intro");
  const panel = createElement("div", ["ai-step-panel", wide ? "ai-step-panel-wide" : null]);
  intro.appendChild(createElement("p", "ai-kicker", kicker));
  intro.appendChild(renderTitle(titleParts));
  intro.appendChild(createElement("p", "ai-description", description));

  const summary = renderSummary();
  if (summary) {
    intro.appendChild(summary);
  }

  section.appendChild(intro);
  panel.appendChild(content);
  section.appendChild(panel);

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
    const avoidLabel = state.survey.avoid
      .map((value) => optionLabel(avoidOptions, value))
      .join(", ");
    rows.push(["피하고 싶은 것", avoidLabel]);
  }

  if (state.posterChoices.likedSeedMovieIds.length) {
    rows.push([
      "포스터 취향",
      `끌림 ${state.posterChoices.likedSeedMovieIds.length}/${LIKE_LIMIT}`,
    ]);
  }

  if (rows.length === 0) {
    return null;
  }

  const summary = createElement("div", "ai-summary");

  rows.forEach(([label, value]) => {
    const chip = createElement("div", "ai-summary-chip");
    chip.appendChild(createElement("span", "ai-summary-label", label));
    chip.appendChild(createElement("span", "ai-summary-value", value));
    summary.appendChild(chip);
  });

  return summary;
}

function renderChoiceGrid(options, selectedValue, onSelect) {
  const grid = createElement("div", "ai-choice-grid");

  options.forEach((option) => {
    const button = createElement("button", ["ai-choice-card", selectedValue === option.value ? "is-selected" : null]);
    button.type = "button";
    button.appendChild(createElement("strong", null, option.label));
    button.appendChild(createElement("span", null, option.hint));
    button.addEventListener("click", () => onSelect(option.value));
    grid.appendChild(button);
  });

  return grid;
}

function renderAudienceStep() {
  const content = renderChoiceGrid(audienceOptions, state.survey.audience, (value) => {
    state.survey.audience = value;
    render();
    scheduleStep("mood");
  });

  return renderStepShell({
    kicker: "AI GUIDE 01",
    titleParts: [
      { text: "누구랑\n볼 거야?" },
    ],
    description: "상황이 다르면 좋은 영화도 달라져. 같이 볼 사람부터 잡고 갈게.",
    content,
  });
}

function renderMoodStep() {
  const content = renderChoiceGrid(moodOptions, state.survey.mood, (value) => {
    state.survey.mood = value;
    render();
    scheduleStep("avoid");
  });

  return renderStepShell({
    kicker: "AI GUIDE 02",
    titleParts: [
      { text: "오늘\n" },
      { text: "컨디션", highlight: true },
      { text: "은?" },
    ],
    description: "지금 보고 싶은 감정의 온도를 고르면 추천 방향이 바로 좁혀져.",
    content,
  });
}

function toggleAvoid(value) {
  const exists = state.survey.avoid.includes(value);
  state.survey.avoid = exists
    ? state.survey.avoid.filter((item) => item !== value)
    : [...state.survey.avoid, value];
  state.avoidNoneSelected = false;
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

function isPosterDiagnosisComplete() {
  return state.posterChoices.likedSeedMovieIds.length >= LIKE_LIMIT;
}

function canContinuePosterDiagnosis() {
  return state.posterChoices.likedSeedMovieIds.length >= MIN_LIKE_COUNT;
}

function schedulePosterProgress() {
  if (isPosterDiagnosisComplete()) {
    scheduleStep("mode", POSTER_AUTO_ADVANCE_MS);
  }
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
  const cardGrid = createElement("div", "ai-avoid-grid");

  [...avoidOptions, avoidNoneOption].forEach((option) => {
    const isNone = option.value === avoidNoneOption.value;
    const isSelected = isNone
      ? state.avoidNoneSelected
      : state.survey.avoid.includes(option.value);
    const card = createElement("button", ["ai-avoid-card", isSelected ? "is-selected" : null, isNone ? "is-none" : null]);
    const media = createElement("div", "ai-avoid-card-media");
    const image = createElement("img", "ai-avoid-card-image");
    const content = createElement("div", "ai-avoid-card-content");

    card.type = "button";
    image.src = option.imageUrl;
    image.alt = `${option.label} 선택 이미지`;
    image.loading = "lazy";

    content.appendChild(createElement("strong", null, option.label));
    content.appendChild(createElement("span", null, option.hint));

    media.appendChild(image);
    media.appendChild(createElement("div", "ai-avoid-card-scrim"));
    media.appendChild(content);

    if (isSelected) {
      media.appendChild(createElement("div", "ai-avoid-card-badge", "선택"));
    }

    card.appendChild(media);
    card.addEventListener("click", () => {
      if (isNone) {
        state.survey.avoid = [];
        state.avoidNoneSelected = true;
        render();
        scheduleStep("posters");
        return;
      }

      toggleAvoid(option.value);
    });

    cardGrid.appendChild(card);
  });

  const completeRow = createElement("div", "ai-complete-row");
  const completeButton = createElement("button", "ai-complete-button", "선택 완료");
  completeButton.type = "button";
  completeButton.addEventListener("click", () => setStep("posters"));
  completeRow.appendChild(completeButton);
  completeRow.appendChild(createElement("span", "ai-complete-hint", "여러 개 골라도 돼. 없으면 딱히 없음을 누르면 바로 넘어가."));

  panel.appendChild(cardGrid);
  panel.appendChild(completeRow);

  return renderStepShell({
    kicker: "AI GUIDE 03",
    titleParts: [
      { text: "피하고\n싶은 건?" },
    ],
    description: "완전히 배제하기보다 점수에서 낮춘다. 단, 아이와 함께라면 위험한 후보는 더 강하게 거른다.",
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
  schedulePosterProgress();
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
  card.setAttribute("aria-pressed", String(isLiked));
  card.setAttribute("aria-label", `${movie.title || "영화"} 포스터 ${isLiked ? "선택됨" : "선택하기"}`);
  card.addEventListener("click", () => selectPoster(id));

  const imageWrap = createElement("div", "ai-poster-image-wrap");
  const image = createElement("img", "ai-poster-image");
  image.alt = `${movie.title || "영화"} 포스터`;
  image.loading = "lazy";

  if (movie.posterUrl) {
    image.src = movie.posterUrl;
    image.addEventListener("load", () => card.classList.add("has-image"));
    image.addEventListener("error", () => {
      card.classList.remove("has-image");
      image.removeAttribute("src");
    });
  }

  imageWrap.appendChild(image);
  imageWrap.appendChild(createElement("div", "ai-poster-fallback", movie.title || "포스터"));
  if (isLiked) {
    imageWrap.appendChild(createElement("div", "ai-poster-selected-mark", "✓"));
  }

  card.appendChild(imageWrap);

  return card;
}

function renderPosterStep() {
  if (state.posters.status === "loading" || state.posters.status === "idle") {
    return renderLoadingMessage("포스터를 가져오는 중", "잠깐만 기다려줘.");
  }

  if (state.posters.status === "error") {
    return renderErrorPanel(
      "포스터를 가져오지 못했어",
      state.posters.error?.message || "백엔드 추천 API 연결을 확인해줘.",
      [
        { label: "다시 시도", onClick: () => ensurePostersLoaded(true) },
        { label: "처음부터", onClick: () => setStep("audience"), secondary: true },
      ],
    );
  }

  if (state.posters.status === "empty") {
    return renderErrorPanel(
      "포스터 seed가 비어 있어",
      "서버에 포스터 진단용 데이터가 아직 없어. 가짜 선택지는 만들지 않을게.",
      [{ label: "다시 시도", onClick: () => ensurePostersLoaded(true) }],
    );
  }

  const wrapper = createElement("section", ["ai-step", "ai-step-wide", "ai-poster-step"]);
  const head = createElement("div", "ai-poster-head");
  const intro = createElement("div", "ai-intro");
  intro.appendChild(createElement("p", "ai-kicker", "AI GUIDE 04"));
  intro.appendChild(renderTitle([
    { text: "포스터만 보고\n" },
    { text: "끌림", highlight: true },
    { text: "을 골라봐" },
  ]));

  const summary = renderSummary();
  if (summary) {
    intro.appendChild(summary);
  }

  const counts = createElement("div", "ai-poster-counts");
  counts.appendChild(renderPosterCount("is-like", "선택", state.posterChoices.likedSeedMovieIds.length, LIKE_LIMIT));

  head.appendChild(intro);
  head.appendChild(counts);

  const activeBatch = currentPosterBatch();
  const roundMeta = createElement("div", "ai-round-meta");
  roundMeta.appendChild(createElement(
    "p",
    "ai-complete-hint",
    `${state.posters.activeBatchIndex + 1}/${posterBatchCount()}라운드 · 끌리는 포스터 ${MIN_LIKE_COUNT}~${LIKE_LIMIT}개`,
  ));
  roundMeta.appendChild(renderPosterRounds());

  const grid = createElement("div", "ai-poster-grid");
  activeBatch.forEach((movie) => {
    grid.appendChild(renderPosterCard(movie));
  });

  wrapper.appendChild(head);
  wrapper.appendChild(roundMeta);
  wrapper.appendChild(grid);

  if (canContinuePosterDiagnosis() && !isPosterDiagnosisComplete()) {
    const actionRow = createElement("div", "ai-poster-next-row");
    const nextButton = createElement("button", "ai-complete-button", "이대로 추천받기");
    nextButton.type = "button";
    nextButton.addEventListener("click", () => setStep("mode"));
    actionRow.appendChild(nextButton);
    wrapper.appendChild(actionRow);
  } else if (!canContinuePosterDiagnosis() && state.posters.activeBatchIndex === posterBatchCount() - 1) {
    wrapper.appendChild(createElement("p", "ai-warning-line", `끌리는 포스터를 ${MIN_LIKE_COUNT}개 이상 골라줘.`));
  }

  return wrapper;
}

function renderModeCard(option) {
  const card = createElement("button", ["ai-mode-card", option.value === "fast" ? "is-fast" : "is-precise"]);
  card.type = "button";
  card.addEventListener("click", () => runRecommendation(option.value));

  const top = createElement("div");
  top.appendChild(createElement("strong", null, option.label));
  top.appendChild(createElement("p", null, option.description));

  const meta = createElement("div", "ai-mode-meta");
  meta.appendChild(createElement("span", null, option.model));
  option.tags.forEach((tag) => meta.appendChild(createElement("span", null, tag)));

  card.appendChild(top);
  card.appendChild(meta);

  return card;
}

function renderModeStep() {
  const grid = createElement("div", "ai-mode-grid");
  modeOptions.forEach((option) => grid.appendChild(renderModeCard(option)));

  return renderStepShell({
    kicker: "AI GUIDE 05",
    titleParts: [
      { text: "어떤 방식으로\n추천할까?" },
    ],
    description: "빠른 추천은 E2B Q4, 정밀 추천은 E4B Q4를 쓰는 계약으로 백엔드에 요청한다.",
    content: grid,
  });
}

async function runRecommendation(mode) {
  if (!isLocalPreviewSession() && (state.sessionStatus !== "ready" || !state.anonymousId)) {
    showToast("익명 세션을 먼저 확인할게.");
    await ensureSession();
  }

  if (!state.anonymousId) {
    return;
  }

  state.run = { status: "loading", mode, response: null, error: null };
  setStep("loading");

  if (isLocalPreviewSession()) {
    window.setTimeout(() => {
      state.run = {
        status: "success",
        mode,
        response: createPreviewRecommendationResponse(mode),
        error: null,
      };
      setStep("results");
    }, 420);
    return;
  }

  const payload = {
    anonymousId: state.anonymousId,
    mode,
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
    state.run = { status: "success", mode, response, error: null };

    const recommendations = Array.isArray(response?.recommendations) ? response.recommendations : [];
    setStep(recommendations.length === 0 ? "empty" : "results");
  } catch (error) {
    if (state.posters.isPreview) {
      state.run = {
        status: "success",
        mode,
        response: createPreviewRecommendationResponse(mode),
        error: null,
      };
      setStep("results");
      return;
    }

    state.run = { status: "error", mode, response: null, error };
    setStep("error");
  }
}

function createPreviewRecommendationResponse(mode) {
  const liked = state.posters.items.filter((movie) => (
    state.posterChoices.likedSeedMovieIds.includes(String(movie.id))
  ));
  const sourceMovies = liked.length > 0 ? liked : state.posters.items.slice(0, 3);
  const regionName = searchContextRegion(state.searchContext) || "서울";
  const showDate = state.searchContext?.date || "오늘";
  const previewTimesByRange = {
    morning: ["08:10", "09:40", "10:30"],
    brunch: ["11:40", "13:20", "15:10"],
    night: ["18:10", "20:00", "21:40"],
  };
  const previewTimes = previewTimesByRange[state.searchContext?.timeRange] || ["14:30", "16:30", "18:30"];
  const recommendations = sourceMovies.slice(0, 3).map((movie, index) => ({
    movieId: 9000 + index,
    showtimeId: 19000 + index,
    title: movie.title.replace("프리뷰 포스터", "프리뷰 추천"),
    score: 91 - index * 4,
    reason: "#가볍게 #12세",
    analysisPoint: mode === "precise" ? (index === 0 ? "#애니메이션취향" : "#판타지취향") : "",
    caution: "",
    valuePoint: `#${previewTimes[index] || previewTimes[previewTimes.length - 1]}상영 #좌석여유`,
    providerCode: index % 2 === 0 ? "CGV" : "MEGABOX",
    theaterName: index % 2 === 0 ? "강남" : "코엑스",
    regionName,
    screenName: `${index + 1}관`,
    showDate,
    startsAt: previewTimes[index] || previewTimes[previewTimes.length - 1],
    minPriceAmount: 12000 + index * 1000,
    currencyCode: "KRW",
    bookingUrl: "https://www.cgv.co.kr/",
    posterUrl: movie.posterUrl,
  }));

  return {
    runId: `${LOCAL_PREVIEW_ID_PREFIX}run`,
    mode,
    model: mode === "fast" ? "E2B Q4 preview" : "E4B Q4 preview",
    status: "preview",
    message: "로컬 프리뷰 결과야. Spring API가 켜지면 실제 추천 응답으로 대체된다.",
    fallback: true,
    recommendations,
  };
}

function renderLoadingMessage(title, description) {
  const card = createElement("section", "ai-loading-card");
  card.appendChild(createElement("div", "ai-loader"));
  card.appendChild(createElement("h2", null, title));
  card.appendChild(createElement("p", null, description));
  return card;
}

function renderLoadingStep() {
  const mode = modeOptions.find((option) => option.value === state.run.mode);
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
    state.run.error?.message || "백엔드 API 또는 로컬 Ollama 연결을 확인해줘.",
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
  if (isLocalPreviewSession()) {
    if (action === "like" || action === "dislike") {
      state.feedback.set(feedbackKey(item), action);
      render();
    }

    if (!quiet) {
      showToast("로컬 프리뷰라 서버 저장은 건너뛸게.");
    }
    return;
  }

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

function renderShowtimeItem(label, value) {
  const item = createElement("div", "ai-showtime-item");
  item.appendChild(createElement("strong", null, label));
  item.appendChild(createElement("span", null, value || "정보 없음"));
  return item;
}

function renderSearchContextPanel() {
  const text = searchContextText(state.searchContext);
  if (!text) {
    return null;
  }

  const panel = createElement("div", "ai-result-context");
  panel.appendChild(createElement("strong", null, "상영 조건"));
  panel.appendChild(createElement("span", null, text));
  return panel;
}

function renderResultCard(item, index) {
  const card = createElement("article", ["ai-result-card", index === 0 ? "is-top" : null]);
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

  body.appendChild(createElement("p", "ai-result-copy", item.reason || "취향과 현재 조건에 잘 맞는 후보야."));
  if (item.analysisPoint) {
    const analysisPoint = createElement("p", ["ai-result-copy", "ai-analysis-point"]);
    analysisPoint.appendChild(createElement("strong", null, "분석 포인트"));
    analysisPoint.appendChild(document.createTextNode(` ${item.analysisPoint}`));
    body.appendChild(analysisPoint);
  }
  body.appendChild(createElement("p", "ai-result-copy", item.valuePoint || "시간, 가격, 좌석 조건을 함께 확인해봐."));

  const showtimeGrid = createElement("div", "ai-showtime-grid");
  showtimeGrid.appendChild(renderShowtimeItem("극장", [item.providerCode, item.theaterName].filter(Boolean).join(" ")));
  showtimeGrid.appendChild(renderShowtimeItem("지역/상영관", [item.regionName, item.screenName].filter(Boolean).join(" ")));
  showtimeGrid.appendChild(renderShowtimeItem("상영 시간", formatShowtime(item)));
  showtimeGrid.appendChild(renderShowtimeItem("최저가", formatPrice(item.minPriceAmount, item.currencyCode)));
  body.appendChild(showtimeGrid);

  const actions = createElement("div", "ai-result-actions");
  const bookingButton = createElement("button", "ai-booking-button", "예매보기");
  bookingButton.type = "button";
  bookingButton.addEventListener("click", () => openBooking(item));
  actions.appendChild(bookingButton);

  const selectedFeedback = state.feedback.get(feedbackKey(item));
  const likeButton = createElement("button", ["ai-feedback-button", selectedFeedback === "like" ? "is-selected" : null], "끌려요");
  likeButton.type = "button";
  likeButton.addEventListener("click", () => sendFeedback(item, "like"));
  actions.appendChild(likeButton);

  const dislikeButton = createElement("button", ["ai-feedback-button", selectedFeedback === "dislike" ? "is-selected" : null], "별로예요");
  dislikeButton.type = "button";
  dislikeButton.addEventListener("click", () => sendFeedback(item, "dislike"));
  actions.appendChild(dislikeButton);

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
  const layout = createElement("section", "ai-result-layout");
  const side = createElement("aside", "ai-result-side");

  side.appendChild(createElement("p", "ai-kicker", "AI RESULT"));
  side.appendChild(renderTitle([
    { text: "지금은\n" },
    { text: "이 영화", highlight: true },
    { text: "가 좋아" },
  ]));
  side.appendChild(createElement("p", "ai-result-note", response.message || "설문, 포스터 취향, 현재 상영 후보를 합쳐 추천했어."));
  const searchContextPanel = renderSearchContextPanel();
  if (searchContextPanel) {
    side.appendChild(searchContextPanel);
  }

  const meta = createElement("div", "ai-result-meta");
  meta.appendChild(createElement("span", null, modeLabel));
  if (response.model) {
    meta.appendChild(createElement("span", null, response.model));
  }
  if (response.runId) {
    meta.appendChild(createElement("span", null, `run ${String(response.runId).slice(0, 8)}`));
  }
  side.appendChild(meta);

  if (response.fallback) {
    side.appendChild(createElement("p", "ai-warning-line", "모델 응답 검증에 실패해서 코드 점수 기준으로 보여주고 있어."));
  }

  const actions = createElement("div", "ai-panel-actions");
  const retryButton = createElement("button", "ai-secondary-button", "다시 추천");
  retryButton.type = "button";
  retryButton.addEventListener("click", () => setStep("mode"));
  actions.appendChild(retryButton);

  const homeButton = createElement("button", "ai-secondary-button", "메인으로 돌아가기");
  homeButton.type = "button";
  homeButton.addEventListener("click", goToMainPage);
  actions.appendChild(homeButton);

  const resetButton = createElement("button", "ai-reset-button", "초기화 후 메인");
  resetButton.type = "button";
  resetButton.addEventListener("click", resetProfile);
  actions.appendChild(resetButton);
  side.appendChild(actions);

  const list = createElement("div", "ai-result-list");
  recommendations.forEach((item, index) => {
    list.appendChild(renderResultCard(item, index));
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
    if (currentId && !isLocalPreviewSession()) {
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
  progress.textContent = `${progressText[state.step] || ""}${isLocalPreviewSession() ? " · 로컬 프리뷰" : ""}`;
  backButton.hidden = !stepBackMap[state.step];

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
    setStep(previousStep);
  }
});

render();
ensureSession();
