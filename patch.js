const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'frontend/src/js/pages/daboyeoAi.js');
let code = fs.readFileSync(filePath, 'utf-8');

// 1. setStep 수정
const setStepRegex = /function setStep\(nextStep\) \{[\s\S]*?render\(\);\s*screen\.focus\(\{ preventScroll: true \}\);\s*\}/;
const newSetStep = `function setStep(nextStep) {
  if (state.stepTimer) {
    window.clearTimeout(state.stepTimer);
    state.stepTimer = null;
  }

  const performStepChange = () => {
    state.step = nextStep;
    if (nextStep === "posters") {
      ensurePostersLoaded();
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
}`;
code = code.replace(setStepRegex, newSetStep);

// 2. renderTitle 수정
const renderTitleRegex = /function renderTitle\(parts\) \{[\s\S]*?return title;\s*\}/;
const newRenderTitle = `function renderTitle(parts) {
  const title = createElement("h1", "ai-title");
  parts.forEach((part) => {
    if (part.highlight) {
      title.appendChild(createElement("span", "ai-highlight", part.text));
    } else {
      title.append(document.createTextNode(part.text));
    }
  });
  return title;
}`;
code = code.replace(renderTitleRegex, newRenderTitle);

// 3. renderStepShell -> renderSplitLayout 치환 및 renderSummary 업데이트
const stepShellRegex = /function renderStepShell\(\{ kicker, titleParts, description, content, wide = false \}\) \{[\s\S]*?return section;\s*\}/;
const newSplitLayout = `function renderSplitLayout({ kicker, titleParts, description, content }) {
  const section = createElement("section", "ai-split-layout");
  
  const leftPane = createElement("div", "ai-split-left");
  leftPane.appendChild(createElement("p", "ai-kicker", kicker));
  leftPane.appendChild(renderTitle(titleParts));
  leftPane.appendChild(createElement("p", "ai-description", description));

  const summary = renderSummary();
  if (summary) {
    leftPane.appendChild(summary);
  }

  const rightPane = createElement("div", "ai-split-right");
  rightPane.appendChild(content);

  section.appendChild(leftPane);
  section.appendChild(rightPane);

  return section;
}`;
code = code.replace(stepShellRegex, newSplitLayout);

const renderSummaryRegex = /function renderSummary\(\) \{[\s\S]*?return summary;\s*\}/;
const newRenderSummary = `function renderSummary() {
  const rows = [];
  if (state.survey.audience) {
    rows.push(["함께 볼 사람", optionLabel(audienceOptions, state.survey.audience)]);
  }
  if (state.survey.mood) {
    rows.push(["오늘 컨디션", optionLabel(moodOptions, state.survey.mood)]);
  }
  if (state.survey.avoid.length > 0) {
    const avoidLabel = state.survey.avoid.map((value) => optionLabel(avoidOptions, value)).join(", ");
    rows.push(["피하고 싶은 것", avoidLabel]);
  }
  if (state.posterChoices.likedSeedMovieIds.length) {
    rows.push(["포스터 취향", \`끌림 \${state.posterChoices.likedSeedMovieIds.length}/\${LIKE_LIMIT}\`]);
  }

  if (rows.length === 0) return null;

  const summary = createElement("div", "ai-summary");
  rows.forEach(([label, value]) => {
    const row = createElement("div", "ai-summary-row");
    row.appendChild(createElement("strong", null, label));
    row.appendChild(document.createTextNode(\` : \${value}\`));
    summary.appendChild(row);
  });
  return summary;
}`;
code = code.replace(renderSummaryRegex, newRenderSummary);

// 4. renderChoiceGrid -> renderOptionList
const choiceGridRegex = /function renderChoiceGrid\(options, selectedValue, onSelect\) \{[\s\S]*?return grid;\s*\}/;
const newOptionList = `function renderOptionList(options, selectedValueOrArray, onSelect, isMulti = false) {
  const list = createElement("div", "ai-option-list");
  list.style.display = 'flex';
  list.style.flexDirection = 'column';
  list.style.gap = '14px';
  list.style.width = '100%';

  options.forEach((option) => {
    const isSelected = isMulti 
      ? selectedValueOrArray.includes(option.value) 
      : selectedValueOrArray === option.value;
      
    const button = createElement("button", ["ai-glass-btn", "can-hover", isSelected ? "is-selected" : null]);
    button.type = "button";
    
    // 모바일 클릭 처리
    button.addEventListener("click", (e) => {
      if (window.innerWidth <= 768 && !isMulti) {
        if (!button.classList.contains("is-selected") && !button.classList.contains("is-expanded")) {
          // 확장
          document.querySelectorAll('.ai-glass-btn').forEach(btn => btn.classList.remove("is-expanded"));
          button.classList.add("is-expanded");
          return;
        }
      }
      onSelect(option.value);
    });

    button.appendChild(createElement("span", "ai-glass-btn-title", option.label));
    if (option.hint || option.description) {
      button.appendChild(createElement("span", "ai-glass-btn-desc", option.hint || option.description));
    }
    list.appendChild(button);
  });

  return list;
}`;
code = code.replace(choiceGridRegex, newOptionList);

// 5. Step Renderers
code = code.replace(
  /function renderAudienceStep\(\) \{[\s\S]*?\}\n/,
  `function renderAudienceStep() {
  const content = renderOptionList(audienceOptions, state.survey.audience, (value) => {
    state.survey.audience = value;
    render();
    scheduleStep("mood");
  });
  return renderSplitLayout({
    kicker: "AI GUIDE 01",
    titleParts: [{ text: "누구랑\\n볼 거야?" }],
    description: "상황이 다르면 좋은 영화도 달라져.\\n같이 볼 사람부터 잡고 갈게.",
    content,
  });
}\n`
);

code = code.replace(
  /function renderMoodStep\(\) \{[\s\S]*?\}\n/,
  `function renderMoodStep() {
  const content = renderOptionList(moodOptions, state.survey.mood, (value) => {
    state.survey.mood = value;
    render();
    scheduleStep("avoid");
  });
  return renderSplitLayout({
    kicker: "AI GUIDE 02",
    titleParts: [{ text: "오늘\\n" }, { text: "컨디션", highlight: true }, { text: "은?" }],
    description: "지금 보고 싶은 감정의 온도를 고르면\\n추천 방향이 바로 좁혀져.",
    content,
  });
}\n`
);

// 6. renderAvoidStep
const avoidStepRegex = /function renderAvoidStep\(\) \{[\s\S]*?return renderStepShell\(\{[\s\S]*?\}\);\n\}/;
const newAvoidStep = `function renderAvoidStep() {
  const panel = createElement("div", "ai-avoid-panel");
  panel.style.width = '100%';
  
  const content = renderOptionList(avoidOptions, state.survey.avoid, toggleAvoid, true);
  panel.appendChild(content);

  const ctaRow = createElement("div", "ai-cta-row");
  const completeBtn = createElement("button", ["ai-primary-cta", state.survey.avoid.length > 0 ? "is-active" : null], 
    state.survey.avoid.length > 0 ? "선택 완료" : "딱히 없음 (건너뛰기)");
  completeBtn.type = "button";
  completeBtn.classList.add(state.survey.avoid.length > 0 ? "is-active" : "is-active"); // always enable since skip is ok
  completeBtn.addEventListener("click", () => setStep("posters"));
  ctaRow.appendChild(completeBtn);
  panel.appendChild(ctaRow);

  return renderSplitLayout({
    kicker: "AI GUIDE 03",
    titleParts: [{ text: "피하고\\n싶은 건?" }],
    description: "단일 선택이 아니라 여러 개를 고를 수 있어.\\n완전히 배제하기보다 추천 점수에서 낮춥니다.",
    content: panel,
  });
}`;
code = code.replace(avoidStepRegex, newAvoidStep);

// 7. renderPosterStep Header updates & grid
const posterStepRegex = /function renderPosterStep\(\) \{[\s\S]*?return wrapper;\n\}/;
const newPosterStep = `function renderPosterStep() {
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

  const ctaRow = createElement("div", "ai-cta-row");
  const isSatisfied = canContinuePosterDiagnosis();
  const completeBtn = createElement("button", ["ai-primary-cta", isSatisfied ? "is-active" : null], "설문 완료");
  completeBtn.type = "button";
  if (isSatisfied) {
    completeBtn.addEventListener("click", () => setStep("mode"));
  }
  ctaRow.appendChild(completeBtn);
  
  const content = createElement("div", "ai-poster-pane");
  const countPane = createElement("div", "ai-poster-count");
  countPane.textContent = \`선택 \${state.posterChoices.likedSeedMovieIds.length} / \${LIKE_LIMIT}\`;
  
  content.appendChild(countPane);
  content.appendChild(grid);
  content.appendChild(ctaRow);

  return renderSplitLayout({
    kicker: "AI GUIDE 04",
    titleParts: [{ text: "포스터만 보고\\n" }, { text: "끌림", highlight: true }, { text: "을 골라봐" }],
    description: \`\${state.posters.activeBatchIndex + 1}/\${posterBatchCount()}라운드. 끌리는 포스터 \${MIN_LIKE_COUNT}~\${LIKE_LIMIT}개를 골라주세요.\`,
    content,
  });
}`;
code = code.replace(posterStepRegex, newPosterStep);

// 8. renderPosterCard - overlay instead of mark
const posterCardRegex = /function renderPosterCard\(movie\) \{[\s\S]*?return card;\n\}/;
const newPosterCard = `function renderPosterCard(movie) {
  const id = String(movie.id);
  const isLiked = state.posterChoices.likedSeedMovieIds.includes(id);
  const card = createElement("button", ["ai-poster-card", isLiked ? "is-selected" : null]);
  card.type = "button";
  card.addEventListener("click", () => selectPoster(id));

  if (movie.posterUrl) {
    const image = createElement("img");
    image.alt = \`\${movie.title || "영화"} 포스터\`;
    image.loading = "lazy";
    image.src = movie.posterUrl;
    image.addEventListener("error", () => image.removeAttribute("src"));
    card.appendChild(image);
  }

  const overlay = createElement("div", "ai-poster-overlay");
  overlay.appendChild(createElement("span", null, movie.title || "이름 없는 포스터"));
  card.appendChild(overlay);

  return card;
}`;
code = code.replace(posterCardRegex, newPosterCard);

// 9. renderModeStep
const modeStepRegex = /function renderModeStep\(\) \{[\s\S]*?return renderStepShell\(\{[\s\S]*?\}\);\n\}/;
const newModeStep = `function renderModeStep() {
  const content = renderOptionList(modeOptions, state.run.mode, (value) => runRecommendation(value));
  return renderSplitLayout({
    kicker: "AI GUIDE 05",
    titleParts: [{ text: "어떤 방식으로\\n추천할까?" }],
    description: "빠른 추천결과를 즉시 볼지, 혹은 깊이 분석할지 선택해.",
    content,
  });
}`;
code = code.replace(modeStepRegex, newModeStep);

fs.writeFileSync(filePath, code, 'utf-8');
console.log('Patch complete.');
