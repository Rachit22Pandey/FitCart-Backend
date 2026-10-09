
// FitCart Frontend Client
const API_BASE = "";

// Authenticated account state (never select the role in the browser).
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

async function initializeApp() {
  const main = document.getElementById("mainContent");
  main.innerHTML = `<p style="padding:2rem;color:var(--text-muted)">Loading your FitCart account...</p>`;

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
      throw new Error(`Unable to load account (HTTP ${response.status}).`);
    }

    currentUser = await response.json();
    currentRole = String(currentUser.role || "")
      .trim()
      .toUpperCase()
      .replace(/^ROLE_/, "");
    currentUserId = Number(currentUser.userId);
    currentSellerId = currentRole === "SELLER" ? currentUserId : null;

    if (
      !currentUserId ||
      !["BUYER", "SELLER", "ADMIN"].includes(currentRole)
    ) {
      throw new Error("The authenticated account has an invalid ID or role.");
    }

    renderCurrentUser();
    await loadCategories();
    renderDashboardForRole();
  } catch (error) {
    console.error("FitCart initialisation failed:", error);
    main.innerHTML = `
      <section class="form-card" style="max-width:700px;margin:2rem auto">
        <h2>Unable to load FitCart</h2>
        <p style="margin-top:.75rem;color:var(--text-muted)">
          We could not verify your signed-in account. Please sign in again.
        </p>
        <button class="btn btn-primary" style="margin-top:1rem"
          onclick="window.location.href='/login.html'">Go to Login</button>
      </section>`;
  }
}

// CSRF protection for state-changing requests.
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
    console.warn("Could not pre-load CSRF token:", error);
  }
}

async function apiFetch(url, options = {}) {
  const method = String(options.method || "GET").toUpperCase();
  const headers = new Headers(options.headers || {});
  const safeMethods = ["GET", "HEAD", "OPTIONS", "TRACE"];

  if (!safeMethods.includes(method)) {
    if (!csrfToken) await refreshCsrfToken();
    if (csrfToken) headers.set(csrfHeaderName, csrfToken);
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
    nameElement.textContent = currentUser.name || currentUser.email;
  }
  if (emailElement) emailElement.textContent = currentUser.email || "";
  if (roleElement) roleElement.textContent = currentRole;

  const navActions = document.querySelector(".nav-actions");
  if (navActions) {
    navActions.style.display = currentRole === "BUYER" ? "flex" : "none";
  }
}

async function logout() {
  try {
    const response = await apiFetch("/logout", { method: "POST" });
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

function showToast(msg) {
  const toast = document.getElementById("notification");
  if (!toast) return;

  toast.innerText = msg;
  toast.style.display = "block";
  setTimeout(() => {
    toast.style.display = "none";
  }, 3000);
}

// The dashboard is chosen only from the role returned by /auth/me.
function renderDashboardForRole() {
  const tabsContainer = document.getElementById("roleTabs");
  const main = document.getElementById("mainContent");
  tabsContainer.innerHTML = "";

  if (currentRole === "BUYER") {
    tabsContainer.innerHTML = `
      <button class="tab-btn active" onclick="setTab(this, renderBuyerCatalog)">Catalog & Products</button>
      <button class="tab-btn" onclick="setTab(this, renderBuyerOrders)">My Orders</button>`;
    renderBuyerCatalog();
    updateCartBadge();
    updateWishlistBadge();
  } else if (currentRole === "SELLER") {
    tabsContainer.innerHTML = `
      <button class="tab-btn active" onclick="setTab(this, renderSellerProducts)">My Products & Inventory</button>
      <button class="tab-btn" onclick="setTab(this, renderSellerAddProduct)">Add New Product</button>
      <button class="tab-btn" onclick="setTab(this, renderSellerOrders)">Customer Orders</button>`;
    renderSellerProducts();
  } else if (currentRole === "ADMIN") {
    tabsContainer.innerHTML = `
      <button class="tab-btn active" onclick="setTab(this, renderAdminCategories)">Categories</button>
      <button class="tab-btn" onclick="setTab(this, renderAdminOrders)">Platform Orders</button>
      <button class="tab-btn" onclick="setTab(this, renderAdminUsers)">User Management</button>`;
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

function setTab(btn, renderFn) {
  document.querySelectorAll(".tab-btn").forEach((b) => {
    b.classList.remove("active");
  });
  btn.classList.add("active");
  renderFn();
}

// DATA FETCHING
async function loadCategories() {
  try {
    const res = await apiFetch("/categories");
    if (res.ok) categories = await res.json();
  } catch (err) {
    console.error("Error loading categories:", err);
  }
}

async function loadProducts(keyword = "", categoryId = "") {
  try {
    let url = "/products?";
    if (keyword) url += `keyword=${encodeURIComponent(keyword)}&`;
    if (categoryId) url += `categoryId=${encodeURIComponent(categoryId)}&`;

    const res = await apiFetch(url);
    if (res.ok) products = await res.json();
  } catch (err) {
    console.error("Error loading products:", err);
  }
}

// BUYER EXPERIENCE
async function renderBuyerCatalog() {
  await loadProducts();

  const main = document.getElementById("mainContent");
  let categoryOptions = `<option value="">All Categories</option>`;

  categories.forEach((c) => {
    categoryOptions += `
      <option value="${c.categoryId}">${escapeHtml(c.categoryName)}</option>`;
  });

  main.innerHTML = `
    <div class="filters-bar">
      <input type="text" id="searchInput" class="search-input"
        placeholder="Search supplements, gym gear..." oninput="handleSearch()">
      <select id="categoryFilter" class="category-select"
        onchange="handleCategoryFilter(this.value)">
        ${categoryOptions}
      </select>
    </div>
    <div class="product-grid" id="productGrid">
      ${renderProductCards(products)}
    </div>`;
}

function renderProductCards(list) {
  if (!list || list.length === 0) {
    return `<p style="grid-column:1/-1;text-align:center;color:var(--text-muted);padding:3rem">No fitness products found in catalog.</p>`;
  }

  return list.map((p) => `
    <div class="product-card">
      <div>
        <div class="product-category">${escapeHtml(p.categoryName || "Fitness Gear")}</div>
        <div class="product-title">${escapeHtml(p.productName)}</div>
        <div class="product-desc">${escapeHtml(p.description || "Premium fitness product.")}</div>
      </div>
      <div>
        <div class="product-footer">
          <div class="product-price">₹${Number(p.price).toFixed(2)}</div>
          <div class="product-stock ${p.stockQuantity > 0 ? "in-stock" : "out-of-stock"}">
            ${p.stockQuantity > 0 ? `${p.stockQuantity} in stock` : "Out of stock"}
          </div>
        </div>
        <div style="display:flex;gap:.5rem;margin-top:1rem">
          <button class="btn btn-primary btn-sm" style="flex:1"
            ${p.stockQuantity <= 0 ? "disabled" : ""}
            onclick="addToCart(${p.productId})">Add to Cart</button>
          <button class="btn btn-secondary btn-sm"
            onclick="addToWishlist(${p.productId})">♥</button>
        </div>
      </div>
    </div>`).join("");
}

async function handleSearch() {
  const keyword = document.getElementById("searchInput").value;
  const categoryId = document.getElementById("categoryFilter").value;
  await loadProducts(keyword, categoryId);
  document.getElementById("productGrid").innerHTML = renderProductCards(products);
}

async function handleCategoryFilter(categoryId) {
  const keyword = document.getElementById("searchInput").value;
  await loadProducts(keyword, categoryId);
  document.getElementById("productGrid").innerHTML = renderProductCards(products);
}

// CART
async function updateCartBadge() {
  if (currentRole !== "BUYER" || !currentUserId) return;

  try {
    const res = await apiFetch(`/cart/${currentUserId}`);
    if (res.ok) {
      cart = await res.json();
      const count = cart.reduce((acc, item) => acc + item.quantity, 0);
      const badge = document.getElementById("cartCount");
      if (badge) badge.innerText = count;
    }
  } catch (e) {
    console.error(e);
  }
}

async function addToCart(productId) {
  if (currentRole !== "BUYER") {
    showToast("Only buyers can add products to their cart.");
    return;
  }

  try {
    const res = await apiFetch("/cart", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userId: currentUserId,
        productId,
        quantity: 1
      })
    });

    if (res.ok) {
      showToast("Added item to cart!");
      updateCartBadge();
    } else {
      const err = await readError(res);
      showToast(err.message || "Failed to add to cart");
    }
  } catch (e) {
    showToast("Network error");
  }
}

async function openCartModal() {
  if (currentRole !== "BUYER") return;
  await updateCartBadge();

  const list = document.getElementById("cartItemsList");
  let total = 0;

  if (cart.length === 0) {
    list.innerHTML = `<p style="color:var(--text-muted);text-align:center;padding:2rem">Your cart is currently empty.</p>`;
    document.getElementById("cartTotal").innerText = "0.00";
  } else {
    list.innerHTML = cart.map((item) => {
      total += Number(item.price) * item.quantity;
      return `
        <div style="display:flex;justify-content:space-between;align-items:center;padding:.75rem 0;border-bottom:1px solid var(--border)">
          <div>
            <strong>${escapeHtml(item.productName)}</strong>
            <div style="font-size:.8rem;color:var(--text-muted)">₹${Number(item.price).toFixed(2)} each</div>
          </div>
          <div style="display:flex;align-items:center;gap:.5rem">
            <span>Qty: ${item.quantity}</span>
            <button class="btn btn-secondary btn-sm" onclick="changeCartQty(${item.cartId},${item.quantity - 1})">-</button>
            <button class="btn btn-secondary btn-sm" onclick="changeCartQty(${item.cartId},${item.quantity + 1})">+</button>
            <button class="btn btn-secondary btn-sm" style="color:var(--danger)" onclick="removeCartItem(${item.cartId})">&times;</button>
          </div>
        </div>`;
    }).join("");

    document.getElementById("cartTotal").innerText = total.toFixed(2);
  }

  document.getElementById("cartModal").classList.add("active");
}

function closeCartModal() {
  document.getElementById("cartModal").classList.remove("active");
}

async function changeCartQty(cartId, qty) {
  if (currentRole !== "BUYER") return;
  if (qty <= 0) return removeCartItem(cartId);

  const res = await apiFetch(`/cart/${cartId}?quantity=${qty}`, { method: "PUT" });
  if (!res.ok) showToast("Failed to update cart quantity.");
  await openCartModal();
}

async function removeCartItem(cartId) {
  if (currentRole !== "BUYER") return;
  const res = await apiFetch(`/cart/${cartId}`, { method: "DELETE" });
  if (!res.ok) showToast("Failed to remove cart item.");
  await openCartModal();
  updateCartBadge();
}

async function clearUserCart() {
  if (currentRole !== "BUYER") return;

  const res = await apiFetch(`/cart/user/${currentUserId}`, { method: "DELETE" });
  if (!res.ok) showToast("Failed to clear cart.");
  await openCartModal();
  updateCartBadge();
}

// WISHLIST
async function updateWishlistBadge() {
  if (currentRole !== "BUYER" || !currentUserId) return;

  try {
    const res = await apiFetch(`/wishlist/${currentUserId}`);
    if (res.ok) {
      wishlist = await res.json();
      const badge = document.getElementById("wishlistCount");
      if (badge) badge.innerText = wishlist.length;
    }
  } catch (e) {
    console.error(e);
  }
}

async function addToWishlist(productId) {
  if (currentRole !== "BUYER") return;

  try {
    const res = await apiFetch("/wishlist", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId: currentUserId, productId })
    });

    if (res.ok) {
      showToast("Added to wishlist!");
      updateWishlistBadge();
    } else {
      showToast("Failed to add to wishlist.");
    }
  } catch (e) {
    showToast("Error updating wishlist");
  }
}

async function openWishlistModal() {
  if (currentRole !== "BUYER") return;
  await updateWishlistBadge();

  const list = document.getElementById("wishlistItemsList");

  if (wishlist.length === 0) {
    list.innerHTML = `<p style="color:var(--text-muted);text-align:center;padding:2rem">No items in wishlist.</p>`;
  } else {
    list.innerHTML = wishlist.map((item) => `
      <div style="display:flex;justify-content:space-between;align-items:center;padding:.75rem 0;border-bottom:1px solid var(--border)">
        <div>
          <strong>${escapeHtml(item.productName)}</strong>
          <div style="font-size:.8rem;color:var(--text-muted)">₹${Number(item.price).toFixed(2)}</div>
        </div>
        <div style="display:flex;gap:.5rem">
          <button class="btn btn-primary btn-sm" onclick="addToCart(${item.productId})">Move to Cart</button>
          <button class="btn btn-secondary btn-sm" onclick="removeWishlistItem(${item.wishlistId})">&times;</button>
        </div>
      </div>`).join("");
  }

  document.getElementById("wishlistModal").classList.add("active");
}

function closeWishlistModal() {
  document.getElementById("wishlistModal").classList.remove("active");
}

async function removeWishlistItem(wishlistId) {
  if (currentRole !== "BUYER") return;

  const res = await apiFetch(`/wishlist/${wishlistId}`, { method: "DELETE" });
  if (!res.ok) showToast("Failed to remove wishlist item.");
  await openWishlistModal();
  updateWishlistBadge();
}

// CHECKOUT
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
  const method = document.getElementById("checkoutPaymentMethod").value;

  try {
    const res = await apiFetch("/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userId: currentUserId,
        useCart: true,
        paymentMethod: method
      })
    });

    if (res.ok) {
      const order = await res.json();
      closeCheckoutModal();
      showToast(`Order #${order.orderId} placed successfully!`);
      updateCartBadge();
      renderBuyerOrders();
    } else {
      const err = await readError(res);
      showToast(err.message || "Failed to place order");
    }
  } catch (e) {
    showToast("Checkout failed");
  }
}

// BUYER ORDERS
async function renderBuyerOrders() {
  if (currentRole !== "BUYER") return;

  const main = document.getElementById("mainContent");
  main.innerHTML = `<h2 style="margin-bottom:1.5rem">My Order History</h2><p>Loading orders...</p>`;

  try {
    const res = await apiFetch(`/orders/user/${currentUserId}`);
    if (!res.ok) throw new Error("Unable to load orders.");

    const orders = await res.json();

    if (orders.length === 0) {
      main.innerHTML = `<h2 style="margin-bottom:1.5rem">My Order History</h2><p style="color:var(--text-muted)">You have not placed any orders yet.</p>`;
      return;
    }

    main.innerHTML = `
      <h2 style="margin-bottom:1.5rem">My Order History</h2>
      ${orders.map((o) => `
        <div style="background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius);padding:1.5rem;margin-bottom:1.5rem">
          <div style="display:flex;justify-content:space-between;flex-wrap:wrap;gap:.5rem;border-bottom:1px solid var(--border);padding-bottom:.75rem;margin-bottom:1rem">
            <div><strong>Order #${o.orderId}</strong> — ${escapeHtml(o.orderDate || "")}</div>
            <div>
              <span class="badge" style="background:${o.status === "DELIVERED" ? "var(--primary)" : "var(--accent)"}">${escapeHtml(o.status)}</span>
              <strong style="margin-left:1rem">Total: ₹${Number(o.totalAmount).toFixed(2)}</strong>
            </div>
          </div>
          <div>${(o.items || []).map((item) => `
            <div style="display:flex;justify-content:space-between;font-size:.9rem;padding:.3rem 0">
              <span>${escapeHtml(item.productName)} &times; ${item.quantity}</span>
              <span>₹${Number(item.price * item.quantity).toFixed(2)}</span>
            </div>`).join("")}</div>
          <div style="margin-top:1rem;font-size:.8rem;color:var(--text-muted)">
            Payment: ${o.payment ? `${escapeHtml(o.payment.paymentMethod)} (${escapeHtml(o.payment.paymentStatus)} - ${escapeHtml(o.payment.transactionId)})` : "Simulated"}
          </div>
        </div>`).join("")}`;
  } catch (e) {
    main.innerHTML = `<p style="color:var(--danger)">Failed to load order history.</p>`;
  }
}

// SELLER EXPERIENCE
async function renderSellerProducts() {
  if (currentRole !== "SELLER") return;

  const main = document.getElementById("mainContent");
  main.innerHTML = `<h2 style="margin-bottom:1.5rem">My Products & Stock Management</h2><p>Loading products...</p>`;

  try {
    const res = await apiFetch(`/products/seller/${currentSellerId}`);
    if (!res.ok) throw new Error("Unable to load seller products.");

    const sellerProducts = await res.json();

    main.innerHTML = `
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1.5rem">
        <h2>My Listed Products</h2>
        <button class="btn btn-primary" onclick="setTab(document.querySelectorAll('.tab-btn')[1],renderSellerAddProduct)">+ Add New Product</button>
      </div>
      <table class="data-table">
        <thead><tr><th>Product Name</th><th>Category</th><th>Price</th><th>Current Stock</th><th>Actions</th></tr></thead>
        <tbody>
          ${sellerProducts.length === 0
            ? `<tr><td colspan="5" style="text-align:center;color:var(--text-muted)">No products listed yet.</td></tr>`
            : sellerProducts.map((p) => `
              <tr>
                <td><strong>${escapeHtml(p.productName)}</strong></td>
                <td>${escapeHtml(p.categoryName || "General")}</td>
                <td>₹${Number(p.price).toFixed(2)}</td>
                <td><input type="number" id="stock_${p.productId}" value="${p.stockQuantity}" min="0" style="width:70px;padding:.3rem;background:#0f172a;color:#fff;border:1px solid var(--border);border-radius:4px"></td>
                <td><button class="btn btn-primary btn-sm" onclick="updateProductStock(${p.productId})">Save Stock</button></td>
              </tr>`).join("")}
        </tbody>
      </table>`;
  } catch (e) {
    main.innerHTML = `<p style="color:var(--danger)">Failed to load seller products. Check seller permissions.</p>`;
  }
}

function renderSellerAddProduct() {
  if (currentRole !== "SELLER") return;

  const main = document.getElementById("mainContent");
  const categoryOptions = categories.map((c) =>
    `<option value="${c.categoryId}">${escapeHtml(c.categoryName)}</option>`
  ).join("");

  main.innerHTML = `
    <h2 style="margin-bottom:1.5rem">Add New Fitness Product</h2>
    <div class="form-card">
      <form onsubmit="handleCreateProduct(event)">
        <div class="form-group">
          <label>Product Name</label>
          <input type="text" id="newProdName" class="form-control" required placeholder="e.g. Whey Protein Isolate 1kg">
        </div>
        <div class="form-group">
          <label>Category</label>
          <select id="newProdCategory" class="form-control" required>${categoryOptions}</select>
        </div>
        <div class="form-group">
          <label>Description</label>
          <textarea id="newProdDesc" class="form-control" rows="3" placeholder="Fitness product specifications, nutrition, etc."></textarea>
        </div>
        <div class="form-group">
          <label>Price (₹)</label>
          <input type="number" step="0.01" min="0.01" id="newProdPrice" class="form-control" required placeholder="49.99">
        </div>
        <div class="form-group">
          <label>Initial Stock Quantity</label>
          <input type="number" id="newProdStock" class="form-control" required min="0" value="25">
        </div>
        <button type="submit" class="btn btn-primary">Publish Product</button>
      </form>
    </div>`;
}

async function handleCreateProduct(e) {
  e.preventDefault();
  if (currentRole !== "SELLER") return;

  const name = document.getElementById("newProdName").value.trim();
  const catId = document.getElementById("newProdCategory").value;
  const desc = document.getElementById("newProdDesc").value;
  const price = document.getElementById("newProdPrice").value;
  const stock = document.getElementById("newProdStock").value;

  try {
    const res = await apiFetch("/products", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        productName: name,
        description: desc,
        price: Number(price),
        categoryId: Number(catId),
        sellerId: currentSellerId,
        initialStock: Number(stock)
      })
    });

    if (res.ok) {
      showToast("Product added successfully!");
      renderSellerProducts();
    } else {
      const err = await readError(res);
      showToast(err.message || "Failed to create product");
    }
  } catch (err) {
    showToast("Error creating product");
  }
}

async function updateProductStock(productId) {
  if (currentRole !== "SELLER") return;

  const newStock = document.getElementById(`stock_${productId}`).value;

  try {
    const res = await apiFetch(`/inventory/product/${productId}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ stockQuantity: Number(newStock) })
    });

    if (res.ok) {
      showToast("Stock updated!");
    } else {
      showToast("Failed to update stock. Check seller permissions.");
    }
  } catch (e) {
    showToast("Error updating stock");
  }
}

async function renderSellerOrders() {
  if (currentRole !== "SELLER") return;

  const main = document.getElementById("mainContent");
  main.innerHTML = `<h2 style="margin-bottom:1.5rem">Customer Orders for Your Products</h2><p>Loading...</p>`;

  try {
    const res = await apiFetch(`/orders/seller/${currentSellerId}`);
    if (!res.ok) throw new Error("Unable to load seller orders.");

    const orders = await res.json();

    main.innerHTML = `
      <h2 style="margin-bottom:1.5rem">Customer Orders for Your Products</h2>
      <table class="data-table">
        <thead><tr><th>Order #</th><th>Customer</th><th>Date</th><th>Status</th><th>Change Status</th></tr></thead>
        <tbody>
          ${orders.length === 0
            ? `<tr><td colspan="5" style="text-align:center;color:var(--text-muted)">No orders for your products yet.</td></tr>`
            : orders.map((o) => `
              <tr>
                <td>#${o.orderId}</td>
                <td>${escapeHtml(o.customerName || "Customer")}</td>
                <td>${escapeHtml(o.orderDate || "")}</td>
                <td><span class="badge">${escapeHtml(o.status)}</span></td>
                <td>
                  <select onchange="updateOrderStatus(${o.orderId},this.value)" style="background:#0f172a;color:#fff;padding:.3rem;border:1px solid var(--border);border-radius:4px">
                    <option value="CONFIRMED" ${o.status === "CONFIRMED" ? "selected" : ""}>CONFIRMED</option>
                    <option value="SHIPPED" ${o.status === "SHIPPED" ? "selected" : ""}>SHIPPED</option>
                    <option value="DELIVERED" ${o.status === "DELIVERED" ? "selected" : ""}>DELIVERED</option>
                    <option value="CANCELLED" ${o.status === "CANCELLED" ? "selected" : ""}>CANCELLED</option>
                  </select>
                </td>
              </tr>`).join("")}
        </tbody>
      </table>`;
  } catch (e) {
    main.innerHTML = `<p style="color:var(--danger)">Failed to load orders. Check seller permissions.</p>`;
  }
}

async function updateOrderStatus(orderId, status) {
  if (!["SELLER", "ADMIN"].includes(currentRole)) return;

  try {
    const res = await apiFetch(`/orders/${orderId}/status`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status })
    });

    if (res.ok) {
      showToast(`Order #${orderId} status updated to ${status}`);
      if (currentRole === "SELLER") renderSellerOrders();
      else renderAdminOrders();
    } else {
      showToast("Failed to update status. Check permissions.");
    }
  } catch (e) {
    showToast("Error updating status");
  }
}

// ADMIN EXPERIENCE
async function renderAdminCategories() {
  if (currentRole !== "ADMIN") return;
  await loadCategories();

  const main = document.getElementById("mainContent");

  main.innerHTML = `
    <h2 style="margin-bottom:1.5rem">Category Management</h2>
    <div class="form-card">
      <form onsubmit="handleCreateCategory(event)" style="display:flex;gap:.5rem">
        <input type="text" id="newCatName" class="form-control" placeholder="New category name (e.g. Gym Apparel)" required>
        <button type="submit" class="btn btn-primary">Add Category</button>
      </form>
    </div>
    <table class="data-table">
      <thead><tr><th>ID</th><th>Category Name</th><th>Actions</th></tr></thead>
      <tbody>
        ${categories.length === 0
          ? `<tr><td colspan="3" style="text-align:center;color:var(--text-muted)">No categories defined yet.</td></tr>`
          : categories.map((c) => `
            <tr>
              <td>#${c.categoryId}</td>
              <td><strong>${escapeHtml(c.categoryName)}</strong></td>
              <td><button class="btn btn-secondary btn-sm" style="color:var(--danger)" onclick="deleteCategory(${c.categoryId})">Delete</button></td>
            </tr>`).join("")}
      </tbody>
    </table>`;
}

async function handleCreateCategory(e) {
  e.preventDefault();
  if (currentRole !== "ADMIN") return;

  const name = document.getElementById("newCatName").value.trim();

  try {
    const res = await apiFetch("/categories", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ categoryName: name })
    });

    if (res.ok) {
      showToast("Category created!");
      renderAdminCategories();
    } else {
      const err = await readError(res);
      showToast(err.message || "Failed to create category");
    }
  } catch (e) {
    showToast("Error creating category");
  }
}

async function deleteCategory(id) {
  if (currentRole !== "ADMIN") return;
  if (!confirm("Are you sure you want to delete this category?")) return;

  try {
    const res = await apiFetch(`/categories/${id}`, { method: "DELETE" });

    if (res.ok) {
      showToast("Category deleted");
      renderAdminCategories();
    } else {
      showToast("Failed to delete category");
    }
  } catch (e) {
    showToast("Error deleting category");
  }
}

async function renderAdminOrders() {
  if (currentRole !== "ADMIN") return;

  const main = document.getElementById("mainContent");
  main.innerHTML = `<h2 style="margin-bottom:1.5rem">Platform Orders (Admin View)</h2><p>Loading...</p>`;

  try {
    const res = await apiFetch("/orders");
    if (!res.ok) throw new Error("Unable to load orders.");

    const orders = await res.json();

    main.innerHTML = `
      <h2 style="margin-bottom:1.5rem">Platform Orders (Admin View)</h2>
      <table class="data-table">
        <thead><tr><th>Order #</th><th>Customer</th><th>Date</th><th>Total</th><th>Status</th></tr></thead>
        <tbody>
          ${orders.length === 0
            ? `<tr><td colspan="5" style="text-align:center;color:var(--text-muted)">No orders recorded on platform yet.</td></tr>`
            : orders.map((o) => `
              <tr>
                <td>#${o.orderId}</td>
                <td>${escapeHtml(o.customerName || "Customer")}</td>
                <td>${escapeHtml(o.orderDate || "")}</td>
                <td>₹${Number(o.totalAmount).toFixed(2)}</td>
                <td><span class="badge">${escapeHtml(o.status)}</span></td>
              </tr>`).join("")}
        </tbody>
      </table>`;
  } catch (e) {
    main.innerHTML = `<p style="color:var(--danger)">Failed to load platform orders.</p>`;
  }
}

async function renderAdminUsers() {
  if (currentRole !== "ADMIN") return;

  const main = document.getElementById("mainContent");
  main.innerHTML = `<h2 style="margin-bottom:1.5rem">Platform Users</h2><p>Loading...</p>`;

  try {
    const res = await apiFetch("/users");
    if (!res.ok) throw new Error("Unable to load users.");

    const users = await res.json();

    main.innerHTML = `
      <h2 style="margin-bottom:1.5rem">Platform Users</h2>
      <table class="data-table">
        <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Phone</th></tr></thead>
        <tbody>
          ${users.length === 0
            ? `<tr><td colspan="5" style="text-align:center;color:var(--text-muted)">No users found.</td></tr>`
            : users.map((u) => `
              <tr>
                <td>#${u.userId}</td>
                <td>${escapeHtml(u.name)}</td>
                <td>${escapeHtml(u.email)}</td>
                <td><span class="badge" style="background:${u.role === "ADMIN" ? "var(--danger)" : u.role === "SELLER" ? "var(--accent)" : "var(--primary)"}">${escapeHtml(u.role)}</span></td>
                <td>${escapeHtml(u.phone || "—")}</td>
              </tr>`).join("")}
        </tbody>
      </table>`;
  } catch (e) {
    main.innerHTML = `<p style="color:var(--danger)">Failed to load platform users.</p>`;
  }
}

async function readError(response) {
  try {
    const data = await response.json();
    return data && typeof data === "object" ? data : { message: "Request failed." };
  } catch (error) {
    return { message: `Request failed (HTTP ${response.status}).` };
  }
}

function escapeHtml(value) {
  if (value === null || value === undefined) return "";
  return String(value).replace(/[&<>"']/g, (m) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[m]);
}
