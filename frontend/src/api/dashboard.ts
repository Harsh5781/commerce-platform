import client from './client';
import { ApiResponse, DashboardStats } from '../types';

export const dashboardApi = {
  getStats: () =>
    client.get<ApiResponse<DashboardStats>>('/dashboard/stats'),
};
