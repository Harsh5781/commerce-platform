import http from 'k6/http';
import { BASE_URL } from './config.js';

/**
 * Logs in with the given credentials and returns the access token.
 * @param {Object} credentials - { email, password }
 * @returns {string|null} Access token or null on failure
 */
export function login(credentials) {
  const url = `${BASE_URL}/api/auth/login`;
  const payload = JSON.stringify({
    email: credentials.email,
    password: credentials.password,
  });
  const res = http.post(url, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (res.status !== 200) {
    return null;
  }

  try {
    const body = JSON.parse(res.body);
    return body.data?.accessToken || null;
  } catch {
    return null;
  }
}

/**
 * Returns headers for authenticated API requests.
 * @param {string} token - JWT access token
 * @returns {Object} Headers object with Authorization and Content-Type
 */
export function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
}
