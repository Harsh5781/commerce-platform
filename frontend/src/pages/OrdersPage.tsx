import { useEffect, useState, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Search, Filter, ChevronLeft, ChevronRight } from 'lucide-react';
import Header from '../components/Header';
import StatusBadge from '../components/StatusBadge';
import { ordersApi, OrderFilters } from '../api/orders';
import { Order, PageResponse } from '../types';

const CHANNELS = ['', 'WEBSITE', 'AMAZON', 'BLINKIT'];
const STATUSES = ['', 'PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED', 'REFUNDED'];

export default function OrdersPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [data, setData] = useState<PageResponse<Order> | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState(searchParams.get('search') || '');
  const [channel, setChannel] = useState(searchParams.get('channel') || '');
  const [status, setStatus] = useState(searchParams.get('status') || '');
  const [page, setPage] = useState(Number(searchParams.get('page')) || 0);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const filters: OrderFilters = {
        page,
        size: 20,
        sort: 'placedAt',
        direction: 'DESC',
      };
      if (channel) filters.channel = channel;
      if (status) filters.status = status;
      if (search) filters.search = search;

      const res = await ordersApi.list(filters);
      setData(res.data.data);
    } catch {
      // handled by interceptor
    } finally {
      setLoading(false);
    }
  }, [page, channel, status, search]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  useEffect(() => {
    const params: Record<string, string> = {};
    if (search) params.search = search;
    if (channel) params.channel = channel;
    if (status) params.status = status;
    if (page > 0) params.page = String(page);
    setSearchParams(params, { replace: true });
  }, [search, channel, status, page, setSearchParams]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchOrders();
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
  };

  const channelBadge = (ch: string) => {
    const colors: Record<string, string> = {
      WEBSITE: 'bg-indigo-50 text-indigo-700',
      AMAZON: 'bg-amber-50 text-amber-700',
      BLINKIT: 'bg-emerald-50 text-emerald-700',
    };
    return (
      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${colors[ch] || 'bg-gray-50 text-gray-700'}`}>
        {ch}
      </span>
    );
  };

  return (
    <div>
      <Header title="Orders" />
      <div className="p-8 space-y-6">
        {/* Filters */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
          <form onSubmit={handleSearch} className="flex flex-wrap items-center gap-4">
            <div className="relative flex-1 min-w-[250px]">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search by order #, customer name, email..."
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none"
              />
            </div>
            <div className="flex items-center gap-2">
              <Filter className="h-4 w-4 text-gray-400" />
              <select value={channel} onChange={(e) => { setChannel(e.target.value); setPage(0); }}
                className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 outline-none">
                <option value="">All Channels</option>
                {CHANNELS.filter(Boolean).map((ch) => <option key={ch} value={ch}>{ch}</option>)}
              </select>
              <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}
                className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 outline-none">
                <option value="">All Statuses</option>
                {STATUSES.filter(Boolean).map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            <button type="submit"
              className="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition">
              Search
            </button>
          </form>
        </div>

        {/* Table */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          {loading ? (
            <div className="p-12 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Order #</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Channel</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Customer</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                      <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Amount</th>
                      <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {data?.content.map((order) => (
                      <tr key={order.id} onClick={() => navigate(`/orders/${order.id}`)}
                        className="hover:bg-gray-50 cursor-pointer transition">
                        <td className="px-6 py-4 text-sm font-medium text-indigo-600">{order.orderNumber}</td>
                        <td className="px-6 py-4">{channelBadge(order.channel)}</td>
                        <td className="px-6 py-4">
                          <div className="text-sm font-medium text-gray-900">{order.customer.name}</div>
                          <div className="text-xs text-gray-500">{order.customer.email}</div>
                        </td>
                        <td className="px-6 py-4"><StatusBadge status={order.status} /></td>
                        <td className="px-6 py-4 text-sm text-right font-medium text-gray-900">₹{order.totalAmount.toLocaleString()}</td>
                        <td className="px-6 py-4 text-sm text-gray-500">{formatDate(order.placedAt)}</td>
                      </tr>
                    ))}
                    {data?.content.length === 0 && (
                      <tr>
                        <td colSpan={6} className="px-6 py-12 text-center text-gray-500">No orders found</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {data && data.totalPages > 1 && (
                <div className="px-6 py-4 border-t border-gray-100 flex items-center justify-between">
                  <p className="text-sm text-gray-500">
                    Showing {data.page * data.size + 1} - {Math.min((data.page + 1) * data.size, data.totalElements)} of {data.totalElements}
                  </p>
                  <div className="flex gap-2">
                    <button onClick={() => setPage(Math.max(0, page - 1))} disabled={data.first}
                      className="p-2 rounded-lg border border-gray-300 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition">
                      <ChevronLeft className="h-4 w-4" />
                    </button>
                    <span className="flex items-center px-3 text-sm text-gray-700">
                      Page {data.page + 1} of {data.totalPages}
                    </span>
                    <button onClick={() => setPage(page + 1)} disabled={data.last}
                      className="p-2 rounded-lg border border-gray-300 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition">
                      <ChevronRight className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
