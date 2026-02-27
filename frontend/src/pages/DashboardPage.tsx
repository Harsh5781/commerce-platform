import { useEffect, useState } from 'react';
import { Package, TrendingUp, Clock, Truck, CheckCircle, XCircle } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import Header from '../components/Header';
import { dashboardApi } from '../api/dashboard';
import { DashboardStats } from '../types';

const CHANNEL_COLORS: Record<string, string> = {
  WEBSITE: '#6366f1',
  AMAZON: '#f59e0b',
  BLINKIT: '#10b981',
};

const PIE_COLORS = ['#eab308', '#6366f1', '#8b5cf6', '#a855f7', '#22c55e', '#ef4444', '#f97316', '#6b7280'];

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi.getStats()
      .then((res) => setStats(res.data.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div>
        <Header title="Dashboard" />
        <div className="p-8 flex justify-center">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600" />
        </div>
      </div>
    );
  }

  if (!stats) return null;

  const statCards = [
    { label: 'Total Orders',      value: stats.totalOrders,      icon: Package,     color: 'bg-indigo-500' },
    { label: 'Revenue',            value: `₹${stats.totalRevenue.toLocaleString()}`, icon: TrendingUp,  color: 'bg-green-500' },
    { label: 'Pending',            value: stats.pendingOrders,    icon: Clock,       color: 'bg-yellow-500' },
    { label: 'Shipped',            value: stats.shippedOrders,    icon: Truck,       color: 'bg-purple-500' },
    { label: 'Delivered',          value: stats.deliveredOrders,  icon: CheckCircle, color: 'bg-emerald-500' },
    { label: 'Cancelled',          value: stats.cancelledOrders,  icon: XCircle,     color: 'bg-red-500' },
  ];

  const channelChartData = stats.channelBreakdown.map((ch) => ({
    name: ch.channel,
    orders: ch.orderCount,
    revenue: ch.revenue,
    fill: CHANNEL_COLORS[ch.channel] || '#6b7280',
  }));

  const statusPieData = Object.entries(stats.statusBreakdown)
    .filter(([, v]) => v > 0)
    .map(([key, value]) => ({ name: key, value }));

  return (
    <div>
      <Header title="Dashboard" />
      <div className="p-8 space-y-8">
        {/* Stat cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          {statCards.map((card) => (
            <div key={card.label} className="bg-white rounded-xl shadow-sm border border-gray-100 p-5">
              <div className="flex items-center gap-3">
                <div className={`p-2.5 rounded-lg ${card.color}`}>
                  <card.icon className="h-5 w-5 text-white" />
                </div>
                <div>
                  <p className="text-sm text-gray-500">{card.label}</p>
                  <p className="text-xl font-bold text-gray-900">{card.value}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Time-based stats */}
        <div className="grid grid-cols-3 gap-4">
          {[
            { label: 'Orders Today', value: stats.ordersToday },
            { label: 'This Week', value: stats.ordersThisWeek },
            { label: 'This Month', value: stats.ordersThisMonth },
          ].map((item) => (
            <div key={item.label} className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 text-center">
              <p className="text-sm text-gray-500">{item.label}</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{item.value}</p>
            </div>
          ))}
        </div>

        {/* Charts */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="text-base font-semibold text-gray-900 mb-4">Orders by Channel</h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={channelChartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="orders" radius={[6, 6, 0, 0]}>
                  {channelChartData.map((entry, i) => (
                    <Cell key={i} fill={entry.fill} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h3 className="text-base font-semibold text-gray-900 mb-4">Status Distribution</h3>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={statusPieData} cx="50%" cy="50%" innerRadius={60} outerRadius={100}
                     paddingAngle={3} dataKey="value" label={({ name, value }) => `${name}: ${value}`}>
                  {statusPieData.map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Channel breakdown table */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-100">
            <h3 className="text-base font-semibold text-gray-900">Channel Breakdown</h3>
          </div>
          <table className="w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Channel</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Orders</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Revenue</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Pending</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Delivered</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {stats.channelBreakdown.map((ch) => (
                <tr key={ch.channel} className="hover:bg-gray-50">
                  <td className="px-6 py-4 text-sm font-medium text-gray-900 flex items-center gap-2">
                    <span className="w-3 h-3 rounded-full" style={{ backgroundColor: CHANNEL_COLORS[ch.channel] }} />
                    {ch.channel}
                  </td>
                  <td className="px-6 py-4 text-sm text-right text-gray-700">{ch.orderCount}</td>
                  <td className="px-6 py-4 text-sm text-right text-gray-700">₹{ch.revenue.toLocaleString()}</td>
                  <td className="px-6 py-4 text-sm text-right text-yellow-600 font-medium">{ch.pendingCount}</td>
                  <td className="px-6 py-4 text-sm text-right text-green-600 font-medium">{ch.deliveredCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
