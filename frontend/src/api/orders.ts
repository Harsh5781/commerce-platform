import client from './client';
import { ApiResponse, Order, PageResponse } from '../types';

export interface OrderFilters {
  page?: number;
  size?: number;
  sort?: string;
  direction?: string;
  channel?: string;
  status?: string;
  search?: string;
  startDate?: string;
  endDate?: string;
}

export const ordersApi = {
  list: (filters: OrderFilters = {}) =>
    client.get<ApiResponse<PageResponse<Order>>>('/orders', { params: filters }),

  getById: (id: string) =>
    client.get<ApiResponse<Order>>(`/orders/${id}`),

  getByNumber: (orderNumber: string) =>
    client.get<ApiResponse<Order>>(`/orders/number/${orderNumber}`),

  create: (data: unknown) =>
    client.post<ApiResponse<Order>>('/orders', data),

  updateStatus: (id: string, status: string, notes?: string) =>
    client.patch<ApiResponse<Order>>(`/orders/${id}/status`, { status, notes }),
};
