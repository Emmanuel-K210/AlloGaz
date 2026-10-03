import { apiFetch } from './client';
import type { AuthTokens, OtpRequested, User } from './types';

export function requestOtp(phone: string) {
  return apiFetch<OtpRequested>('/auth/otp/request', { method: 'POST', body: { phone }, auth: false });
}

export function verifyOtp(phone: string, code: string) {
  return apiFetch<AuthTokens>('/auth/otp/verify', { method: 'POST', body: { phone, code }, auth: false });
}

export function logout(refreshToken: string) {
  return apiFetch<void>('/auth/logout', { method: 'POST', body: { refreshToken }, auth: false });
}

export function me() {
  return apiFetch<User>('/me');
}

export function renameMe(displayName: string) {
  return apiFetch<User>('/me', { method: 'PATCH', body: { displayName } });
}
