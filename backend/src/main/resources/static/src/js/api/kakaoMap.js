// ============================================
// 0. Kakao SDK 로드 확인
// ============================================
console.log("카카오 확인:", window.kakao);

// ============================================
// 1. DOM 로드 후 실행
// ============================================
document.addEventListener("DOMContentLoaded", function () {

  // ============================================
  // 2. 요소 가져오기
  // ============================================
  const regionInput = document.getElementById("regionInput");
  const regionDropdown = document.getElementById("regionDropdown");

  if (!regionInput || !regionDropdown) {
    console.error("regionInput 또는 regionDropdown 없음");
    return;
  }

  // ============================================
  // 3. Kakao SDK 확인
  // ============================================
  if (!window.kakao || !kakao.maps || !kakao.maps.services) {
    console.error("Kakao SDK 로드 실패");
    return;
  }

  const ps = new kakao.maps.services.Places();

  // ============================================
  // 4. 주소 → 동/읍/면/리까지만 추출 (핵심)
  // ============================================
  function extractRegion(address) {
    const parts = address.split(" ");
    let result = [];

    for (let part of parts) {
      result.push(part);

      if (
        part.endsWith("동") ||
        part.endsWith("읍") ||
        part.endsWith("면") ||
        part.endsWith("리")
      ) {
        break;
      }
    }

    return result.join(" ");
  }

  // ============================================
  // 5. 지역 검색
  // ============================================
  function searchRegion(keyword) {
    if (!keyword) {
      regionDropdown.style.display = "none";
      return;
    }

    ps.keywordSearch(keyword, function (data, status) {
      console.log("상태:", status);
      console.log("데이터:", data);

      if (status === kakao.maps.services.Status.OK) {
        renderDropdown(data, keyword);
      } else {
        regionDropdown.style.display = "none";
      }
    });
  }

  // ============================================
  // 6. 드롭다운 렌더링
  // ============================================
  function renderDropdown(data, keyword) {
    regionDropdown.innerHTML = "";

    const uniqueSet = new Set();

    data.forEach(item => {
      const address = item.address_name;
      const shortAddress = extractRegion(address);

      // 👉 키워드 포함된 것만 필터 (핵심 추가)
      if (!shortAddress.includes(keyword)) return;

      if (uniqueSet.has(shortAddress)) return;
      uniqueSet.add(shortAddress);

      const li = document.createElement("li");

      li.innerHTML = shortAddress.replace(
        new RegExp(keyword, "gi"),
        `<b>${keyword}</b>`
      );

      li.addEventListener("click", () => {
        regionInput.value = shortAddress;
        regionDropdown.style.display = "none";
      });

      regionDropdown.appendChild(li);
    });

    regionDropdown.style.display =
      regionDropdown.children.length > 0 ? "block" : "none";
  }

  // ============================================
  // 7. 입력 이벤트
  // ============================================
  regionInput.addEventListener("input", function () {
    const keyword = this.value.trim();
    searchRegion(keyword);
  });

  // ============================================
  // 8. 외부 클릭 시 닫기
  // ============================================
  document.addEventListener("click", (e) => {
    if (!e.target.closest(".form-group")) {
      regionDropdown.style.display = "none";
    }
  });

});