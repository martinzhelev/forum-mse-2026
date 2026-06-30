const sessionKey = "forum-session";

const state = {
  session: loadSession(),
  route: { name: "topics" },
  topics: [],
  topic: null,
  replies: null,
  users: [],
  message: "",
  error: "",
};

const api = {
  async request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body) {
      headers.set("Content-Type", "application/json");
    }
    if (state.session?.token) {
      headers.set("Authorization", `Bearer ${state.session.token}`);
    }
    const response = await fetch(path, { ...options, headers });
    if (!response.ok) {
      const body = await response.json().catch(() => null);
      throw new Error(body?.message || body?.error || `${response.status} ${response.statusText}`);
    }
    return response.status === 204 ? null : response.json();
  },
  login(username, password) {
    return this.request("/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
  },
  register(payload) {
    return this.request("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  listTopics() {
    return this.request("/posts");
  },
  getTopic(id) {
    return this.request(`/posts/${id}`);
  },
  createTopic(payload) {
    return this.request("/posts", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  updateTopic(id, payload) {
    return this.request(`/posts/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
  listReplies(topicId, page = 0) {
    return this.request(`/posts/${topicId}/replies?page=${page}&size=10`);
  },
  createReply(topicId, content) {
    return this.request(`/posts/${topicId}/replies`, {
      method: "POST",
      body: JSON.stringify({ content }),
    });
  },
  updateReply(id, content) {
    return this.request(`/replies/${id}`, {
      method: "PUT",
      body: JSON.stringify({ content }),
    });
  },
  listUsers() {
    return this.request("/users");
  },
  createUser(payload) {
    return this.request("/users", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  updateUser(id, payload) {
    return this.request(`/users/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
};

function loadSession() {
  try {
    const session = JSON.parse(localStorage.getItem(sessionKey) || "null");
    if (!session || session.expiresAt <= Date.now()) {
      localStorage.removeItem(sessionKey);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

function saveSession(loginResponse) {
  const claims = parseJwt(loginResponse.accessToken);
  state.session = {
    token: loginResponse.accessToken,
    userId: claims.uid,
    username: claims.sub,
    role: claims.role,
    expiresAt: claims.exp * 1000,
  };
  localStorage.setItem(sessionKey, JSON.stringify(state.session));
}

function parseJwt(token) {
  const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(atob(payload));
}

function canEdit(author) {
  const user = state.session;
  return Boolean(user && author && (user.userId === author.id || user.role === "ADMIN" || user.role === "MODERATOR"));
}

function fmt(value) {
  if (!value) return "Not set";
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}

function setRoute(name, params = {}) {
  state.route = { name, ...params };
  state.message = "";
  state.error = "";
  render();
  loadRouteData();
}

async function loadRouteData() {
  try {
    if (state.route.name === "topics") {
      state.topics = await api.listTopics();
      render();
    }
    if (state.route.name === "topic") {
      await loadTopic(state.route.id, state.route.page || 0);
    }
    if (state.route.name === "users" && state.session?.role === "ADMIN") {
      state.users = await api.listUsers();
      render();
    }
  } catch (error) {
    state.error = error.message;
    render();
  }
}

async function loadTopic(id, page = 0) {
  state.topic = await api.getTopic(id);
  state.replies = await api.listReplies(id, page);
  state.route.page = state.replies.page;
  render();
}

function app() {
  return `
    <div class="shell">
      <header class="topbar">
        <div class="brand">
          <span class="brand-mark">CG</span>
          <span>Common Ground Forum</span>
        </div>
        <nav class="nav">
          ${navButton("topics", "Topics")}
          ${state.session ? navButton("profile", "Profile") : ""}
          ${state.session?.role === "ADMIN" ? navButton("users", "Users") : ""}
          ${state.session ? "" : navButton("auth", "Login / Register")}
        </nav>
        <div class="identity">
          ${state.session ? `<span>${escapeHtml(state.session.username)} <span class="badge">${state.session.role}</span></span><button class="button secondary" data-action="logout">Sign out</button>` : `<span class="small">Guest</span>`}
        </div>
      </header>
      <main class="page">
        ${state.error ? `<div class="error">${escapeHtml(state.error)}</div>` : ""}
        ${state.message ? `<div class="notice">${escapeHtml(state.message)}</div>` : ""}
        ${renderRoute()}
      </main>
    </div>
  `;
}

function navButton(name, label) {
  return `<button class="${state.route.name === name ? "active" : ""}" data-route="${name}">${label}</button>`;
}

function renderRoute() {
  if (state.route.name === "auth") return authView();
  if (state.route.name === "profile") return profileView();
  if (state.route.name === "users") return usersView();
  if (state.route.name === "topic") return topicView();
  return topicsView();
}

function topicsView() {
  return `
    <section class="hero">
      <div>
        <h1>Topics</h1>
        <p>Browse discussions, open a topic to load replies 10 per page, or create a new topic after login.</p>
      </div>
      <span class="badge">${state.topics.length} topics</span>
    </section>
    <section class="grid">
      <div class="topic-list">
        ${state.topics.length ? state.topics.map(topicCard).join("") : `<div class="panel">No topics yet.</div>`}
      </div>
      <aside class="panel stack">
        <h2>Create Topic</h2>
        ${state.session ? topicForm() : `<p>Register or login to create topics and replies.</p><button class="button" data-route="auth">Login / Register</button>`}
      </aside>
    </section>
  `;
}

function topicCard(topic) {
  return `
    <button class="topic-card" data-topic-id="${topic.id}">
      <header>
        <div>
          <h3>${escapeHtml(topic.title)}</h3>
          <div class="meta">By ${escapeHtml(topic.author?.username || "unknown")} · created ${fmt(topic.createdAt)} · modified ${fmt(topic.modifiedAt)}</div>
        </div>
        <span class="badge">${topic.viewCount || 0} views</span>
      </header>
      <p>${escapeHtml(topic.content || "")}</p>
    </button>
  `;
}

function topicForm(topic = null) {
  return `
    <form class="form" data-form="${topic ? "update-topic" : "create-topic"}" ${topic ? `data-id="${topic.id}"` : ""}>
      <label>Unique title
        <input name="title" maxlength="500" required value="${escapeAttr(topic?.title || "")}" />
      </label>
      <label>Content
        <textarea name="content" maxlength="10000" rows="6" required>${escapeHtml(topic?.content || "")}</textarea>
      </label>
      <button class="button">${topic ? "Save topic" : "Publish topic"}</button>
    </form>
  `;
}

function topicView() {
  const topic = state.topic;
  if (!topic) return `<div class="panel">Loading topic...</div>`;
  const replies = state.replies;
  return `
    <section class="stack">
      <button class="text-button" data-route="topics">Back to topics</button>
      <article class="panel stack">
        <div class="row-between">
          <div>
            <h1>${escapeHtml(topic.title)}</h1>
            <p>${escapeHtml(topic.content || "")}</p>
            <div class="meta">By ${escapeHtml(topic.author?.username || "unknown")} · created ${fmt(topic.createdAt)} · modified ${fmt(topic.modifiedAt)}</div>
          </div>
          <span class="badge">${topic.viewCount || 0} views</span>
        </div>
        ${canEdit(topic.author) ? `<details><summary>Edit topic</summary>${topicForm(topic)}</details>` : ""}
      </article>
      <section class="grid">
        <div class="stack">
          <div class="row-between">
            <h2>Replies</h2>
            <span class="badge">${replies?.totalElements || 0} total</span>
          </div>
          <div class="reply-list">
            ${replies?.items?.length ? replies.items.map(replyCard).join("") : `<div class="panel">No replies yet.</div>`}
          </div>
          ${pagination()}
        </div>
        <aside class="panel stack">
          <h2>Create Reply</h2>
          ${state.session ? replyForm() : `<p>Login or register to reply.</p><button class="button" data-route="auth">Login / Register</button>`}
        </aside>
      </section>
    </section>
  `;
}

function replyCard(reply) {
  return `
    <article class="reply-card stack">
      <header>
        <div class="meta">By ${escapeHtml(reply.author?.username || "unknown")} · created ${fmt(reply.createdAt)} · modified ${fmt(reply.modifiedAt)}</div>
        <span class="badge">#${reply.id}</span>
      </header>
      <p>${escapeHtml(reply.content || "")}</p>
      ${canEdit(reply.author) ? `<details><summary>Edit reply</summary>${replyForm(reply)}</details>` : ""}
    </article>
  `;
}

function replyForm(reply = null) {
  return `
    <form class="form" data-form="${reply ? "update-reply" : "create-reply"}" ${reply ? `data-id="${reply.id}"` : ""}>
      <label>Reply text
        <textarea name="content" maxlength="10000" rows="4" required>${escapeHtml(reply?.content || "")}</textarea>
      </label>
      <button class="button">${reply ? "Save reply" : "Post reply"}</button>
    </form>
  `;
}

function pagination() {
  const page = state.replies;
  if (!page || page.totalPages <= 1) return "";
  return `
    <div class="pagination panel">
      <button class="button secondary" data-page="${page.page - 1}" ${page.page <= 0 ? "disabled" : ""}>Previous</button>
      <span>Page ${page.page + 1} of ${page.totalPages}</span>
      <button class="button secondary" data-page="${page.page + 1}" ${page.page + 1 >= page.totalPages ? "disabled" : ""}>Next</button>
    </div>
  `;
}

function authView() {
  return `
    <section class="grid">
      <form class="panel form" data-form="login">
        <h2>Login</h2>
        <label>Username <input name="username" required autocomplete="off" /></label>
        <label>Password <input name="password" type="password" required autocomplete="new-password" /></label>
        <button class="button">Login</button>
      </form>
      <form class="panel form" data-form="register">
        <h2>Register User</h2>
        <label>Username <input name="username" required maxlength="100" autocomplete="off" /></label>
        <label>Email <input name="email" type="email" maxlength="320" autocomplete="off" /></label>
        <label>Password <input name="password" type="password" minlength="8" maxlength="72" required autocomplete="new-password" /></label>
        <button class="button">Register</button>
        <span class="small">Registration always creates a regular USER account.</span>
      </form>
    </section>
  `;
}

function profileView() {
  if (!state.session) return authView();
  return `
    <section class="panel stack">
      <h1>Profile</h1>
      <p>You are signed in as <strong>${escapeHtml(state.session.username)}</strong> with role <span class="badge">${state.session.role}</span>.</p>
      <p>Admins can promote users to moderators from the Users screen.</p>
    </section>
  `;
}

function usersView() {
  if (state.session?.role !== "ADMIN") return `<div class="panel">Only admins can manage users.</div>`;
  return `
    <section class="stack">
      <div class="row-between">
        <h1>Users</h1>
        <button class="button secondary" data-action="refresh-users">Refresh</button>
      </div>
      <div class="grid">
        <div class="panel">
          <table class="table">
            <thead><tr><th>User</th><th>Role</th><th>Joined</th><th>Promote / edit</th></tr></thead>
            <tbody>
              ${state.users.map(userRow).join("")}
            </tbody>
          </table>
        </div>
        <form class="panel form" data-form="create-user">
          <h2>Create User</h2>
          <label>Username <input name="username" required maxlength="100" /></label>
          <label>Email <input name="email" type="email" maxlength="320" /></label>
          <label>Role
            <select name="role">
              <option>USER</option>
              <option>MODERATOR</option>
            </select>
          </label>
          <label>Password <input name="password" type="password" minlength="8" maxlength="72" required /></label>
          <button class="button">Create account</button>
        </form>
      </div>
    </section>
  `;
}

function userRow(user) {
  return `
    <tr>
      <td><strong>${escapeHtml(user.username)}</strong><br><span class="small">${escapeHtml(user.email || "No email")}</span></td>
      <td><span class="badge">${user.role}</span></td>
      <td>${fmt(user.createdAt)}</td>
      <td>
        <form class="inline-actions" data-form="update-user" data-id="${user.id}">
          <input name="username" class="hidden" value="${escapeAttr(user.username)}" />
          <input name="email" class="hidden" value="${escapeAttr(user.email || "")}" />
          <select name="role">
            <option ${user.role === "USER" ? "selected" : ""}>USER</option>
            <option ${user.role === "MODERATOR" ? "selected" : ""}>MODERATOR</option>
          </select>
          <input name="password" class="hidden" value="" />
          <button class="button secondary">Save</button>
        </form>
      </td>
    </tr>
  `;
}

async function handleSubmit(event) {
  const form = event.target.closest("form[data-form]");
  if (!form) return;
  event.preventDefault();
  const data = Object.fromEntries(new FormData(form));
  try {
    state.error = "";
    state.message = "";
    switch (form.dataset.form) {
      case "login": {
        saveSession(await api.login(data.username, data.password));
        state.message = "Logged in.";
        setRoute("topics");
        return;
      }
      case "register": {
        await api.register({ username: data.username, email: data.email || undefined, password: data.password });
        saveSession(await api.login(data.username, data.password));
        state.message = "Registered and logged in.";
        setRoute("topics");
        return;
      }
      case "create-topic":
        await api.createTopic({ title: data.title, content: data.content });
        state.message = "Topic created.";
        await loadRouteData();
        break;
      case "update-topic":
        await api.updateTopic(form.dataset.id, { title: data.title, content: data.content });
        state.message = "Topic updated.";
        await loadTopic(state.route.id, state.route.page || 0);
        break;
      case "create-reply":
        await api.createReply(state.topic.id, data.content);
        state.message = "Reply created.";
        await loadTopic(state.topic.id, state.route.page || 0);
        break;
      case "update-reply":
        await api.updateReply(form.dataset.id, data.content);
        state.message = "Reply updated.";
        await loadTopic(state.topic.id, state.route.page || 0);
        break;
      case "create-user":
        await api.createUser({ username: data.username, email: data.email || undefined, role: data.role, password: data.password });
        state.message = "User created.";
        state.users = await api.listUsers();
        render();
        break;
      case "update-user":
        await api.updateUser(form.dataset.id, {
          username: data.username,
          email: data.email || null,
          role: data.role,
          password: data.password || undefined,
        });
        state.message = "User role saved.";
        state.users = await api.listUsers();
        render();
        break;
    }
  } catch (error) {
    state.error = error.message;
    render();
  }
}

function handleClick(event) {
  const route = event.target.closest("[data-route]")?.dataset.route;
  if (route) {
    setRoute(route);
    return;
  }
  const topicId = event.target.closest("[data-topic-id]")?.dataset.topicId;
  if (topicId) {
    state.topic = null;
    state.replies = null;
    setRoute("topic", { id: Number(topicId), page: 0 });
    return;
  }
  const page = event.target.closest("[data-page]")?.dataset.page;
  if (page !== undefined && state.topic) {
    loadTopic(state.topic.id, Number(page)).catch(error => {
      state.error = error.message;
      render();
    });
    return;
  }
  const action = event.target.closest("[data-action]")?.dataset.action;
  if (action === "logout") {
    state.session = null;
    localStorage.removeItem(sessionKey);
    state.message = "Signed out.";
    setRoute("topics");
  }
  if (action === "refresh-users") {
    loadRouteData();
  }
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;",
  })[char]);
}

function escapeAttr(value) {
  return escapeHtml(value).replace(/`/g, "&#096;");
}

function render() {
  document.getElementById("app").innerHTML = app();
}

document.addEventListener("submit", handleSubmit);
document.addEventListener("click", handleClick);

render();
loadRouteData();
