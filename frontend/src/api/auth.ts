import client from './client';
import { ApiResponse, LoginResponse, UserInfo } from '../types';

export const authApi = {
  login: (email: string, password: string) =>
    client.post<ApiResponse<LoginResponse>>('/auth/login', { email, password }),

  register: (data: { email: string; password: string; name: string; role: string }) =>
    client.post<ApiResponse<UserInfo>>('/auth/register', data),

  me: () =>
    client.get<ApiResponse<UserInfo>>('/auth/me'),

  refresh: (refreshToken: string) =>
    client.post<ApiResponse<LoginResponse>>('/auth/refresh', { refreshToken }),
};
