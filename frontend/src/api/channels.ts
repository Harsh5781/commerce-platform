import client from './client';
import { ApiResponse, Channel } from '../types';

export const channelsApi = {
  list: () =>
    client.get<ApiResponse<Channel[]>>('/channels'),

  getByCode: (code: string) =>
    client.get<ApiResponse<Channel>>(`/channels/${code}`),

  sync: (code: string) =>
    client.post<ApiResponse<number>>(`/channels/${code}/sync`),
};
