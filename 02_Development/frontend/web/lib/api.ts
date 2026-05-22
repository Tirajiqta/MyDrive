const API_BASE =
  process.env.NEXT_PUBLIC_API_URL || "http://localhost:8083";

// ─── Types ───────────────────────────────────────────────────────────────────

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  createdAt: string;
  lastLogin: string;
  currentStorageUsedBytes: number;
  storageLimitGB: number;
  preferredLanguageCode: string;
}

export interface AuthTokensResponse {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
}

export interface FileResponse {
  id: number;
  originalFileName: string;
  type: string;
  size: number;
  parentId: number | null;
  owner: UserResponse;
  uploadDate: string;
  lastModifiedDate: string;
  isDeleted: boolean;
}

export interface FolderResponse {
  id: number;
  canonicalName: string;
  translatedName: string;
  parentId: number | null;
  owner: UserResponse;
  creationDate: string;
  lastModifiedDate: string;
  isDeleted: boolean;
}

export interface ShareCreateResponse {
  shareId: number;
  token: string;
  itemType: string;
  itemId: number;
  permissionLevel: string;
  expiresAt: string | null;
  sharedDate: string;
}

export interface ShareListItemResponse {
  shareId: number;
  itemType: string;
  itemId: number;
  permissionLevel: string;
  expiresAt: string | null;
  sharedDate: string;
  revoked: boolean;
}

export interface ShareInfoResponse {
  shareId: number;
  itemType: string;
  itemId: number;
  permissionLevel: string;
  expiresAt: string | null;
  sharedDate: string;
}

// ─── Logged fetch ────────────────────────────────────────────────────────────

async function loggedFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const method = (options.method ?? "GET").toUpperCase();
  const start = Date.now();

  console.groupCollapsed(`→ ${method} ${url}`);
  const { Authorization, authorization, ...safeHeaders } =
    (options.headers as Record<string, string>) ?? {};
  if (Object.keys(safeHeaders).length) console.log("headers:", safeHeaders);
  if (Authorization ?? authorization)
    console.log("Authorization: Bearer <redacted>");
  if (options.body) {
    try {
      console.log("body:", JSON.parse(options.body as string));
    } catch {
      console.log("body:", options.body);
    }
  }
  console.groupEnd();

  let res: Response;
  try {
    res = await fetch(url, options);
  } catch (err: unknown) {
    const ms = Date.now() - start;
    console.error(`✗ ${method} ${url} network error after ${ms}ms:`, err);
    throw err;
  }

  const ms = Date.now() - start;
  if (res.ok) {
    console.log(`← ${res.status} ${res.statusText} ${method} ${url} (${ms}ms)`);
  } else {
    const bodyText = await res.clone().text();
    console.group(`✗ ${res.status} ${res.statusText} ${method} ${url} (${ms}ms)`);
    if (bodyText) {
      try { console.error("error body:", JSON.parse(bodyText)); }
      catch { console.error("error body:", bodyText); }
    }
    console.groupEnd();
  }

  return res;
}

// ─── Token helpers ────────────────────────────────────────────────────────────

function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("accessToken");
}

function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("refreshToken");
}

function saveTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("refreshToken", refreshToken);
}

function clearTokens() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("user");
}

export { clearTokens, saveTokens };

// ─── Core request ─────────────────────────────────────────────────────────────

async function refreshTokens(): Promise<boolean> {
  const rt = getRefreshToken();
  if (!rt) return false;
  try {
    const res = await loggedFetch(`${API_BASE}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: rt }),
    });
    if (!res.ok) return false;
    const data: AuthTokensResponse = await res.json();
    saveTokens(data.accessToken, data.refreshToken);
    return true;
  } catch {
    return false;
  }
}

async function request<T>(
  path: string,
  options: RequestInit & { skipAuth?: boolean } = {},
  retry = true
): Promise<T> {
  const { skipAuth, ...fetchOptions } = options;
  const token = getAccessToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(fetchOptions.headers as Record<string, string>),
  };
  if (token && !skipAuth) headers["Authorization"] = `Bearer ${token}`;

  const res = await loggedFetch(`${API_BASE}${path}`, { ...fetchOptions, headers });

  if (res.status === 401 && retry && !skipAuth) {
    const ok = await refreshTokens();
    if (ok) return request<T>(path, options, false);
    clearTokens();
    if (typeof window !== "undefined") window.location.href = "/login";
    throw new Error("Session expired");
  }

  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const text = await res.text();
      if (text) {
        try {
          const json = JSON.parse(text) as Record<string, unknown>;
          // Spring Boot validation errors: { errors: [{ defaultMessage, field }] }
          if (Array.isArray(json.errors) && json.errors.length > 0) {
            message = (json.errors as { defaultMessage?: string; field?: string }[])
              .map((e) => e.defaultMessage ?? e.field ?? "")
              .filter(Boolean)
              .join("; ");
          } else if (typeof json.message === "string" && json.message) {
            message = json.message;
          } else if (typeof json.error === "string" && json.error) {
            message = json.error;
          }
        } catch {
          message = text;
        }
      }
    } catch {}
    throw new Error(message);
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

export const auth = {
  register: (data: { username: string; email: string; password: string }) =>
    request<AuthTokensResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
      skipAuth: true,
    }),

  login: (data: { usernameOrEmail: string; password: string }) =>
    request<AuthTokensResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
      skipAuth: true,
    }),

  logout: () => {
    const rt = getRefreshToken();
    return request<void>("/api/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken: rt }),
    });
  },

  me: () => request<UserResponse>("/api/auth/me"),

  forgotPassword: (email: string) =>
    request<void>("/api/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify({ email }),
      skipAuth: true,
    }),

  resetPassword: (data: { token: string; newPassword: string }) =>
    request<void>("/api/auth/reset-password", {
      method: "POST",
      body: JSON.stringify(data),
      skipAuth: true,
    }),

  changePassword: (data: { currentPassword: string; newPassword: string }) =>
    request<void>("/api/auth/change-password", {
      method: "POST",
      body: JSON.stringify(data),
    }),
};

// ─── Files ────────────────────────────────────────────────────────────────────

export const filesApi = {
  list: (folderId: number) =>
    request<FileResponse[]>(`/api/files?folderId=${folderId}`),

  get: (id: number) => request<FileResponse>(`/api/files/${id}`),

  create: (data: {
    name: string;
    folderId: number;
    mimeType: string;
    sizeBytes: number;
    storageKey: string;
  }) =>
    request<FileResponse>("/api/files", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  update: (id: number, data: { name: string; parentId?: number }) =>
    request<FileResponse>(`/api/files/${id}`, {
      method: "PATCH",
      body: JSON.stringify(data),
    }),

  move: (id: number, targetFolderId: number) =>
    request<FileResponse>(`/api/files/${id}/move`, {
      method: "POST",
      body: JSON.stringify({ targetFolderId }),
    }),

  delete: (id: number) =>
    request<void>(`/api/files/${id}`, { method: "DELETE" }),
};

// ─── Folders ──────────────────────────────────────────────────────────────────

export const foldersApi = {
  list: (parentId?: number | null) =>
    request<FolderResponse[]>(
      parentId != null
        ? `/api/folders?parentId=${parentId}`
        : "/api/folders"
    ),

  create: (data: { canonicalName: string; parentId?: number | null }) =>
    request<FolderResponse>("/api/folders", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  update: (
    id: number,
    data: { canonicalName: string; parentId?: number | null }
  ) =>
    request<FolderResponse>(`/api/folders/${id}`, {
      method: "PATCH",
      body: JSON.stringify(data),
    }),

  delete: (id: number) =>
    request<void>(`/api/folders/${id}`, { method: "DELETE" }),
};

// ─── Shares ───────────────────────────────────────────────────────────────────

export const sharesApi = {
  list: () => request<ShareListItemResponse[]>("/api/shares"),

  create: (data: {
    itemId: number;
    itemType: "FILE" | "FOLDER";
    permissionLevel: "VIEW" | "EDIT";
    expiresAt?: string | null;
  }) =>
    request<ShareCreateResponse>("/api/shares", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  revoke: (id: number) =>
    request<void>(`/api/shares/${id}`, { method: "DELETE" }),
};

// ─── Public shares ────────────────────────────────────────────────────────────

export const publicSharesApi = {
  get: (token: string) =>
    request<ShareInfoResponse>(`/public/shares/${token}`),
};

// ─── Chat ─────────────────────────────────────────────────────────────────────

export const chatApi = {
  send: async (message: string): Promise<string> => {
    const token = getAccessToken();
    const res = await loggedFetch(`${API_BASE}/api/chat`, {
      method: "POST",
      headers: {
        "Content-Type": "text/plain",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: message,
    });
    if (!res.ok) throw new Error("Chat request failed");
    return res.text();
  },
};

// ─── Document content (collaborative editing) ────────────────────────────────

export interface DocumentContentResponse {
  fileId: number;
  content: string | null;
  lastUpdated: string | null;
  lastEditorUsername: string | null;
}

export const documentsApi = {
  async getContent(fileId: number): Promise<DocumentContentResponse> {
    const token = getAccessToken();
    const res = await loggedFetch(`${API_BASE}/api/documents/${fileId}/content`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) throw new Error("Failed to load document content");
    return res.json();
  },
};

// ─── Document signing (eIDAS PAdES) ──────────────────────────────────────────

export interface SignatureInfo {
  id: number;
  fileId: number;
  signerName: string;
  certificateSubject: string;
  certificateIssuer: string;
  certificateValidFrom: string;
  certificateValidTo: string;
  signedAt: string;
  signatureLevel: string;
  signedFileDownloadPath: string;
}

export const signingApi = {
  async sign(fileId: number): Promise<SignatureInfo> {
    const token = getAccessToken();
    const res = await loggedFetch(`${API_BASE}/api/signatures/files/${fileId}/sign`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) throw new Error("Signing failed");
    return res.json();
  },

  async getSignatures(fileId: number): Promise<SignatureInfo[]> {
    const token = getAccessToken();
    const res = await loggedFetch(`${API_BASE}/api/signatures/files/${fileId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) throw new Error("Failed to load signatures");
    return res.json();
  },

  downloadUrl(signatureId: number): string {
    return `${API_BASE}/api/signatures/${signatureId}/download`;
  },
};

export const API_BASE_URL = API_BASE;

// ─── Utils ────────────────────────────────────────────────────────────────────

export function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-GB", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}
