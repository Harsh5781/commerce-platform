export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errors?: Record<string, string>;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface UserInfo {
  id: string;
  email: string;
  name: string;
  role: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

export interface OrderItem {
  productName: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface Customer {
  name: string;
  email: string;
  phone: string;
}

export interface Address {
  line1: string;
  line2?: string;
  city: string;
  state: string;
  pincode: string;
  country: string;
}

export interface OrderTimeline {
  status: string;
  changedBy: string;
  notes: string;
  timestamp: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  channel: string;
  channelOrderRef: string;
  customer: Customer;
  items: OrderItem[];
  status: string;
  totalAmount: number;
  shippingAddress: Address;
  channelMetadata: Record<string, unknown>;
  timeline: OrderTimeline[];
  placedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface Channel {
  id: string;
  name: string;
  code: string;
  status: string;
  description: string;
  logoUrl: string;
  available: boolean;
  orderCount: number;
  createdAt: string;
}

export interface ChannelBreakdown {
  channel: string;
  orderCount: number;
  revenue: number;
  pendingCount: number;
  deliveredCount: number;
}

export interface DashboardStats {
  totalOrders: number;
  totalRevenue: number;
  pendingOrders: number;
  processingOrders: number;
  shippedOrders: number;
  deliveredOrders: number;
  cancelledOrders: number;
  channelBreakdown: ChannelBreakdown[];
  statusBreakdown: Record<string, number>;
  ordersToday: number;
  ordersThisWeek: number;
  ordersThisMonth: number;
}

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'RETURNED'
  | 'REFUNDED';
