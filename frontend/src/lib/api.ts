import { supabase } from './supabase';

const API_BASE = import.meta.env.VITE_API_URL || '/api';

interface ApiOptions {
  method?: string;
  body?: FormData | string;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

async function getAuthHeaders(): Promise<Record<string, string>> {
  try {
    const { data: { session } } = await supabase.auth.getSession();
    if (session?.access_token) {
      return { 'Authorization': `Bearer ${session.access_token}` };
    }
  } catch {
    // ignore
  }

  // Fallback to local dev session if present
  const stored = localStorage.getItem('securevault_dev_user');
  if (stored) {
    try {
      const parsed = JSON.parse(stored);
      if (parsed.session?.access_token) {
        return { 'Authorization': `Bearer ${parsed.session.access_token}` };
      }
    } catch {
      // ignore
    }
  }

  return { 'Authorization': 'Bearer dev-mock-token-securevault' };
}

export async function apiRequest<T>(
  endpoint: string,
  options: ApiOptions = {}
): Promise<T> {
  const authHeaders = await getAuthHeaders();
  const { method = 'GET', body, headers = {}, signal } = options;

  const requestHeaders: Record<string, string> = {
    ...authHeaders,
    ...headers,
  };

  // Don't set Content-Type for FormData - browser handles it
  if (body && typeof body === 'string') {
    requestHeaders['Content-Type'] = 'application/json';
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    method,
    headers: requestHeaders,
    body,
    signal,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new ApiError(
      errorData.message || `Request failed with status ${response.status}`,
      response.status,
      errorData.error
    );
  }

  // Handle empty responses (204 No Content)
  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export async function apiDownload(
  endpoint: string,
  signal?: AbortSignal,
  method: string = 'GET'
): Promise<{ blob: Blob; filename: string }> {
  const authHeaders = await getAuthHeaders();

  const response = await fetch(`${API_BASE}${endpoint}`, {
    method,
    headers: authHeaders,
    signal,
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new ApiError(
      errorData.message || `Download failed with status ${response.status}`,
      response.status,
      errorData.error
    );
  }

  const blob = await response.blob();
  const contentDisposition = response.headers.get('Content-Disposition');
  let filename = 'download';
  if (contentDisposition) {
    const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
    if (match) {
      filename = match[1].replace(/['"]/g, '');
    }
  }

  return { blob, filename };
}

export async function apiUpload(
  endpoint: string,
  file: File,
  onProgress?: (progress: number) => void,
  signal?: AbortSignal
): Promise<unknown> {
  const authHeaders = await getAuthHeaders();
  const formData = new FormData();
  formData.append('file', file);

  // Use XMLHttpRequest for progress tracking
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();

    if (signal) {
      signal.addEventListener('abort', () => {
        xhr.abort();
        reject(new Error('Upload cancelled'));
      });
    }

    xhr.upload.addEventListener('progress', (event) => {
      if (event.lengthComputable && onProgress) {
        const progress = Math.round((event.loaded / event.total) * 100);
        onProgress(progress);
      }
    });

    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          resolve(JSON.parse(xhr.responseText));
        } catch {
          resolve(undefined);
        }
      } else {
        try {
          const errorData = JSON.parse(xhr.responseText);
          reject(new ApiError(
            errorData.message || `Upload failed with status ${xhr.status}`,
            xhr.status,
            errorData.error
          ));
        } catch {
          reject(new ApiError(`Upload failed with status ${xhr.status}`, xhr.status));
        }
      }
    });

    xhr.addEventListener('error', () => {
      reject(new ApiError('Network error during upload', 0));
    });

    xhr.open('POST', `${API_BASE}${endpoint}`);
    Object.entries(authHeaders).forEach(([key, value]) => {
      xhr.setRequestHeader(key, value);
    });
    xhr.send(formData);
  });
}

export class ApiError extends Error {
  status: number;
  code?: string;

  constructor(message: string, status: number, code?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}
