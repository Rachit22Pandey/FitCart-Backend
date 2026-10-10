// FitCart Frontend Client
const API_BASE = "";

// Authenticated account state
let currentUser = null;
let currentRole = null;
let currentUserId = null;
let currentSellerId = null;
let csrfToken = null;
let csrfHeaderName = "X-XSRF-TOKEN";

let categories = [];
let products = [];
let cart = [];
let wishlist = [];

document.addEventListener("DOMContentLoaded", initializeApp);

// ==================== INITIALIZATION ====================

async function initializeApp() {
  const main = document.getElementById("mainContent");
  if (main) {
    main.innerHTML = `
      <p style="padding:2rem;color:var(--text-muted)">
        Loading your FitCart account...
      </p>`;
  }
  try {
    await refreshCsrfToken();
    const response = await apiFetch("/auth/me");
    const contentType = response.headers.get("content-type") || "";
    if (
      response.status === 401 ||
      response.status === 403 ||
      response.redirected ||
      contentType.includes("text/html")
    ) {
      window.location.replace("/login.html");
      return;
    }
    if (!response.ok) {
      throw new Error(
        `Unable to load account (HTTP ${response.status}).`
      );
    }
    currentUser = await response.json();
    currentRole = String(currentUser.role || "")
      .trim()
      .toUpperCase()
      .replace(/^ROLE_/, "");
    currentUserId = Number(currentUser.userId);
    currentSellerId =
      currentRole === "SELLER" ? currentUserId : null;
    if (
      !currentUserId ||
      !["BUYER", "SELLER", "ADMIN"].includes(currentRole)
    ) {
      throw new Error(
        "The authenticated account has an invalid ID or role."
      );
    }
    renderCurrentUser();
    await loadCategories();
    renderDashboardForRole();
  } catch (error) {
    console.error("FitCart initialization failed:", error);
    if (main) {
      main.innerHTML = `
        <section class="form-card"
                 style="max-width:700px;margin:2rem auto">
          <h2>Unable to load FitCart</h2>
          <p style="margin-top:.75rem;color:var(--text-muted)">
            We could not verify your signed-in account.
            Please sign in again.
          </p>
          <button class="btn btn-primary"
                  style="margin-top:1rem"
                  onclick="window.location.href='/login.html'">
            Go to Login
          </button>
        </section>`;
    }
  }
}

// ==================== CSRF AND API ====================

async function refreshCsrfToken() {
  try {
    const response = await fetch("/csrf", {
      method: "GET",
      credentials: "same-origin",
      cache: "no-store"
    });
    if (!response.ok) return;
    const data = await response.json();
    csrfToken = data.token || null;
    csrfHeaderName = data.headerName || "X-XSRF-TOKEN";
  } catch (error) {
    console.warn("Could not preload CSRF token:", error);
  }
}

async function apiFetch(url, options = {}) {
  const method = String(options.method || "GET").toUpperCase();
  const headers = new Headers(options.headers || {});
  const safeMethods = ["GET", "HEAD", "OPTIONS", "TRACE"];
  if (!safeMethods.includes(method)) {
    if (!csrfToken) {
      await refreshCsrfToken();
    }
    if (csrfToken) {
      headers.set(csrfHeaderName, csrfToken);
    }
  }
  return fetch(url, {
    ...options,
    method,
    headers,
    credentials: "same-origin"
  });
}

function renderCurrentUser() {
  const nameElement = document.getElementById("currentUserName");
  const emailElement = document.getElementById("currentUserEmail");
  const roleElement = document.getElementById("currentUserRole");
  if (nameElement) {
    nameElement.textContent =
      currentUser.name || currentUser.email;
  }
  if (emailElement) {
    emailElement.textContent = currentUser.email || "";
  }
  if (roleElement) {
    roleElement.textContent = currentRole;
  }
  const navActions = document.querySelector(".nav-actions");
  if (navActions) {
    navActions.style.display =
      currentRole === "BUYER" ? "flex" : "none";
  }
}

async function logout() {
  try {
    const response = await apiFetch("/logout", {
      method: "POST"
    });
    if (response.ok || response.redirected) {
      window.location.replace("/login.html?logout");
      return;
    }
    showToast("Logout failed. Please try again.");
  } catch (error) {
    console.error("Logout failed:", error);
    showToast("Could not log out. Please try again.");
  }
}

function showToast(message) {
  const toast = document.getElementById("notification");
  if (!toast) return;
  toast.innerText = message;
  toast.style.display = "block";
  setTimeout(() => {
    toast.style.display = "none";
  }, 3000);
}

// ==================== ROLE-BASED DASHBOARDS ====================

function renderDashboardForRole() {
  const tabsContainer = document.getElementById("roleTabs");
  const main = document.getElementById("mainContent");
  tabsContainer.innerHTML = "";
  if (currentRole === "BUYER") {
    tabsContainer.innerHTML = `
      <button class="tab-btn active"
              onclick="setTab(this, renderBuyerCatalog)">
        Catalog & Products
      </button>
      <button class="tab-btn"
              onclick="setTab(this, renderBuyerOrders)">
        My Orders
      </button>`;
    renderBuyerCatalog();
    updateCartBadge();
    updateWishlistBadge();
  } else if (currentRole === "SELLER") {
    tabsContainer.innerHTML = `
      <button class="tab-btn active"
              onclick="setTab(this, renderSellerProducts)">
        My Products & Inventory
      </button>
      <button class="tab-btn"
              onclick="setTab(this, renderSellerAddProduct)">
        Add New Product
      </button>
      <button class="tab-btn"
              onclick="setTab(this, renderSellerOrders)">
        Customer Orders
      </button>`;
    renderSellerProducts();
  } else if (currentRole === "ADMIN") {
    tabsContainer.innerHTML = `
      <button class="tab-btn active"
              onclick="setTab(this, renderAdminCategories)">
        Categories
      </button>
      <button class="tab-btn"
              onclick="setTab(this, renderAdminOrders)">
        Platform Orders
      </button>
      <button class="tab-btn"
              onclick="setTab(this, renderAdminUsers)">
        User Management
      </button>`;
    renderAdminCategories();
  } else {
    tabsContainer.innerHTML = "";
    main.innerHTML = `
      <section class="form-card">
        <h2>Access denied</h2>
        <p>This account does not have a supported FitCart role.</p>
      </section>`;
  }
}

function setTab(button, renderFunction) {
  document.querySelectorAll(".tab-btn").forEach((tab) => {
    tab.classList.remove("active");
  });
  button.classList.add("active");
  renderFunction();
}

// ==================== DATA FETCHING ====================

async function loadCategories() {
  try {
    const response = await apiFetch("/categories");
    if (response.ok) {
      categories = await response.json();
    }
  } catch (error) {
    console.error("Error loading categories:", error);
  }
}

async function loadProducts(keyword = "", categoryId = "") {
  try {
    const params = new URLSearchParams();
    if (keyword) {
      params.set("keyword", keyword);
    }
    if (categoryId) {
      params.set("categoryId", categoryId);
    }
    const response = await apiFetch(
      `/products?${params.toString()}`
    );
    if (response.ok) {
      products = await response.json();
    }
  } catch (error) {
    console.error("Error loading products:", error);
  }
}

// ==================== BUYER HOMEPAGE DESIGN ====================

function ensureFitcartBuyerStyles() {
  if (document.getElementById("fitcart-buyer-home-styles")) {
    return;
  }
  const style = document.createElement("style");
  style.id = "fitcart-buyer-home-styles";
  style.textContent = `
    .fitcart-hero {
      position: relative;
      display: grid;
      grid-template-columns: minmax(0, 1.1fr) minmax(250px, .9fr);
      align-items: center;
      gap: 28px;
      min-height: 350px;
      padding: clamp(25px, 5vw, 54px);
      margin-bottom: 24px;
      border: 1px solid var(--border, #303930);
      border-radius: 24px;
      overflow: hidden;
      background: linear-gradient(
        120deg,
        #1b2518 0%,
        #101510 62%,
        #1c2816 100%
      );
    }
    .fitcart-hero-copy {
      position: relative;
      z-index: 2;
    }
    .fitcart-eyebrow,
    .fitcart-section-kicker {
      color: var(--primary, #c8f169);
      font-size: .72rem;
      font-weight: 900;
      letter-spacing: 2px;
    }
    .fitcart-hero h1 {
      margin: 18px 0;
      font-size: clamp(2.4rem, 4.5vw, 4.3rem);
      font-weight: 950;
      line-height: .98;
      letter-spacing: -2px;
    }
    .fitcart-hero h1 span {
      color: var(--primary, #c8f169);
    }
    .fitcart-hero-copy > p {
      max-width: 490px;
      color: var(--text-muted, #a1aba0);
      line-height: 1.8;
      margin: 0 0 23px;
    }
    .fitcart-hero-actions {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
    }
    .fitcart-hero-actions .btn {
      padding: 13px 21px;
      border-radius: 999px;
    }
    .fitcart-shop-button {
      border: 0;
      cursor: pointer;
      font-weight: 900;
      background: var(--primary, #c8f169);
      color: #10140d;
      padding: 13px 21px;
      border-radius: 999px;
    }
    .fitcart-shop-button:hover {
      filter: brightness(.96);
    }
    .fitcart-hero-note {
      font-size: .67rem;
      font-weight: 800;
      letter-spacing: 1px;
      color: var(--text-muted, #a1aba0);
    }
    .fitcart-hero-points {
      display: flex;
      flex-wrap: wrap;
      gap: 9px;
      margin-top: 25px;
    }
    .fitcart-hero-points span {
      padding: 8px 11px;
      border-radius: 999px;
      border: 1px solid #394635;
      background: #111710;
      color: var(--text-muted, #a1aba0);
      font-size: .78rem;
    }
    .fitcart-hero-art {
      position: relative;
      min-height: 250px;
      display: grid;
      place-items: center;
    }
    .fitcart-hero-glow {
      position: absolute;
      width: 230px;
      height: 230px;
      border-radius: 50%;
      background: #c8f169;
      opacity: .13;
      filter: blur(40px);
    }
    .fitcart-hero-ring {
      position: absolute;
      width: 245px;
      height: 245px;
      border: 1px solid #c8f16935;
      border-radius: 50%;
    }
    .fitcart-hero-equipment {
      position: relative;
      display: grid;
      place-items: center;
      gap: 7px;
      width: 210px;
      height: 210px;
      border: 1px solid #c8f16945;
      border-radius: 30px;
      background: linear-gradient(145deg, #222d1e, #101410);
      box-shadow: 0 24px 70px #0005;
      transform: rotate(-4deg);
    }
    .fitcart-hero-equipment-icon {
      font-size: 5.7rem;
      filter: drop-shadow(0 10px 16px #0008);
    }
    .fitcart-hero-equipment-label {
      color: #c8f169;
      font-size: .62rem;
      font-weight: 900;
      letter-spacing: 1.5px;
    }
    .fitcart-hero-floating-badge {
      position: absolute;
      right: 0;
      bottom: 8px;
      display: flex;
      flex-direction: column;
      padding: 13px 16px;
      border-radius: 12px;
      background: #c8f169;
      color: #10140d;
      transform: rotate(3deg);
      box-shadow: 0 12px 32px #0005;
    }
    .fitcart-hero-floating-badge span {
      font-size: .58rem;
      font-weight: 800;
      letter-spacing: 1.5px;
    }
    .fitcart-hero-floating-badge strong {
      font-size: .83rem;
    }
    .fitcart-benefits {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 13px;
      margin: 22px 0 48px;
    }
    .fitcart-benefit {
      display: flex;
      align-items: flex-start;
      gap: 13px;
      padding: 17px;
      border: 1px solid var(--border, #303930);
      border-radius: 14px;
      background: #121712;
    }
    .fitcart-benefit-number {
      color: #c8f169;
      font-size: .78rem;
      font-weight: 900;
      padding-top: 3px;
    }
    .fitcart-benefit strong {
      font-size: .9rem;
    }
    .fitcart-benefit p {
      margin: 5px 0 0;
      color: var(--text-muted, #a1aba0);
      font-size: .78rem;
      line-height: 1.5;
    }
    .fitcart-catalog {
      scroll-margin-top: 110px;
      margin-bottom: 30px;
    }
    .fitcart-catalog-brand {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 11px;
    }
    .fitcart-catalog-logo {
      display: block;
      width: 34px;
      height: 34px;
      flex: 0 0 34px;
      border-radius: 8px;
      box-shadow: 0 4px 16px #c8f16915;
    }
    .fitcart-section-heading {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      gap: 18px;
      margin-bottom: 22px;
    }
    .fitcart-section-heading h2 {
      margin: 9px 0;
      font-size: clamp(1.65rem, 3vw, 2.45rem);
      letter-spacing: -.6px;
    }
    .fitcart-section-heading p {
      color: var(--text-muted, #a1aba0);
      line-height: 1.6;
      margin: 0;
    }
    .fitcart-product-count {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 11px 14px;
      border: 1px solid var(--border, #303930);
      border-radius: 12px;
      white-space: nowrap;
    }
    .fitcart-product-count span {
      color: #c8f169;
      font-size: 1.3rem;
      font-weight: 900;
    }
    .fitcart-product-count small {
      color: var(--text-muted, #a1aba0);
      font-size: .62rem;
      font-weight: 800;
      letter-spacing: 1px;
    }
    .fitcart-filters {
      display: grid;
      grid-template-columns:
        minmax(0, 1fr)
        minmax(170px, 230px)
        minmax(170px, 210px);
      gap: 12px;
      margin-bottom: 18px;
    }
    .fitcart-filters input,
    .fitcart-filters select {
      width: 100%;
      min-width: 0;
      min-height: 48px;
      padding: 0 15px;
      background: #151a15;
      color: var(--text-main, #f4f7f0);
      border: 1px solid var(--border, #303930);
      border-radius: 12px;
    }
    .fitcart-filters input:focus,
    .fitcart-filters select:focus {
      outline: 2px solid #c8f169;
      outline-offset: 1px;
    }
    .fitcart-category-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 9px;
      margin: 0 0 24px;
    }
    .fitcart-chip {
      padding: 9px 14px;
      border: 1px solid var(--border, #303930);
      border-radius: 999px;
      background: #151a15;
      color: var(--text-muted, #a1aba0);
      cursor: pointer;
      font-size: .82rem;
      font-weight: 700;
      transition: .2s ease;
    }
    .fitcart-chip:hover,
    .fitcart-chip.active {
      background: #c8f169;
      border-color: #c8f169;
      color: #10140d;
    }
    .fitcart-product-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 19px;
    }
    .fitcart-product-card {
      min-width: 0;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      border: 1px solid var(--border, #303930);
      border-radius: 18px;
      background: #141914;
      transition: transform .22s ease, border-color .22s ease;
    }
    .fitcart-product-card:hover {
      transform: translateY(-5px);
      border-color: #8eae54;
    }
    .fitcart-product-art {
      position: relative;
      display: grid;
      place-items: center;
      min-height: 175px;
      overflow: hidden;
      background:
        radial-gradient(circle at 50% 62%, #465735 0%, transparent 51%),
        linear-gradient(145deg, #20291d, #101410);
      border-bottom: 1px solid var(--border, #303930);
    }
    .fitcart-product-art::before {
      content: "";
      position: absolute;
      width: 135px;
      height: 135px;
      border: 1px solid #c8f1693b;
      border-radius: 50%;
    }
    .fitcart-art-category {
      position: absolute;
      top: 13px;
      left: 12px;
      max-width: calc(100% - 24px);
      padding: 6px 9px;
      border-radius: 999px;
      background: #0b0e0be8;
      color: #c8f169;
      font-size: .62rem;
      font-weight: 900;
      letter-spacing: .5px;
    }
    .fitcart-product-icon {
      position: relative;
      font-size: 4.3rem;
      filter: drop-shadow(0 12px 13px #0009);
      transition: transform .22s ease;
    }
    .fitcart-product-card:hover .fitcart-product-icon {
      transform: scale(1.12) rotate(-4deg);
    }
    .fitcart-art-caption {
      position: absolute;
      bottom: 11px;
      color: #a5b69a;
      font-size: .58rem;
      font-weight: 800;
      letter-spacing: 1.7px;
    }
    .fitcart-product-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 17px;
    }
    .fitcart-product-content .product-category {
      color: #c8f169;
      font-size: .7rem;
      font-weight: 900;
      letter-spacing: 1px;
      text-transform: uppercase;
    }
    .fitcart-product-content .product-title {
      margin: 7px 0 9px;
      line-height: 1.35;
      font-weight: 850;
      overflow-wrap: anywhere;
    }
    .fitcart-product-content .product-desc {
      color: var(--text-muted, #a1aba0);
      font-size: .84rem;
      line-height: 1.65;
      margin: 0;
    }
    .fitcart-product-content .product-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: auto;
      padding-top: 16px;
    }
    .fitcart-product-content .product-price {
      font-size: 1.3rem;
      font-weight: 900;
      color: var(--text-main, #f4f7f0);
    }
    .fitcart-product-content .product-stock {
      font-size: .78rem;
    }
    .fitcart-product-content .product-stock.in-stock {
      color: #c8f169;
    }
    .fitcart-product-content .product-stock.out-of-stock {
      color: #fb7185;
    }
    .fitcart-product-actions {
      display: flex;
      gap: 9px;
      margin-top: 16px;
    }
    .fitcart-product-actions .fitcart-add-button {
      flex: 1;
      justify-content: center;
      border-radius: 999px;
      background: #c8f169;
      color: #10140d;
      border: 0;
      font-weight: 900;
      cursor: pointer;
      padding: 11px 14px;
    }
    .fitcart-product-actions .fitcart-add-button:disabled {
      opacity: .45;
      cursor: not-allowed;
    }
    .fitcart-wishlist-button {
      min-width: 43px;
      border-radius: 999px;
      justify-content: center;
      font-size: 1.15rem;
    }
    .fitcart-empty-products {
      grid-column: 1 / -1;
      text-align: center;
      padding: 50px 20px;
      background: #121712;
      border: 1px dashed var(--border, #303930);
      border-radius: 16px;
    }
    .fitcart-empty-products > span {
      display: block;
      font-size: 2.5rem;
      color: #c8f169;
    }
    .fitcart-empty-products h3 {
      margin: 12px 0 6px;
    }
    .fitcart-empty-products p {
      color: var(--text-muted, #a1aba0);
    }
    @media (max-width: 1000px) {
      .fitcart-product-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
      .fitcart-hero {
        grid-template-columns: minmax(0, 1fr) minmax(220px, .7fr);
      }
      .fitcart-hero-equipment {
        width: 170px;
        height: 170px;
      }
      .fitcart-hero-equipment-icon {
        font-size: 4.6rem;
      }
      .fitcart-hero-ring {
        width: 200px;
        height: 200px;
      }
      .fitcart-filters {
        grid-template-columns: minmax(0, 1fr) minmax(140px, .6fr);
      }
      .fitcart-filters select:last-child {
        grid-column: 1 / -1;
      }
    }
    @media (max-width: 650px) {
      .fitcart-hero {
        grid-template-columns: 1fr;
        padding: 27px 20px;
      }
      .fitcart-hero h1 {
        font-size: 2.65rem;
      }
      .fitcart-hero-art {
        min-height: 220px;
      }
      .fitcart-benefits {
        grid-template-columns: 1fr;
        margin-bottom: 38px;
      }
      .fitcart-section-heading {
        align-items: flex-start;
      }
      .fitcart-section-heading h2 {
        font-size: 1.7rem;
      }
      .fitcart-product-count {
        gap: 5px;
        padding: 8px;
      }
      .fitcart-product-count small {
        display: none;
      }
      .fitcart-filters {
        grid-template-columns: 1fr;
      }
      .fitcart-filters select:last-child {
        grid-column: auto;
      }
      .fitcart-product-grid {
        grid-template-columns: 1fr;
      }
      .fitcart-product-art {
        min-height: 195px;
      }
    }
  `;
  document.head.appendChild(style);
}

// ==================== BUYER CATALOG ====================

async function renderBuyerCatalog() {
  ensureFitcartBuyerStyles();
  await loadProducts();
  const main = document.getElementById("mainContent");
  if (!main) return;

  const categoryOptions = [
    `<option value="">All Categories</option>`,
    ...categories.map((category) => `
      <option value="${Number(category.categoryId)}">
        ${escapeHtml(category.categoryName)}
      </option>
    `)
  ].join("");

  main.innerHTML = `
    <section class="fitcart-hero">
      <div class="fitcart-hero-copy">
        <div class="fitcart-eyebrow">
          YOUR TRAINING. YOUR RULES.
        </div>
        <h1>
          TRAIN HARD.<br>
          <span>SHOP SMART.</span>
        </h1>
        <p>
          Discover your training essentials, equip your workouts,
          and take the next step toward your fitness goals.
        </p>
        <div class="fitcart-hero-actions">
          <button
            type="button"
            class="fitcart-shop-button"
            onclick="scrollToFitcartProducts()">
            Shop Products <span aria-hidden="true">↘</span>
          </button>
          <span class="fitcart-hero-note">
            BUILT FOR EVERY REP.
          </span>
        </div>
        <div class="fitcart-hero-points">
          <span>🏋️ Strength</span>
          <span>⚡ Performance</span>
          <span>💪 Recovery</span>
        </div>
      </div>
      <div class="fitcart-hero-art" aria-hidden="true">
        <div class="fitcart-hero-glow"></div>
        <div class="fitcart-hero-ring"></div>
        <div class="fitcart-hero-equipment">
          <span class="fitcart-hero-equipment-icon">🏋️</span>
          <span class="fitcart-hero-equipment-label">
            FITNESS ESSENTIALS
          </span>
        </div>
        <div class="fitcart-hero-floating-badge">
          <span>YOUR</span>
          <strong>FITNESS</strong>
          <strong>JOURNEY</strong>
        </div>
      </div>
    </section>

    <section class="fitcart-benefits" aria-label="FitCart benefits">
      <article class="fitcart-benefit">
        <span class="fitcart-benefit-number">01</span>
        <div>
          <strong>Training Gear</strong>
          <p>Find equipment for your routine.</p>
        </div>
      </article>
      <article class="fitcart-benefit">
        <span class="fitcart-benefit-number">02</span>
        <div>
          <strong>Easy Shopping</strong>
          <p>Browse, save, and add to cart.</p>
        </div>
      </article>
      <article class="fitcart-benefit">
        <span class="fitcart-benefit-number">03</span>
        <div>
          <strong>One Fitness Store</strong>
          <p>Explore products in one place.</p>
        </div>
      </article>
    </section>

    <section class="fitcart-catalog" id="fitcartCatalog">
      <div class="fitcart-section-heading">
        <div>
          <div class="fitcart-catalog-brand">
            <svg
              class="fitcart-catalog-logo"
              viewBox="0 0 200 200"
              role="img"
              aria-label="FitCart dumbbell logo"
              xmlns="http://www.w3.org/2000/svg">
              <rect width="200" height="200" rx="42" fill="#101410"/>
              <g fill="#c8f169">
                <rect x="48" y="75" width="15" height="50" rx="5"/>
                <rect x="31" y="65" width="12" height="70" rx="4"/>
                <rect x="137" y="75" width="15" height="50" rx="5"/>
                <rect x="157" y="65" width="12" height="70" rx="4"/>
                <rect x="60" y="91" width="80" height="18" rx="6"/>
              </g>
              <text x="100" y="158" text-anchor="middle"
                    font-family="Arial, sans-serif" font-size="19"
                    font-weight="900" fill="#ffffff">FITCART</text>
            </svg>
            <span class="fitcart-section-kicker">THE FITCART COLLECTION</span>
          </div>
          <h2>Find your next essential.</h2>
          <p>Discover products for your training goals.</p>
        </div>
        <div class="fitcart-product-count">
          <span id="productResultCount">${products.length}</span>
          <small>PRODUCTS</small>
        </div>
      </div>

      <div class="fitcart-filters">
        <input
          type="search"
          id="searchInput"
          placeholder="Search supplements, gym gear..."
          aria-label="Search fitness products"
          oninput="handleSearch()">
        <select
          id="categoryFilter"
          aria-label="Filter by category"
          onchange="handleCategoryFilter(this.value)">
          ${categoryOptions}
        </select>
        <select
          id="fitcartSort"
          aria-label="Sort products"
          onchange="renderFitcartProductGrid()">
          <option value="featured">Recommended</option>
          <option value="price-low">Price: Low to High</option>
          <option value="price-high">Price: High to Low</option>
          <option value="name">Name: A to Z</option>
        </select>
      </div>

      <div class="fitcart-category-chips">
        <button
          type="button"
          class="fitcart-chip active"
          data-category=""
          onclick="chooseFitcartCategory('')">
          All Products
        </button>
        ${categories.map((category) => `
          <button
            type="button"
            class="fitcart-chip"
            data-category="${Number(category.categoryId)}"
            onclick="chooseFitcartCategory('${Number(category.categoryId)}')">
            ${escapeHtml(category.categoryName)}
          </button>
        `).join("")}
      </div>

      <div class="fitcart-product-grid" id="productGrid">
        ${renderProductCards(products)}
      </div>
    </section>
  `;

  renderFitcartProductGrid();
}

function scrollToFitcartProducts() {
  document.getElementById("fitcartCatalog")?.scrollIntoView({
    behavior: "smooth",
    block: "start"
  });
}

async function chooseFitcartCategory(categoryId) {
  const categoryFilter = document.getElementById("categoryFilter");
  if (categoryFilter) {
    categoryFilter.value = String(categoryId);
  }
  await handleCategoryFilter(String(categoryId));
}

function syncFitcartCategoryChips() {
  const selected =
    document.getElementById("categoryFilter")?.value || "";
  document.querySelectorAll(".fitcart-chip").forEach((button) => {
    button.classList.toggle(
      "active",
      button.dataset.category === String(selected)
    );
  });
}

function getFitcartProductIcon(product) {
  const text = `
    ${product.productName || ""}
    ${product.categoryName || ""}
  `.toLowerCase();
  if (/protein|whey|creatine|supplement|nutrition/.test(text)) {
    return "🥤";
  }
  if (/dumbbell|weight|barbell|kettlebell|equipment/.test(text)) {
    return "🏋️";
  }
  if (/glove|boxing|wrap|punch/.test(text)) {
    return "🥊";
  }
  if (/band|resistance/.test(text)) {
    return "💪";
  }
  if (/mat|yoga|stretch/.test(text)) {
    return "🧘";
  }
  if (/bottle|shaker/.test(text)) {
    return "🚰";
  }
  return "⚡";
}

function renderProductCards(list) {
  if (!list || list.length === 0) {
    return `
      <div class="fitcart-empty-products">
        <span>⌕</span>
        <h3>No products found</h3>
        <p>Try a different search or category.</p>
      </div>`;
  }
  return list.map((product) => {
    const stock = Number(product.stockQuantity ?? 0);
    const price = Number(product.price ?? 0);
    const productId = Number(product.productId);
    const productName = escapeHtml(
      product.productName || "Fitness Product"
    );
    return `
      <article class="fitcart-product-card">
        <div class="fitcart-product-art">
          <span class="fitcart-art-category">
            ${escapeHtml(product.categoryName || "FITNESS GEAR")}
          </span>
          <span class="fitcart-product-icon" aria-hidden="true">
            ${getFitcartProductIcon(product)}
          </span>
          <span class="fitcart-art-caption">
            TRAINING ESSENTIAL
          </span>
        </div>
        <div class="fitcart-product-content">
          <div class="product-category">
            ${escapeHtml(product.categoryName || "Fitness Gear")}
          </div>
          <h3 class="product-title">${productName}</h3>
          <p class="product-desc">
            ${escapeHtml(
              product.description || "Premium fitness product."
            )}
          </p>
          <div class="product-footer">
            <div class="product-price">
              ₹${price.toFixed(2)}
            </div>
            <div class="product-stock ${
              stock > 0 ? "in-stock" : "out-of-stock"
            }">
              ${stock > 0 ? `${stock} in stock` : "Out of stock"}
            </div>
          </div>
          <div class="fitcart-product-actions">
            <button
              type="button"
              class="fitcart-add-button"
              ${stock <= 0 ? "disabled" : ""}
              onclick="addToCart(${productId})">
              Add to Cart
            </button>
            <button
              type="button"
              class="btn btn-secondary fitcart-wishlist-button"
              aria-label="Add ${productName} to wishlist"
              title="Add to wishlist"
              onclick="addToWishlist(${productId})">
              ♡
            </button>
          </div>
        </div>
      </article>`;
  }).join("");
}

function renderFitcartProductGrid() {
  const grid = document.getElementById("productGrid");
  if (!grid) return;
  const sortMode =
    document.getElementById("fitcartSort")?.value || "featured";
  const sortedProducts = [...products];
  if (sortMode === "price-low") {
    sortedProducts.sort(
      (a, b) => Number(a.price) - Number(b.price)
    );
  }
  if (sortMode === "price-high") {
    sortedProducts.sort(
      (a, b) => Number(b.price) - Number(a.price)
    );
  }
  if (sortMode === "name") {
    sortedProducts.sort((a, b) =>
      String(a.productName || "").localeCompare(
        String(b.productName || "")
      )
    );
  }
  grid.innerHTML = renderProductCards(sortedProducts);
  const count = document.getElementById("productResultCount");
  if (count) {
    count.textContent = String(sortedProducts.length);
  }
  syncFitcartCategoryChips();
}

async function handleSearch() {
  const keyword =
    document.getElementById("searchInput")?.value || "";
  const categoryId =
    document.getElementById("categoryFilter")?.value || "";
  await loadProducts(keyword, categoryId);
  renderFitcartProductGrid();
}

async function handleCategoryFilter(categoryId) {
  const keyword =
    document.getElementById("searchInput")?.value || "";
  await loadProducts(keyword, categoryId);
  renderFitcartProductGrid();
}

// ==================== CART ====================

async function updateCartBadge() {
  if (currentRole !== "BUYER" || !currentUserId) return;
  try {
    const response = await apiFetch(`/cart/${currentUserId}`);
    if (response.ok) {
      cart = await response.json();
      const count = cart.reduce(
        (total, item) => total + Number(item.quantity || 0),
        0
      );
      const badge = document.getElementById("cartCount");
      if (badge) {
        badge.innerText = count;
      }
    }
  } catch (error) {
    console.error("Could not update cart:", error);
  }
}

async function addToCart(productId) {
  if (currentRole !== "BUYER") {
    showToast("Only buyers can add products to their cart.");
    return;
  }
  try {
    const response = await apiFetch("/cart", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        userId: currentUserId,
        productId,
        quantity: 1
      })
    });
    if (response.ok) {
      showToast("Added item to cart!");
      await updateCartBadge();
    } else {
      const error = await readError(response);
      showToast(error.message || "Failed to add to cart");
    }
  } catch (error) {
    console.error("Add to cart failed:", error);
    showToast("Network error");
  }
}

async function openCartModal() {
  if (currentRole !== "BUYER") return;
  await updateCartBadge();
  const list = document.getElementById("cartItemsList");
  let total = 0;
  if (cart.length === 0) {
    list.innerHTML = `
      <p style="color:var(--text-muted);text-align:center;padding:2rem">
        Your cart is currently empty.
      </p>`;
    document.getElementById("cartTotal").innerText = "0.00";
  } else {
    list.innerHTML = cart.map((item) => {
      total += Number(item.price) * Number(item.quantity);
      return `
        <div style="
          display:flex;
          justify-content:space-between;
          align-items:center;
          gap:1rem;
          padding:.75rem 0;
          border-bottom:1px solid var(--border);
          flex-wrap:wrap">
          <div>
            <strong>${escapeHtml(item.productName)}</strong>
            <div style="font-size:.8rem;color:var(--text-muted)">
              ₹${Number(item.price).toFixed(2)} each
            </div>
          </div>
          <div style="display:flex;align-items:center;gap:.5rem">
            <span>Qty: ${item.quantity}</span>
            <button class="btn btn-secondary btn-sm"
                    onclick="changeCartQty(
                      ${item.cartId},
                      ${Number(item.quantity) - 1}
                    )">−</button>
            <button class="btn btn-secondary btn-sm"
                    onclick="changeCartQty(
                      ${item.cartId},
                      ${Number(item.quantity) + 1}
                    )">+</button>
            <button class="btn btn-secondary btn-sm"
                    onclick="removeCartItem(${item.cartId})"
                    aria-label="Remove item"
                    style="color:var(--danger)">
              &times;
            </button>
          </div>
        </div>`;
    }).join("");
    document.getElementById("cartTotal").innerText =
      total.toFixed(2);
  }
  document.getElementById("cartModal").classList.add("active");
}

function closeCartModal() {
  document.getElementById("cartModal").classList.remove("active");
}

async function changeCartQty(cartId, quantity) {
  if (currentRole !== "BUYER") return;
  if (quantity <= 0) {
    return removeCartItem(cartId);
  }
  const response = await apiFetch(
    `/cart/${cartId}?quantity=${quantity}`,
    { method: "PUT" }
  );
  if (!response.ok) {
    showToast("Failed to update cart quantity.");
  }
  await openCartModal();
}

async function removeCartItem(cartId) {
  if (currentRole !== "BUYER") return;
  const response = await apiFetch(`/cart/${cartId}`, {
    method: "DELETE"
  });
  if (!response.ok) {
    showToast("Failed to remove cart item.");
  }
  await openCartModal();
  await updateCartBadge();
}

async function clearUserCart() {
  if (currentRole !== "BUYER") return;
  const response = await apiFetch(
    `/cart/user/${currentUserId}`,
    { method: "DELETE" }
  );
  if (!response.ok) {
    showToast("Failed to clear cart.");
  }
  await openCartModal();
  await updateCartBadge();
}

// ==================== WISHLIST ====================

async function updateWishlistBadge() {
  if (currentRole !== "BUYER" || !currentUserId) return;
  try {
    const response = await apiFetch(
      `/wishlist/${currentUserId}`
    );
    if (response.ok) {
      wishlist = await response.json();
      const badge = document.getElementById("wishlistCount");
      if (badge) {
        badge.innerText = wishlist.length;
      }
    }
  } catch (error) {
    console.error("Could not update wishlist:", error);
  }
}

async function addToWishlist(productId) {
  if (currentRole !== "BUYER") return;
  try {
    const response = await apiFetch("/wishlist", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        userId: currentUserId,
        productId
      })
    });
    if (response.ok) {
      showToast("Added to wishlist!");
      await updateWishlistBadge();
    } else {
      const error = await readError(response);
      showToast(error.message || "Failed to add to wishlist.");
    }
  } catch (error) {
    console.error("Wishlist update failed:", error);
    showToast("Error updating wishlist");
  }
}

async function openWishlistModal() {
  if (currentRole !== "BUYER") return;
  await updateWishlistBadge();
  const list = document.getElementById("wishlistItemsList");
  if (wishlist.length === 0) {
    list.innerHTML = `
      <p style="color:var(--text-muted);text-align:center;padding:2rem">
        No items in wishlist.
      </p>`;
  } else {
    list.innerHTML = wishlist.map((item) => `
      <div style="
        display:flex;
        justify-content:space-between;
        align-items:center;
        gap:1rem;
        padding:.75rem 0;
        border-bottom:1px solid var(--border);
        flex-wrap:wrap">
        <div>
          <strong>${escapeHtml(item.productName)}</strong>
          <div style="font-size:.8rem;color:var(--text-muted)">
            ₹${Number(item.price).toFixed(2)}
          </div>
        </div>
        <div style="display:flex;gap:.5rem">
          <button class="btn btn-primary btn-sm"
                  onclick="addToCart(${item.productId})">
            Move to Cart
          </button>
          <button class="btn btn-secondary btn-sm"
                  onclick="removeWishlistItem(${item.wishlistId})"
                  aria-label="Remove wishlist item">
            &times;
          </button>
        </div>
      </div>
    `).join("");
  }
  document.getElementById("wishlistModal").classList.add("active");
}

function closeWishlistModal() {
  document.getElementById("wishlistModal").classList.remove("active");
}

async function removeWishlistItem(wishlistId) {
  if (currentRole !== "BUYER") return;
  const response = await apiFetch(`/wishlist/${wishlistId}`, {
    method: "DELETE"
  });
  if (!response.ok) {
    showToast("Failed to remove wishlist item.");
  }
  await openWishlistModal();
  await updateWishlistBadge();
}

// ==================== CHECKOUT ====================

function openCheckoutModal() {
  if (currentRole !== "BUYER") return;
  if (cart.length === 0) {
    showToast("Cart is empty!");
    return;
  }
  closeCartModal();
  document.getElementById("checkoutModal").classList.add("active");
}

function closeCheckoutModal() {
  document.getElementById("checkoutModal").classList.remove("active");
}

async function confirmOrderCheckout() {
  if (currentRole !== "BUYER") return;
  const method =
    document.getElementById("checkoutPaymentMethod").value;
  try {
    const response = await apiFetch("/orders", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        userId: currentUserId,
        useCart: true,
        paymentMethod: method
      })
    });
    if (response.ok) {
      const order = await response.json();
      closeCheckoutModal();
      showToast(
        `Order #${order.orderId} placed successfully!`
      );
      await updateCartBadge();
      await renderBuyerOrders();
    } else {
      const error = await readError(response);
      showToast(
        error.message || "Failed to place order"
      );
    }
  } catch (error) {
    console.error("Checkout failed:", error);
    showToast("Checkout failed");
  }
}

// ==================== BUYER ORDERS ====================

async function renderBuyerOrders() {
  if (currentRole !== "BUYER") return;
  const main = document.getElementById("mainContent");
  main.innerHTML = `
    <h2 style="margin-bottom:1.5rem">My Order History</h2>
    <p>Loading orders...</p>`;
  try {
    const response = await apiFetch(
      `/orders/user/${currentUserId}`
    );
    if (!response.ok) {
      throw new Error("Unable to load orders.");
    }
    const orders = await response.json();
    if (orders.length === 0) {
      main.innerHTML = `
        <h2 style="margin-bottom:1.5rem">My Order History</h2>
        <p style="color:var(--text-muted)">
          You have not placed any orders yet.
        </p>`;
      return;
    }
    main.innerHTML = `
      <h2 style="margin-bottom:1.5rem">My Order History</h2>
      ${orders.map((order) => `
        <div style="
          background:var(--bg-card);
          border:1px solid var(--border);
          border-radius:var(--radius);
          padding:1.5rem;
          margin-bottom:1.5rem">
          <div style="
            display:flex;
            justify-content:space-between;
            flex-wrap:wrap;
            gap:.5rem;
            border-bottom:1px solid var(--border);
            padding-bottom:.75rem;
            margin-bottom:1rem">
            <div>
              <strong>Order #${order.orderId}</strong>
              — ${escapeHtml(order.orderDate || "")}
            </div>
            <div>
              <span class="badge">
                ${escapeHtml(order.status)}
              </span>
              <strong style="margin-left:1rem">
                Total: ₹${Number(order.totalAmount).toFixed(2)}
              </strong>
            </div>
          </div>
          <div>
            ${(order.items || []).map((item) => `
              <div style="
                display:flex;
                justify-content:space-between;
                gap:1rem;
                font-size:.9rem;
                padding:.3rem 0">
                <span>
                  ${escapeHtml(item.productName)}
                  &times; ${item.quantity}
                </span>
                <span>
                  ₹${Number(
                    Number(item.price) * Number(item.quantity)
                  ).toFixed(2)}
                </span>
              </div>
            `).join("")}
          </div>
          <div style="
            margin-top:1rem;
            font-size:.8rem;
            color:var(--text-muted)">
            Payment:
            ${order.payment
              ? `${escapeHtml(order.payment.paymentMethod)}
                 (${escapeHtml(order.payment.paymentStatus)}
                 - ${escapeHtml(order.payment.transactionId)})`
              : "Simulated"}
          </div>
        </div>
      `).join("")}`;
  } catch (error) {
    console.error("Failed to load order history:", error);
    main.innerHTML = `
      <p style="color:var(--danger)">
        Failed to load order history.
      </p>`;
  }
}

// ==================== SELLER PRODUCTS ====================

async function renderSellerProducts() {
  if (currentRole !== "SELLER") return;
  const main = document.getElementById("mainContent");
  main.innerHTML = `
    <h2 style="margin-bottom:1.5rem">
      My Products & Stock Management
    </h2>
    <p>Loading products...</p>`;
  try {
    const response = await apiFetch(
      `/products/seller/${currentSellerId}`
    );
    if (!response.ok) {
      throw new Error("Unable to load seller products.");
    }
    const sellerProducts = await response.json();
    main.innerHTML = `
      <div style="
        display:flex;
        justify-content:space-between;
        align-items:center;
        gap:1rem;
        margin-bottom:1.5rem;
        flex-wrap:wrap">
        <h2>My Listed Products</h2>
        <button class="btn btn-primary"
                onclick="setTab(
                  document.querySelectorAll('.tab-btn')[1],
                  renderSellerAddProduct
                )">
          + Add New Product
        </button>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>Product Name</th>
            <th>Category</th>
            <th>Price</th>
            <th>Current Stock</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          ${sellerProducts.length === 0
            ? `
              <tr>
                <td colspan="5"
                    style="text-align:center;color:var(--text-muted)">
                  No products listed yet.
                </td>
              </tr>`
            : sellerProducts.map((product) => `
              <tr>
                <td>
                  <strong>${escapeHtml(product.productName)}</strong>
                </td>
                <td>
                  ${escapeHtml(product.categoryName || "General")}
                </td>
                <td>₹${Number(product.price).toFixed(2)}</td>
                <td>
                  <input
                    type="number"
                    id="stock_${product.productId}"
                    value="${product.stockQuantity}"
                    min="0"
                    style="
                      width:80px;
                      padding:.45rem;
                      background:#0f172a;
                      color:#fff;
                      border:1px solid var(--border);
                      border-radius:6px">
                </td>
                <td>
                  <button class="btn btn-primary btn-sm"
                          onclick="updateProductStock(${product.productId})">
                    Save Stock
                  </button>
                </td>
              </tr>
            `).join("")}
        </tbody>
      </table>`;
  } catch (error) {
    console.error("Could not load seller products:", error);
    main.innerHTML = `
      <p style="color:var(--danger)">
        Failed to load seller products. Check seller permissions.
      </p>`;
  }
}

function renderSellerAddProduct() {
  if (currentRole !== "SELLER") return;
  const main = document.getElementById("mainContent");
  const categoryOptions = categories.map((category) => `
    <option value="${category.categoryId}">
      ${escapeHtml(category.categoryName)}
    </option>
  `).join("");
  main.innerHTML = `
    <h2 style="margin-bottom:1.5rem">
      Add New Fitness Product
    </h2>
    <div class="form-card">
      <form onsubmit="handleCreateProduct(event)">
        <div class="form-group">
          <label for="newProdName">Product Name</label>
          <input
            type="text"
            id="newProdName"
            class="form-control"
            required
            placeholder="e.g. Whey Protein Isolate 1kg">
        </div>
        <div class="form-group">
          <label for="newProdCategory">Category</label>
          <select
            id="newProdCategory"
            class="form-control"
            required>
            ${categoryOptions}
          </select>
        </div>
        <div class="form-group">
          <label for="newProdDesc">Description</label>
          <textarea
            id="newProdDesc"
            class="form-control"
            rows="3"
            placeholder="Fitness product specifications, nutrition, etc.">
          </textarea>
        </div>
        <div class="form-group">
          <label for="newProdPrice">Price (₹)</label>
          <input
            type="number"
            step="0.01"
            min="0.01"
            id="newProdPrice"
            class="form-control"
            required
            placeholder="499.00">
        </div>
        <div class="form-group">
          <label for="newProdStock">Initial Stock Quantity</label>
          <input
            type="number"
            id="newProdStock"
            class="form-control"
            required
            min="0"
            value="25">
        </div>
        <button type="submit" class="btn btn-primary">
          Publish Product
        </button>
      </form>
    </div>`;
}

async function handleCreateProduct(event) {
  event.preventDefault();
  if (currentRole !== "SELLER") return;
  const name =
    document.getElementById("newProdName").value.trim();
  const categoryId =
    document.getElementById("newProdCategory").value;
  const description =
    document.getElementById("newProdDesc").value;
  const price =
    document.getElementById("newProdPrice").value;
  const stock =
    document.getElementById("newProdStock").value;
  try {
    const response = await apiFetch("/products", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        productName: name,
        description,
        price: Number(price),
        categoryId: Number(categoryId),
        sellerId: currentSellerId,
        initialStock: Number(stock)
      })
    });
    if (response.ok) {
      showToast("Product added successfully!");
      await renderSellerProducts();
    } else {
      const error = await readError(response);
      showToast(
        error.message || "Failed to create product"
      );
    }
  } catch (error) {
    console.error("Error creating product:", error);
    showToast("Error creating product");
  }
}

async function updateProductStock(productId) {
  if (currentRole !== "SELLER") return;
  const stockInput =
    document.getElementById(`stock_${productId}`);
  const newStock = Number(stockInput?.value);
  if (!Number.isInteger(newStock) || newStock < 0) {
    showToast("Enter a valid stock quantity.");
    return;
  }
  try {
    const response = await apiFetch(
      `/inventory/product/${productId}`,
      {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          stockQuantity: newStock
        })
      }
    );
    if (response.ok) {
      showToast("Stock updated!");
    } else {
      showToast(
        "Failed to update stock. Check seller permissions."
      );
    }
  } catch (error) {
    console.error("Stock update failed:", error);
    showToast("Error updating stock");
  }
}

// ==================== SELLER ORDERS ====================

async function renderSellerOrders() {
  if (currentRole !== "SELLER") return;
  const main = document.getElementById("mainContent");
  main.innerHTML = `
    <h2 style="margin-bottom:1.5rem">
      Customer Orders for Your Products
    </h2>
    <p>Loading...</p>`;
  try {
    const response = await apiFetch(
      `/orders/seller/${currentSellerId}`
    );
    if (!response.ok) {
      throw new Error("Unable to load seller orders.");
    }
    const orders = await response.json();
    main.innerHTML = `
      <h2 style="margin-bottom:1.5rem">
        Customer Orders for Your Products
      </h2>
      <table class="data-table">
        <thead>
          <tr>
            <th>Order #</th>
            <th>Customer</th>
            <th>Date</th>
            <th>Status</th>
            <th>Change Status</th>
          </tr>
        </thead>
        <tbody>
          ${orders.length === 0
            ? `
              <tr>
                <td colspan="5"
                    style="text-align:center;color:var(--text-muted)">
                  No orders for your products yet.
                </td>
              </tr>`
            : orders.map((order) => `
              <tr>
                <td>#${order.orderId}</td>
                <td>
                  ${escapeHtml(order.customerName || "Customer")}
                </td>
                <td>${escapeHtml(order.orderDate || "")}</td>
                <td>
                  <span class="badge">
                    ${escapeHtml(order.status)}
                  </span>
                </td>
                <td>
                  <select
                    aria-label="Change order status"
                    onchange="updateOrderStatus(
                      ${order.orderId},
                      this.value
                    )"
                    style="
                      background:#0f172a;
                      color:#fff;
                      padding:.4rem;
                      border:1px solid var(--border);
                      border-radius:6px">
                    <option value="CONFIRMED"
                      ${order.status === "CONFIRMED" ? "selected" : ""}>
                      CONFIRMED
                    </option>
                    <option value="SHIPPED"
                      ${order.status === "SHIPPED" ? "selected" : ""}>
                      SHIPPED
                    </option>
                    <option value="DELIVERED"
                      ${order.status === "DELIVERED" ? "selected" : ""}>
                      DELIVERED
                    </option>
                    <option value="CANCELLED"
                      ${order.status === "CANCELLED" ? "selected" : ""}>
                      CANCELLED
                    </option>
                  </select>
                </td>
              </tr>
            `).join("")}
        </tbody>
      </table>`;
  } catch (error) {
    console.error("Could not load seller orders:", error);
    main.innerHTML = `
      <p style="color:var(--danger)">
        Failed to load orders. Check seller permissions.
      </p>`;
  }
}

async function updateOrderStatus(orderId, status) {
  if (!["SELLER", "ADMIN"].includes(currentRole)) return;
  try {
    const response = await apiFetch(
      `/orders/${orderId}/status`,
      {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ status })
      }
    );
    if (response.ok) {
      showToast(
        `Order #${orderId} status updated to ${status}`
      );
      if (currentRole === "SELLER") {
        await renderSellerOrders();
      } else {
        await renderAdminOrders();
      }
    } else {
      showToast(
        "Failed to update status. Check permissions."
      );
    }
  } catch (error) {
    console.error("Error updating order status:", error);
    showToast("Error updating status");
  }
}

// ==================== ADMIN CATEGORIES ====================

async function renderAdminCategories() {
  if (currentRole !== "ADMIN") return;
  await loadCategories();
  const main = document.getElementById("mainContent");
  main.innerHTML = `
    <h2 style="margin-bottom:1.5rem">Category Management</h2>
    <div class="form-card">
      <form
        onsubmit="handleCreateCategory(event)"
        style="
          display:flex;
          gap:.5rem;
          flex-wrap:wrap">
        <input
          type="text"
          id="newCatName"
          class="form-control"
          placeholder="New category name (e.g. Gym Apparel)"
          required>
        <button type="submit" class="btn btn-primary">
          Add Category
        </button>
      </form>
    </div>
    <table class="data-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Category Name</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        ${categories.length === 0
          ? `
            <tr>
              <td colspan="3"
                  style="text-align:center;color:var(--text-muted)">
                No categories defined yet.
              </td>
            </tr>`
          : categories.map((category) => `
            <tr>
              <td>#${category.categoryId}</td>
              <td>
                <strong>
                  ${escapeHtml(category.categoryName)}
                </strong>
              </td>
              <td>
                <button
                  class="btn btn-secondary btn-sm"
                  style="color:var(--danger)"
                  onclick="deleteCategory(${category.categoryId})">
                  Delete
                </button>
              </td>
            </tr>
          `).join("")}
      </tbody>
    </table>`;
}

async function handleCreateCategory(event) {
  event.preventDefault();
  if (currentRole !== "ADMIN") return;
  const name =
    document.getElementById("newCatName").value.trim();
  if (!name) {
    showToast("Enter a category name.");
    return;
  }
  try {
    const response = await apiFetch("/categories", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        categoryName: name
      })
    });
    if (response.ok) {
      showToast("Category created!");
      await renderAdminCategories();
    } else {
      const error = await readError(response);
      showToast(
        error.message || "Failed to create category"
      );
    }
  } catch (error) {
    console.error("Error creating category:", error);
    showToast("Error creating category");
  }
}

async function deleteCategory(categoryId) {
  if (currentRole !== "ADMIN") return;
  if (!confirm("Are you sure you want to delete this category?")) {
    return;
  }
  try {
    const response = await apiFetch(
      `/categories/${categoryId}`,
      { method: "DELETE" }
    );
    if (response.ok) {
      showToast("Category deleted");
      await renderAdminCategories();
    } else {
      showToast("Failed to delete category");
    }
  } catch (error) {
    console.error("Error deleting category:", error);
    showToast("Error deleting category");
  }
}

// ==================== ADMIN ORDERS ====================

async function renderAdminOrders() {
  if (currentRole !== "ADMIN") return;
  const main = document.getElementById("mainContent");
  main.innerHTML = `
    <h2 style="margin-bottom:1.5rem">
      Platform Orders (Admin View)
    </h2>
    <p>Loading...</p>`;
  try {
    const response = await apiFetch("/orders");
    if (!response.ok) {
      throw new Error("Unable to load orders.");
    }
    const orders = await response.json();
    main.innerHTML = `
      <h2 style="margin-bottom:1.5rem">
        Platform Orders (Admin View)
      </h2>
      <table class="data-table">
        <thead>
          <tr>
            <th>Order #</th>
            <th>Customer</th>
            <th>Date</th>
            <th>Total</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          ${orders.length === 0
            ? `
              <tr>
                <td colspan="5"
                    style="text-align:center;color:var(--text-muted)">
                  No orders recorded on platform yet.
                </td>
              </tr>`
            : orders.map((order) => `
              <tr>
                <td>#${order.orderId}</td>
                <td>
                  ${escapeHtml(order.customerName || "Customer")}
                </td>
                <td>${escapeHtml(order.orderDate || "")}</td>
                <td>
                  ₹${Number(order.totalAmount).toFixed(2)}
                </td>
                <td>
                  <span class="badge">
                    ${escapeHtml(order.status)}
                  </span>
                </td>
              </tr>
            `).join("")}
        </tbody>
      </table>`;
  } catch (error) {
    console.error("Could not load platform orders:", error);
    main.innerHTML = `
      <p style="color:var(--danger)">
        Failed to load platform orders.
      </p>`;
  }
}

// ==================== ADMIN USERS ====================

async function renderAdminUsers() {
  if (currentRole !== "ADMIN") return;
  const main = document.getElementById("mainContent");
  main.innerHTML = `
    <h2 style="margin-bottom:1.5rem">Platform Users</h2>
    <p>Loading...</p>`;
  try {
    const response = await apiFetch("/users");
    if (!response.ok) {
      throw new Error("Unable to load users.");
    }
    const users = await response.json();
    main.innerHTML = `
      <h2 style="margin-bottom:1.5rem">Platform Users</h2>
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Phone</th>
          </tr>
        </thead>
        <tbody>
          ${users.length === 0
            ? `
              <tr>
                <td colspan="5"
                    style="text-align:center;color:var(--text-muted)">
                  No users found.
                </td>
              </tr>`
            : users.map((user) => `
              <tr>
                <td>#${user.userId}</td>
                <td>${escapeHtml(user.name)}</td>
                <td>${escapeHtml(user.email)}</td>
                <td>
                  <span class="badge"
                        style="background:${
                          user.role === "ADMIN"
                            ? "var(--danger)"
                            : user.role === "SELLER"
                              ? "var(--accent)"
                              : "var(--primary)"
                        }">
                    ${escapeHtml(user.role)}
                  </span>
                </td>
                <td>${escapeHtml(user.phone || "—")}</td>
              </tr>
            `).join("")}
        </tbody>
      </table>`;
  } catch (error) {
    console.error("Could not load platform users:", error);
    main.innerHTML = `
      <p style="color:var(--danger)">
        Failed to load platform users.
      </p>`;
  }
}

// ==================== ERROR HANDLING ====================

async function readError(response) {
  try {
    const data = await response.json();
    return data && typeof data === "object"
      ? data
      : { message: "Request failed." };
  } catch (error) {
    return {
      message: `Request failed (HTTP ${response.status}).`
    };
  }
}

// ==================== HTML ESCAPING ====================

function escapeHtml(value) {
  if (value === null || value === undefined) {
    return "";
  }
  return String(value).replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[character]);
}

/* ===== FITCART FITFINDER FEATURE ===== */
(function initFitCartFitFinder() {
  const STYLE_ID = "fitcart-fitfinder-styles";

  function addFitFinderStyles() {
    if (document.getElementById(STYLE_ID)) return;

    const style = document.createElement("style");
    style.id = STYLE_ID;
    style.textContent = `
      .fitfinder-panel {
        margin: 28px 0 38px;
        padding: 25px;
        border: 1px solid #384432;
        border-radius: 20px;
        background:
          radial-gradient(circle at 100% 0%, #c8f16912, transparent 45%),
          linear-gradient(145deg, #192018, #101410);
      }

      .fitfinder-heading {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 16px;
        margin-bottom: 20px;
      }

      .fitfinder-kicker {
        color: #c8f169;
        font-size: .68rem;
        font-weight: 900;
        letter-spacing: 2px;
      }

      .fitfinder-heading h2 {
        margin: 8px 0;
        font-size: clamp(1.45rem, 3vw, 2rem);
        letter-spacing: -.5px;
      }

      .fitfinder-heading p {
        margin: 0;
        color: #a1aba0;
        font-size: .9rem;
        line-height: 1.6;
      }

      .fitfinder-tag {
        flex-shrink: 0;
        padding: 7px 10px;
        border: 1px solid #45513a;
        border-radius: 999px;
        color: #c8f169;
        font-size: .65rem;
        font-weight: 900;
        letter-spacing: 1px;
      }

      .fitfinder-options {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 13px;
      }

      .fitfinder-option {
        display: grid;
        grid-template-columns: 47px minmax(0, 1fr) 16px;
        grid-template-rows: auto auto;
        align-items: center;
        gap: 5px 12px;
        min-width: 0;
        padding: 17px;
        text-align: left;
        border: 1px solid #303930;
        border-radius: 15px;
        background: #121712;
        color: #f4f7f0;
        cursor: pointer;
        transition: border-color .2s, transform .2s, background .2s;
      }

      .fitfinder-option:hover,
      .fitfinder-option:focus-visible {
        transform: translateY(-3px);
        border-color: #c8f169;
        background: #1b2417;
        outline: none;
      }

      .fitfinder-icon {
        grid-row: 1 / 3;
        display: grid;
        place-items: center;
        width: 45px;
        height: 45px;
        border-radius: 12px;
        background: #c8f16917;
        font-size: 1.6rem;
      }

      .fitfinder-title {
        font-weight: 900;
        font-size: .95rem;
      }

      .fitfinder-description {
        color: #a1aba0;
        font-size: .75rem;
        line-height: 1.5;
      }

      .fitfinder-arrow {
        grid-column: 3;
        grid-row: 1 / 3;
        color: #c8f169;
        font-size: 1.2rem;
      }

      @media (max-width: 760px) {
        .fitfinder-options {
          grid-template-columns: 1fr;
        }

        .fitfinder-panel {
          padding: 18px;
        }
      }

      @media (max-width: 420px) {
        .fitfinder-heading {
          flex-direction: column;
        }
      }
    `;

    document.head.appendChild(style);
  }

  function chooseGoal(goal) {
    const priorities = {
      strength: [
        /gym equipment/i,
        /accessories/i,
        /gym supplements/i
      ],
      performance: [
        /sports nutrition/i,
        /gym clothing/i,
        /gym supplements/i
      ],
      recovery: [
        /recovery\s*(?:&|and)?\s*wellness/i,
        /gym supplements/i,
        /sports nutrition/i
      ]
    };

    const availableCategories =
      Array.isArray(categories) ? categories : [];

    const patterns = priorities[goal] || [];
    let chosenCategory = null;

    for (const pattern of patterns) {
      chosenCategory = availableCategories.find(category =>
        pattern.test(String(category.categoryName || ""))
      );

      if (chosenCategory) break;
    }

    const catalog = document.getElementById("fitcartCatalog");
    catalog?.scrollIntoView({
      behavior: "smooth",
      block: "start"
    });

    if (chosenCategory) {
      const categoryId = String(chosenCategory.categoryId);

      if (typeof chooseFitcartCategory === "function") {
        chooseFitcartCategory(categoryId);
      }
      return;
    }

    if (typeof chooseFitcartCategory === "function") {
      chooseFitcartCategory("");
    }

    if (typeof showToast === "function") {
      showToast("Showing all products for your fitness journey.");
    }
  }

  function renderFitFinder() {
    const main = document.getElementById("mainContent");

    if (
      !main ||
      !main.querySelector("#fitcartCatalog") ||
      main.querySelector("#fitfinderPanel")
    ) {
      return;
    }

    const benefits = main.querySelector(".fitcart-benefits");
    if (!benefits) return;

    const panel = document.createElement("section");
    panel.id = "fitfinderPanel";
    panel.className = "fitfinder-panel";
    panel.setAttribute("aria-labelledby", "fitfinderTitle");

    panel.innerHTML = `
      <div class="fitfinder-heading">
        <div>
          <span class="fitfinder-kicker">YOUR GOAL. YOUR GEAR.</span>
          <h2 id="fitfinderTitle">Find your fit.</h2>
          <p>Choose your training focus and explore the right category.</p>
        </div>
        <span class="fitfinder-tag">FITFINDER / 01</span>
      </div>

      <div class="fitfinder-options">
        <button type="button" class="fitfinder-option"
          data-fitfinder-goal="strength">
          <span class="fitfinder-icon">🏋️</span>
          <span class="fitfinder-title">Build Strength</span>
          <span class="fitfinder-description">
            Equipment and strength essentials
          </span>
          <span class="fitfinder-arrow" aria-hidden="true">↗</span>
        </button>

        <button type="button" class="fitfinder-option"
          data-fitfinder-goal="performance">
          <span class="fitfinder-icon">⚡</span>
          <span class="fitfinder-title">Boost Performance</span>
          <span class="fitfinder-description">
            Nutrition and training gear
          </span>
          <span class="fitfinder-arrow" aria-hidden="true">↗</span>
        </button>

        <button type="button" class="fitfinder-option"
          data-fitfinder-goal="recovery">
          <span class="fitfinder-icon">💪</span>
          <span class="fitfinder-title">Focus on Recovery</span>
          <span class="fitfinder-description">
            Recovery and wellness essentials
          </span>
          <span class="fitfinder-arrow" aria-hidden="true">↗</span>
        </button>
      </div>
    `;

    panel.querySelectorAll("[data-fitfinder-goal]").forEach(button => {
      button.addEventListener("click", () => {
        chooseGoal(button.dataset.fitfinderGoal);
      });
    });

    benefits.insertAdjacentElement("afterend", panel);
  }

  function start() {
    addFitFinderStyles();

    const main = document.getElementById("mainContent");
    if (!main) return;

    const observer = new MutationObserver(renderFitFinder);
    observer.observe(main, {
      childList: true,
      subtree: true
    });

    renderFitFinder();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start, { once: true });
  } else {
    start();
  }
})();

/* ===== FITCART: BUILD YOUR FITNESS KIT ===== */
(function initFitCartKitBuilder() {
  const STYLE_ID = "fitcart-kit-builder-styles";
  const PANEL_ID = "fitcartKitBuilder";
  const selectedItems = new Map();

  let mainObserver = null;
  let gridObserver = null;
  let observedGrid = null;

  function safeText(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;"
    })[character]);
  }

  function money(value) {
    return "₹" + Number(value || 0).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });
  }

  function injectStyles() {
    if (document.getElementById(STYLE_ID)) return;

    const style = document.createElement("style");
    style.id = STYLE_ID;
    style.textContent = `
      #${PANEL_ID} {
        margin: 26px 0;
        padding: 24px;
        color: #f4f7f0;
        background:
          radial-gradient(ellipse at 100% 0%, #c8f16912, transparent 45%),
          #151a14;
        border: 1px solid #34402d;
        border-radius: 20px;
      }

      #${PANEL_ID} * { box-sizing: border-box; }

      .fitkit-kicker {
        color: #c8f169;
        font-size: 10px;
        font-weight: 900;
        letter-spacing: 2px;
      }

      .fitkit-heading {
        margin: 8px 0;
        font-size: clamp(22px, 3vw, 30px);
        letter-spacing: -0.7px;
      }

      .fitkit-subtitle {
        max-width: 600px;
        color: #a6afa3;
        font-size: 13px;
        line-height: 1.7;
      }

      .fitkit-limit {
        display: inline-block;
        margin-top: 13px;
        padding: 6px 10px;
        border: 1px solid #374332;
        border-radius: 30px;
        color: #c8f169;
        font-size: 11px;
        font-weight: 800;
      }

      .fitkit-products {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 12px;
        margin-top: 22px;
      }

      .fitkit-product {
        min-width: 0;
        padding: 15px;
        background: #101410;
        border: 1px solid #30392d;
        border-radius: 15px;
        transition: border-color .2s, transform .2s;
      }

      .fitkit-product.is-selected {
        border-color: #c8f169;
        box-shadow: inset 0 0 0 1px #c8f16930;
      }

      .fitkit-product-icon {
        display: grid;
        place-items: center;
        width: 43px;
        height: 43px;
        margin-bottom: 14px;
        background: #c8f16917;
        border-radius: 12px;
        font-size: 22px;
      }

      .fitkit-category {
        color: #a2ad9c;
        font-size: 10px;
        text-transform: uppercase;
        letter-spacing: 1px;
      }

      .fitkit-name {
        margin: 8px 0;
        font-size: 14px;
        line-height: 1.5;
        overflow-wrap: anywhere;
      }

      .fitkit-price {
        margin: 12px 0;
        color: #c8f169;
        font-size: 17px;
        font-weight: 900;
      }

      .fitkit-select {
        width: 100%;
        padding: 10px 8px;
        border: 1px solid #46513e;
        border-radius: 9px;
        background: #1c2419;
        color: #f4f7f0;
        font-weight: 800;
        font-size: 12px;
        cursor: pointer;
      }

      .fitkit-select:hover:not(:disabled) {
        border-color: #c8f169;
      }

      .fitkit-select.selected {
        background: #c8f169;
        color: #13170f;
        border-color: #c8f169;
      }

      .fitkit-select:disabled {
        cursor: not-allowed;
        opacity: .5;
      }

      .fitkit-summary {
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 18px;
        margin-top: 20px;
        padding-top: 19px;
        border-top: 1px solid #30392d;
      }

      .fitkit-summary-label {
        color: #a6afa3;
        font-size: 11px;
        letter-spacing: 1px;
      }

      .fitkit-total {
        margin-top: 6px;
        color: #c8f169;
        font-size: 25px;
        font-weight: 900;
      }

      .fitkit-count {
        margin-top: 5px;
        color: #a6afa3;
        font-size: 12px;
      }

      .fitkit-add {
        min-height: 43px;
        padding: 12px 19px;
        background: #c8f169;
        border: 0;
        border-radius: 10px;
        color: #14180f;
        font-weight: 900;
        cursor: pointer;
      }

      .fitkit-add:hover:not(:disabled) {
        filter: brightness(1.06);
        transform: translateY(-1px);
      }

      .fitkit-add:disabled {
        opacity: .4;
        cursor: not-allowed;
        transform: none;
      }

      .fitkit-empty {
        grid-column: 1 / -1;
        padding: 20px;
        color: #a6afa3;
        border: 1px dashed #384331;
        border-radius: 12px;
        font-size: 13px;
      }

      @media (max-width: 680px) {
        .fitkit-products { grid-template-columns: repeat(2, minmax(0, 1fr)); }
        #${PANEL_ID} { padding: 17px; }
      }

      @media (max-width: 420px) {
        .fitkit-products { grid-template-columns: 1fr; }
        .fitkit-add { width: 100%; }
      }
    `;

    document.head.appendChild(style);
  }

  function currentProducts() {
    return Array.isArray(products) ? products : [];
  }

  function iconFor(product) {
    const description =
      `${product.productName || ""} ${product.categoryName || ""}`.toLowerCase();

    if (/protein|whey|creatine|supplement|nutrition/.test(description)) return "🥤";
    if (/dumbbell|weight|barbell|kettlebell|equipment/.test(description)) return "🏋️";
    if (/glove|boxing|wrap/.test(description)) return "🥊";
    if (/band|resistance/.test(description)) return "💪";
    if (/mat|yoga|stretch/.test(description)) return "🧘";
    if (/bottle|shaker/.test(description)) return "🚰";

    return "⚡";
  }

  function renderPanel() {
    const panel = document.getElementById(PANEL_ID);
    if (!panel) return;

    const list = currentProducts().filter(product =>
      Number(product.productId) > 0 &&
      Number(product.stockQuantity ?? 0) > 0
    );

    const selected = [...selectedItems.values()];
    const total = selected.reduce(
      (sum, product) => sum + Math.max(0, Number(product.price) || 0),
      0
    );

    panel.innerHTML = `
      <span class="fitkit-kicker">CREATE YOUR OWN COMBINATION</span>
      <h2 class="fitkit-heading">Build Your Fitness Kit.</h2>
      <p class="fitkit-subtitle">
        Choose the essentials that match your routine. Select up to three
        products and add your complete selection to your cart.
      </p>
      <span class="fitkit-limit">UP TO 3 PRODUCTS · YOUR CHOICE</span>

      <div class="fitkit-products">
        ${
          list.length
            ? list.slice(0, 6).map(product => {
                const id = Number(product.productId);
                const isSelected = selectedItems.has(id);
                const atLimit = selectedItems.size >= 3 && !isSelected;

                return `
                  <article class="fitkit-product ${isSelected ? "is-selected" : ""}">
                    <div class="fitkit-product-icon">${iconFor(product)}</div>
                    <div class="fitkit-category">
                      ${safeText(product.categoryName || "Fitness Gear")}
                    </div>
                    <h3 class="fitkit-name">
                      ${safeText(product.productName || "Fitness Product")}
                    </h3>
                    <div class="fitkit-price">${money(product.price)}</div>
                    <button type="button"
                      class="fitkit-select ${isSelected ? "selected" : ""}"
                      data-fitkit-select="${id}"
                      ${atLimit ? "disabled" : ""}>
                      ${
                        isSelected
                          ? "✓ Selected · Remove"
                          : atLimit
                            ? "Kit limit reached"
                            : "+ Add to kit"
                      }
                    </button>
                  </article>`;
              }).join("")
            : `<div class="fitkit-empty">
                No in-stock products are available to add to your kit right now.
              </div>`
        }
      </div>

      <div class="fitkit-summary">
        <div>
          <div class="fitkit-summary-label">YOUR KIT TOTAL</div>
          <div class="fitkit-total">${money(total)}</div>
          <div class="fitkit-count">
            ${selected.length} of 3 products selected
          </div>
        </div>
        <button type="button" class="fitkit-add" data-fitkit-add
          ${selected.length === 0 ? "disabled" : ""}>
          Add Kit to Cart →
        </button>
      </div>
    `;
  }

  function installPanel() {
    const main = document.getElementById("mainContent");
    if (!main) return;

    const catalog = main.querySelector("#fitcartCatalog");
    const benefits = main.querySelector(".fitcart-benefits");
    if (!catalog || !benefits) return;

    let panel = main.querySelector(`#${PANEL_ID}`);

    if (!panel) {
      panel = document.createElement("section");
      panel.id = PANEL_ID;
      panel.setAttribute("aria-label", "Build your fitness kit");

      const anchor = main.querySelector("#fitfinderPanel") || benefits;
      anchor.insertAdjacentElement("afterend", panel);

      panel.addEventListener("click", async event => {
        const selectButton = event.target.closest("[data-fitkit-select]");

        if (selectButton) {
          const productId = Number(selectButton.dataset.fitkitSelect);
          const product = currentProducts().find(
            item => Number(item.productId) === productId
          );

          if (!product) {
            if (typeof showToast === "function") {
              showToast("Please refresh the catalogue and try again.");
            }
            return;
          }

          if (selectedItems.has(productId)) {
            selectedItems.delete(productId);
          } else if (selectedItems.size >= 3) {
            if (typeof showToast === "function") {
              showToast("Your kit can contain up to three products.");
            }
            return;
          } else {
            selectedItems.set(productId, product);
          }

          renderPanel();
          return;
        }

        const addButton = event.target.closest("[data-fitkit-add]");
        if (!addButton) return;

        if (typeof addToCart !== "function") {
          if (typeof showToast === "function") {
            showToast("The cart is unavailable right now.");
          }
          return;
        }

        if (typeof currentRole !== "undefined" && currentRole !== "BUYER") {
          if (typeof showToast === "function") {
            showToast("Please sign in as a buyer to add products to your cart.");
          }
          return;
        }

        const chosenIds = [...selectedItems.keys()];
        if (!chosenIds.length) return;

        addButton.disabled = true;
        addButton.textContent = "Adding to cart…";

        try {
          for (const id of chosenIds) {
            await addToCart(id);
          }

          selectedItems.clear();
          renderPanel();

          if (typeof updateCartBadge === "function") {
            await updateCartBadge();
          }

          if (typeof showToast === "function") {
            showToast("Your fitness kit has been added to the cart.");
          }
        } catch (error) {
          console.error("FitCart kit add failed:", error);
          if (typeof showToast === "function") {
            showToast("Some items may not have been added. Please check your cart.");
          }
        } finally {
          renderPanel();
        }
      });
    }

    renderPanel();

    const grid = main.querySelector("#productGrid");
    if (grid && grid !== observedGrid) {
      if (gridObserver) gridObserver.disconnect();

      observedGrid = grid;
      gridObserver = new MutationObserver(() => {
        renderPanel();
      });

      gridObserver.observe(grid, { childList: true, subtree: true });
    }
  }

  function start() {
    injectStyles();

    const main = document.getElementById("mainContent");
    if (!main) return;

    installPanel();

    mainObserver = new MutationObserver(() => {
      if (!document.getElementById(PANEL_ID)) {
        installPanel();
      } else {
        const grid = main.querySelector("#productGrid");
        if (grid && grid !== observedGrid) installPanel();
      }
    });

    mainObserver.observe(main, {
      childList: true,
      subtree: true
    });
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start, { once: true });
  } else {
    start();
  }
})();