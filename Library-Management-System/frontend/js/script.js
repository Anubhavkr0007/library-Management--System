const API = "http://localhost:8080/api";

// ---------- Dark / Light mode ----------
function getSavedTheme() {
  try { return localStorage.getItem("theme"); } catch (e) { return null; }
}
function applyTheme(t) {
  document.documentElement.setAttribute("data-theme", t);
  const btn = document.getElementById("themeBtn");
  if (btn) btn.textContent = t === "dark" ? "Light mode" : "Dark mode";
}
function wireTheme() {
  const btn = document.getElementById("themeBtn");
  applyTheme(document.documentElement.getAttribute("data-theme") || "light");
  if (btn) btn.onclick = () => {
    const next = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
    try { localStorage.setItem("theme", next); } catch (e) {}
    applyTheme(next);
  };
}
document.documentElement.setAttribute("data-theme",
  getSavedTheme() || (window.matchMedia && matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"));

// ---------- helpers ----------
async function api(path, method = "GET", data = {}) {
  try {
    const opts = { method };
    let url = API + path;
    const body = new URLSearchParams(data).toString();
    if (method === "GET") { if (body) url += "?" + body; }
    else { opts.headers = { "Content-Type": "application/x-www-form-urlencoded" }; opts.body = body; }
    const res = await fetch(url, opts);
    return await res.json();
  } catch (e) {
    return { ok: false, message: "Cannot reach backend. Is the Java server running?" };
  }
}
function getRole() { try { return sessionStorage.getItem("role"); } catch (e) { return null; } }
function getName() { try { return sessionStorage.getItem("name"); } catch (e) { return null; } }
function esc(s) { return String(s ?? "").replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); }
function show(id, text, good) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = text;
  el.className = "msg " + (good ? "good" : "err");
}

function buildNav(active) {
  const role = getRole();
  const links = [["dashboard.html", "Dashboard"], ["books.html", "Books"]];
  if (role === "admin") links.push(["members.html", "Members"], ["issue.html", "Issue"], ["return.html", "Return"]);
  document.getElementById("nav").innerHTML =
    '<span class="logo">Library Management</span>' +
    links.map(l => `<a href="${l[0]}" class="${l[0] === active ? "active" : ""}">${l[1]}</a>`).join("") +
    '<span class="spacer"></span><span>' + esc(getName()) + ' (' + esc(role) + ')</span>&nbsp;' +
    '<button id="themeBtn" type="button">Dark mode</button>&nbsp;' +
    '<button id="logoutBtn">Logout</button>';
  document.getElementById("logoutBtn").onclick = () => {
    try { sessionStorage.clear(); } catch (e) {}
    location.href = "index.html";
  };
}

function guard(active, adminOnly) {
  const role = getRole();
  if (!role || (adminOnly && role !== "admin")) { location.href = "index.html"; return false; }
  buildNav(active);
  return true;
}

// ---------- Login page ----------
function initLogin() {
  const role = document.getElementById("role");
  const userLabel = document.getElementById("userLabel");
  const passLabel = document.getElementById("passLabel");
  const hint = document.getElementById("hint");
  function setLabels() {
    const admin = role.value === "admin";
    userLabel.textContent = admin ? "Username" : "Email";
    passLabel.textContent = admin ? "Password" : "Phone";
    hint.textContent = admin ? "Default admin: admin / admin123" : "Members log in with the email and phone saved by the admin.";
  }
  role.onchange = setLabels; setLabels();

  document.getElementById("loginForm").onsubmit = async e => {
    e.preventDefault();
    const res = await api("/login", "POST", {
      role: role.value,
      username: document.getElementById("username").value,
      password: document.getElementById("password").value
    });
    if (!res.ok) return show("msg", res.message, false);
    try { sessionStorage.setItem("role", res.role); sessionStorage.setItem("name", res.name); } catch (err) {}
    location.href = "dashboard.html";
  };
}

// ---------- Dashboard ----------
async function initDashboard() {
  if (!guard("dashboard.html")) return;
  const res = await api("/dashboard");
  if (!res.ok) return show("msg", res.message, false);
  document.getElementById("totalBooks").textContent = res.books;
  document.getElementById("totalMembers").textContent = res.members;
  document.getElementById("issuedBooks").textContent = res.issued;
  document.getElementById("returnedBooks").textContent = res.returned;
}

// ---------- Books ----------
async function loadBooks(q = "") {
  const res = await api("/books", "GET", q ? { q } : {});
  if (!res.ok) return show("msg", res.message, false);
  const admin = getRole() === "admin";
  document.getElementById("rows").innerHTML = res.data.map(b =>
    `<tr><td>${b.id}</td><td>${esc(b.title)}</td><td>${esc(b.author)}</td><td>${b.quantity}</td>
     <td>${admin ? `<button class="btn small alt" onclick='editBook(${JSON.stringify(b).replace(/'/g, "&#39;")})'>Update</button>` : "-"}</td></tr>`
  ).join("") || '<tr><td colspan="5">No books found</td></tr>';
}
function editBook(b) {
  document.getElementById("bookId").value = b.id;
  document.getElementById("title").value = b.title;
  document.getElementById("author").value = b.author;
  document.getElementById("quantity").value = b.quantity;
  document.getElementById("saveBtn").textContent = "Update Book";
  window.scrollTo(0, 0);
}
function initBooks() {
  if (!guard("books.html")) return;
  const admin = getRole() === "admin";
  if (!admin) document.getElementById("formPanel").style.display = "none";
  document.getElementById("searchBtn").onclick = () => loadBooks(document.getElementById("search").value);
  document.getElementById("bookForm").onsubmit = async e => {
    e.preventDefault();
    const id = document.getElementById("bookId").value;
    const data = {
      title: document.getElementById("title").value,
      author: document.getElementById("author").value,
      quantity: document.getElementById("quantity").value
    };
    const res = id ? await api("/books/update", "POST", { id, ...data }) : await api("/books/add", "POST", data);
    show("msg", res.message, res.ok);
    if (res.ok) {
      e.target.reset(); document.getElementById("bookId").value = "";
      document.getElementById("saveBtn").textContent = "Add Book";
      loadBooks();
    }
  };
  loadBooks();
}

// ---------- Members ----------
async function loadMembers(q = "") {
  const res = await api("/members", "GET", q ? { q } : {});
  if (!res.ok) return show("msg", res.message, false);
  document.getElementById("rows").innerHTML = res.data.map(m =>
    `<tr><td>${m.id}</td><td>${esc(m.name)}</td><td>${esc(m.email)}</td><td>${esc(m.phone)}</td>
     <td><button class="btn small alt" onclick='editMember(${JSON.stringify(m).replace(/'/g, "&#39;")})'>Update</button></td></tr>`
  ).join("") || '<tr><td colspan="5">No members found</td></tr>';
}
function editMember(m) {
  document.getElementById("memberId").value = m.id;
  document.getElementById("name").value = m.name;
  document.getElementById("email").value = m.email;
  document.getElementById("phone").value = m.phone;
  document.getElementById("saveBtn").textContent = "Update Member";
  window.scrollTo(0, 0);
}
function initMembers() {
  if (!guard("members.html", true)) return;
  document.getElementById("searchBtn").onclick = () => loadMembers(document.getElementById("search").value);
  document.getElementById("memberForm").onsubmit = async e => {
    e.preventDefault();
    const id = document.getElementById("memberId").value;
    const data = {
      name: document.getElementById("name").value,
      email: document.getElementById("email").value,
      phone: document.getElementById("phone").value
    };
    const res = id ? await api("/members/update", "POST", { id, ...data }) : await api("/members/add", "POST", data);
    show("msg", res.message, res.ok);
    if (res.ok) {
      e.target.reset(); document.getElementById("memberId").value = "";
      document.getElementById("saveBtn").textContent = "Add Member";
      loadMembers();
    }
  };
  loadMembers();
}

// ---------- Issue ----------
async function initIssue() {
  if (!guard("issue.html", true)) return;
  const [books, members] = await Promise.all([api("/books"), api("/members")]);
  if (!books.ok || !members.ok) return show("msg", (books.message || members.message), false);
  document.getElementById("bookSel").innerHTML = books.data.map(b =>
    `<option value="${b.id}">${b.id} - ${esc(b.title)} (qty ${b.quantity})</option>`).join("");
  document.getElementById("memberSel").innerHTML = members.data.map(m =>
    `<option value="${m.id}">${m.id} - ${esc(m.name)}</option>`).join("");
  document.getElementById("issueForm").onsubmit = async e => {
    e.preventDefault();
    const res = await api("/issue", "POST", {
      bookId: document.getElementById("bookSel").value,
      memberId: document.getElementById("memberSel").value
    });
    show("msg", res.message, res.ok);
    if (res.ok) initIssue();
  };
}

// ---------- Return ----------
async function loadIssues() {
  const res = await api("/issues");
  if (!res.ok) return show("msg", res.message, false);
  document.getElementById("rows").innerHTML = res.data.map(r =>
    `<tr><td>${r.id}</td><td>${r.bookId}</td><td>${r.memberId}</td><td>${r.issueDate}</td><td>${r.dueDate}</td>
     <td>${r.returnDate ?? "-"}</td>
     <td>${r.returnDate ? (r.late ? '<span class="tag-late">Returned late</span>' : "Returned")
        : (r.late ? '<span class="tag-late">Overdue</span>' : "Issued")}</td>
     <td>${r.returnDate ? "-" : `<button class="btn small" onclick="returnBook(${r.id})">Return</button>`}</td></tr>`
  ).join("") || '<tr><td colspan="8">No issue records yet</td></tr>';
}
async function returnBook(id) {
  const res = await api("/return", "POST", { issueId: id });
  show("msg", res.message, res.ok);
  loadIssues();
}
function initReturn() {
  if (!guard("return.html", true)) return;
  document.getElementById("returnForm").onsubmit = async e => {
    e.preventDefault();
    await returnBook(document.getElementById("issueId").value);
    e.target.reset();
  };
  loadIssues();
}

// ---------- page router ----------
document.addEventListener("DOMContentLoaded", () => {
  const page = document.body.dataset.page;
  ({ login: initLogin, dashboard: initDashboard, books: initBooks,
     members: initMembers, issue: initIssue, ret: initReturn }[page] || (() => {}))();
  wireTheme();
});
