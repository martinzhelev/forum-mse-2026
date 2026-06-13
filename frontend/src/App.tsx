import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { Link, NavLink, Navigate, Route, Routes, useNavigate, useParams } from "react-router-dom";
import { api } from "./api";
import { useAuth } from "./auth";
import type { MaintenanceStatus, Post, Reply, User, UserInput, UserRole } from "./types";

function message(error: unknown) {
  return error instanceof Error ? error.message : "Something went wrong";
}

function dateTime(value?: string | null) {
  return value ? new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "Not set";
}

function PageState({ loading, error, children }: { loading: boolean; error: string; children: ReactNode }) {
  if (loading) return <div className="state-card">Loading…</div>;
  if (error) return <div className="state-card error">{error}</div>;
  return children;
}

function Shell() {
  const { session, logout } = useAuth();
  return (
    <div className="app-shell">
      <header className="topbar">
        <Link className="brand" to="/">
          <span className="brand-mark">CG</span>
          <span><strong>Common Ground</strong><small>Ideas worth discussing</small></span>
        </Link>
        <nav>
          <NavLink to="/">Forum</NavLink>
          {session && <NavLink to="/profile">Profile</NavLink>}
          {(session?.role === "ADMIN" || session?.role === "MODERATOR") && <NavLink to="/users">Users</NavLink>}
          {session?.role === "ADMIN" && <NavLink to="/maintenance">Maintenance</NavLink>}
        </nav>
        <div className="session-actions">
          {session ? (
            <>
              <span className="identity"><strong>{session.username}</strong><small>{session.role}</small></span>
              <button className="button ghost" onClick={logout}>Sign out</button>
            </>
          ) : <Link className="button" to="/login">Sign in</Link>}
        </div>
      </header>
      <main><Routes>
        <Route path="/" element={<ForumPage />} />
        <Route path="/posts/:id" element={<PostPage />} />
        <Route path="/replies/:id" element={<ReplyLookupPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/profile" element={<Protected><ProfilePage /></Protected>} />
        <Route path="/users" element={<RoleGate roles={["ADMIN", "MODERATOR"]}><UsersPage /></RoleGate>} />
        <Route path="/maintenance" element={<RoleGate roles={["ADMIN"]}><MaintenancePage /></RoleGate>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes></main>
      <footer><span>Common Ground Forum</span><a href="/docs" target="_blank">API documentation</a></footer>
    </div>
  );
}

function Protected({ children }: { children: ReactNode }) {
  return useAuth().session ? children : <Navigate to="/login" replace />;
}

function RoleGate({ roles, children }: { roles: UserRole[]; children: ReactNode }) {
  const { session } = useAuth();
  return session && roles.includes(session.role) ? children : <Navigate to="/" replace />;
}

function LoginPage() {
  const { session, login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  if (session) return <Navigate to="/" replace />;
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError("");
    try { await login(username, password); navigate("/"); } catch (e) { setError(message(e)); } finally { setBusy(false); }
  }
  return <section className="auth-layout">
    <div className="auth-intro"><span className="eyebrow">Welcome back</span><h1>Join the conversation.</h1><p>Thoughtful discussions, shared openly. Sign in to publish posts and replies.</p></div>
    <form className="panel form-stack" onSubmit={submit}>
      <h2>Sign in</h2>
      {error && <div className="inline-error">{error}</div>}
      <label>Username<input value={username} onChange={e => setUsername(e.target.value)} required /></label>
      <label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} required /></label>
      <button className="button wide" disabled={busy}>{busy ? "Signing in…" : "Sign in"}</button>
      <small>Local default: admin / admin</small>
    </form>
  </section>;
}

function ForumPage() {
  const { session } = useAuth();
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [busy, setBusy] = useState(false);
  async function load() { setLoading(true); try { setPosts(await api.listPosts()); setError(""); } catch (e) { setError(message(e)); } finally { setLoading(false); } }
  useEffect(() => { void load(); }, []);
  async function create(event: FormEvent) {
    event.preventDefault(); if (!session) return; setBusy(true);
    try { const post = await api.createPost(title, content, session.token); setPosts(current => [...current, post]); setTitle(""); setContent(""); } catch (e) { setError(message(e)); } finally { setBusy(false); }
  }
  return <section className="page">
    <div className="hero"><div><span className="eyebrow">The forum</span><h1>Ideas become clearer<br />when we share them.</h1><p>Ask questions, offer a perspective, and build on what others know.</p></div><div className="hero-stat"><strong>{posts.length}</strong><span>open discussions</span></div></div>
    {session && <details className="composer"><summary><span>Start a new discussion</span><b>+</b></summary><form className="form-stack" onSubmit={create}><label>Title<input value={title} onChange={e => setTitle(e.target.value)} maxLength={500} required placeholder="What should we talk about?" /></label><label>Your opening thought<textarea value={content} onChange={e => setContent(e.target.value)} maxLength={10000} required rows={5} /></label><button className="button" disabled={busy}>{busy ? "Publishing…" : "Publish discussion"}</button></form></details>}
    <div className="section-heading"><h2>Recent discussions</h2><button className="text-button" onClick={() => void load()}>Refresh</button></div>
    <PageState loading={loading} error={error}><div className="post-list">{posts.length ? [...posts].reverse().map(post => <Link className="post-card" key={post.id} to={`/posts/${post.id}`}><div className="post-number">#{post.id}</div><div><h3>{post.title}</h3><p>{post.content}</p><span>{dateTime(post.createdAt)}</span></div><b>→</b></Link>) : <div className="state-card">No discussions yet. Start the first one.</div>}</div></PageState>
  </section>;
}

function PostPage() {
  const { id } = useParams();
  const { session } = useAuth();
  const postId = Number(id);
  const [post, setPost] = useState<Post | null>(null);
  const [replies, setReplies] = useState<Reply[]>([]);
  const [content, setContent] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  async function load() { setLoading(true); try { const [p, r] = await Promise.all([api.getPost(postId), api.listReplies(postId)]); setPost(p); setReplies(r); setError(""); } catch (e) { setError(message(e)); } finally { setLoading(false); } }
  // Reload the full discussion whenever the route changes.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { void load(); }, [postId]);
  async function reply(event: FormEvent) {
    event.preventDefault(); if (!session) return;
    try { const created = await api.createReply(postId, content, session.token); setReplies(current => [...current, created]); setContent(""); } catch (e) { setError(message(e)); }
  }
  return <section className="page narrow"><Link className="back-link" to="/">← All discussions</Link><PageState loading={loading} error={error}>{post && <><article className="topic"><span className="eyebrow">Discussion #{post.id}</span><h1>{post.title}</h1><p>{post.content}</p><time>{dateTime(post.createdAt)}</time></article><div className="section-heading"><h2>{replies.length} {replies.length === 1 ? "reply" : "replies"}</h2></div><div className="reply-list">{replies.map(reply => <Link to={`/replies/${reply.id}`} className="reply-card" key={reply.id}><span>#{reply.id}</span><p>{reply.content}</p><time>{dateTime(reply.createdAt)}</time></Link>)}</div>{session ? <form className="panel form-stack reply-form" onSubmit={reply}><h3>Add your perspective</h3><textarea value={content} onChange={e => setContent(e.target.value)} maxLength={10000} rows={4} required /><button className="button">Post reply</button></form> : <div className="signin-prompt"><Link to="/login">Sign in</Link> to join this discussion.</div>}</>}</PageState></section>;
}

function ReplyLookupPage() {
  const { id } = useParams();
  const [reply, setReply] = useState<Reply | null>(null);
  const [error, setError] = useState("");
  useEffect(() => { api.getReply(Number(id)).then(setReply).catch(e => setError(message(e))); }, [id]);
  return <section className="page narrow"><PageState loading={!reply && !error} error={error}>{reply && <><Link className="back-link" to={`/posts/${reply.postId}`}>← Discussion #{reply.postId}</Link><article className="topic"><span className="eyebrow">Reply #{reply.id}</span><p>{reply.content}</p><time>{dateTime(reply.createdAt)}</time></article></>}</PageState></section>;
}

function ProfilePage() {
  const { session } = useAuth();
  const [user, setUser] = useState<User | null>(null);
  const [error, setError] = useState("");
  useEffect(() => { if (session) api.getUser(session.userId, session.token).then(setUser).catch(e => setError(message(e))); }, [session]);
  return <section className="page narrow"><div className="section-heading"><div><span className="eyebrow">Your account</span><h1>Profile</h1></div></div><PageState loading={!user && !error} error={error}>{user && <UserEditor user={user} canSetRole={session?.role === "ADMIN"} onSaved={setUser} />}</PageState></section>;
}

function UserEditor({ user, canSetRole, onSaved }: { user: User; canSetRole: boolean; onSaved: (user: User) => void }) {
  const { session } = useAuth();
  const [input, setInput] = useState<UserInput>({ username: user.username, email: user.email, role: user.role, password: "" });
  const [notice, setNotice] = useState("");
  async function save(event: FormEvent) {
    event.preventDefault(); if (!session) return;
    try { const result = await api.updateUser(user.id, { ...input, password: input.password || undefined }, session.token); onSaved(result); setInput({ ...input, password: "" }); setNotice("Changes saved."); } catch (e) { setNotice(message(e)); }
  }
  return <form className="panel form-stack" onSubmit={save}><div className="form-grid"><label>Username<input value={input.username} onChange={e => setInput({ ...input, username: e.target.value })} required /></label><label>Email<input type="email" value={input.email ?? ""} onChange={e => setInput({ ...input, email: e.target.value || null })} /></label><label>Role<select value={input.role} disabled={!canSetRole} onChange={e => setInput({ ...input, role: e.target.value as UserRole })}><option>USER</option><option>MODERATOR</option><option>ADMIN</option></select></label><label>New password<input type="password" minLength={8} maxLength={72} value={input.password} onChange={e => setInput({ ...input, password: e.target.value })} placeholder="Leave blank to keep current" /></label></div>{notice && <p className="notice">{notice}</p>}<button className="button">Save changes</button></form>;
}

function UsersPage() {
  const { session } = useAuth();
  const [users, setUsers] = useState<User[]>([]);
  const [error, setError] = useState("");
  const [input, setInput] = useState<UserInput>({ username: "", email: "", role: "USER", password: "" });
  async function load() { if (!session) return; try { setUsers(await api.listUsers(session.token)); setError(""); } catch (e) { setError(message(e)); } }
  // The session change is the only event that should refetch this administration view.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { void load(); }, [session]);
  async function create(event: FormEvent) { event.preventDefault(); if (!session) return; try { await api.createUser(input, session.token); setInput({ username: "", email: "", role: "USER", password: "" }); await load(); } catch (e) { setError(message(e)); } }
  async function remove(id: number) { if (!session || !confirm("Delete this user?")) return; try { await api.deleteUser(id, session.token); await load(); } catch (e) { setError(message(e)); } }
  return <section className="page"><div className="section-heading"><div><span className="eyebrow">Administration</span><h1>Users</h1></div><span>{users.length} accounts</span></div>{error && <div className="inline-error">{error}</div>}{session?.role === "ADMIN" && <details className="composer"><summary><span>Create user</span><b>+</b></summary><form className="form-grid" onSubmit={create}><label>Username<input value={input.username} onChange={e => setInput({ ...input, username: e.target.value })} required /></label><label>Email<input type="email" value={input.email ?? ""} onChange={e => setInput({ ...input, email: e.target.value })} /></label><label>Role<select value={input.role} onChange={e => setInput({ ...input, role: e.target.value as UserRole })}><option>USER</option><option>MODERATOR</option><option>ADMIN</option></select></label><label>Password<input type="password" minLength={8} maxLength={72} value={input.password} onChange={e => setInput({ ...input, password: e.target.value })} required /></label><button className="button">Create account</button></form></details>}<div className="table-wrap"><table><thead><tr><th>User</th><th>Role</th><th>Joined</th><th></th></tr></thead><tbody>{users.map(user => <tr key={user.id}><td><strong>{user.username}</strong><small>{user.email || "No email"}</small></td><td><span className={`role ${user.role.toLowerCase()}`}>{user.role}</span></td><td>{dateTime(user.createdAt)}</td><td>{session?.role === "ADMIN" && user.id !== session.userId && <button className="text-button danger" onClick={() => void remove(user.id)}>Delete</button>}</td></tr>)}</tbody></table></div></section>;
}

function MaintenancePage() {
  const { session } = useAuth();
  const [status, setStatus] = useState<MaintenanceStatus | null>(null);
  const [health, setHealth] = useState("Checking");
  const [duration, setDuration] = useState(120);
  const [error, setError] = useState("");
  async function load() { if (!session) return; try { setStatus(await api.maintenanceStatus(session.token)); setError(""); } catch (e) { setError(message(e)); } }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { void load(); api.health().then(result => setHealth(result.status)).catch(() => setHealth("Unavailable")); }, [session]);
  async function toggle() { if (!session || !status) return; try { setStatus(status.restoreInProgress ? await api.finishRestore(session.token) : await api.startRestore(duration, session.token)); } catch (e) { setError(message(e)); } }
  return <section className="page narrow"><div className="section-heading"><div><span className="eyebrow">Operations</span><h1>Maintenance</h1></div><button className="text-button" onClick={() => void load()}>Refresh</button></div>{error && <div className="inline-error">{error}</div>}<div className="metric-grid"><div className="metric"><span>Application health</span><strong>{health}</strong></div><div className="metric"><span>Restore mode</span><strong>{status?.restoreInProgress ? "Active" : "Inactive"}</strong></div><div className="metric"><span>Retry after</span><strong>{status?.retryAfterSeconds ?? "—"}s</strong></div></div><div className={`panel maintenance-panel ${status?.restoreInProgress ? "active" : ""}`}><div><h2>{status?.restoreInProgress ? "Restore is in progress" : "Service is open"}</h2><p>{status?.restoreInProgress ? "Regular endpoints return 503. Authentication, health, and maintenance controls remain available." : "Starting restore mode temporarily blocks regular API traffic."}</p><small>Started: {dateTime(status?.restoreStartedAt)} · Estimated completion: {dateTime(status?.estimatedCompletionAt)}</small></div>{!status?.restoreInProgress && <label>Estimated duration (seconds)<input type="number" min={1} value={duration} onChange={e => setDuration(Number(e.target.value))} /></label>}<button className={`button ${status?.restoreInProgress ? "danger-button" : ""}`} onClick={() => void toggle()} disabled={!status}>{status?.restoreInProgress ? "Finish restore" : "Start restore"}</button></div></section>;
}

export function App() {
  return <Shell />;
}
