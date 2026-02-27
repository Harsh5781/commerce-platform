import { useEffect, useState } from 'react';
import { RefreshCw, Wifi, WifiOff } from 'lucide-react';
import Header from '../components/Header';
import StatusBadge from '../components/StatusBadge';
import { channelsApi } from '../api/channels';
import { useAuth } from '../context/AuthContext';
import { Channel } from '../types';
import toast from 'react-hot-toast';

const CHANNEL_ICONS: Record<string, string> = {
  WEBSITE: '🌐',
  AMAZON: '📦',
  BLINKIT: '⚡',
};

const CHANNEL_COLORS: Record<string, string> = {
  WEBSITE: 'border-indigo-200 bg-gradient-to-br from-indigo-50 to-white',
  AMAZON: 'border-amber-200 bg-gradient-to-br from-amber-50 to-white',
  BLINKIT: 'border-emerald-200 bg-gradient-to-br from-emerald-50 to-white',
};

export default function ChannelsPage() {
  const { isManager } = useAuth();
  const [channels, setChannels] = useState<Channel[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState<string | null>(null);

  const fetchChannels = () => {
    channelsApi.list()
      .then((res) => setChannels(res.data.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchChannels();
  }, []);

  const handleSync = async (code: string) => {
    setSyncing(code);
    try {
      const res = await channelsApi.sync(code);
      const count = res.data.data;
      toast.success(`Synced ${count} orders from ${code}`);
      fetchChannels();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Sync failed';
      toast.error(msg);
    } finally {
      setSyncing(null);
    }
  };

  if (loading) {
    return (
      <div>
        <Header title="Channels" />
        <div className="p-8 flex justify-center">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600" />
        </div>
      </div>
    );
  }

  return (
    <div>
      <Header title="Sales Channels" />
      <div className="p-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {channels.map((channel) => (
            <div key={channel.id}
              className={`rounded-xl shadow-sm border-2 p-6 ${CHANNEL_COLORS[channel.code] || 'border-gray-200 bg-white'}`}>
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <span className="text-3xl">{CHANNEL_ICONS[channel.code] || '📌'}</span>
                  <div>
                    <h3 className="text-lg font-bold text-gray-900">{channel.name}</h3>
                    <p className="text-sm text-gray-500">{channel.description}</p>
                  </div>
                </div>
                <div className="flex items-center gap-1.5">
                  {channel.available ? (
                    <Wifi className="h-4 w-4 text-green-500" />
                  ) : (
                    <WifiOff className="h-4 w-4 text-red-500" />
                  )}
                  <StatusBadge status={channel.status} />
                </div>
              </div>

              <div className="mt-6 grid grid-cols-2 gap-4">
                <div className="bg-white/70 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-gray-900">{channel.orderCount}</p>
                  <p className="text-xs text-gray-500 mt-1">Total Orders</p>
                </div>
                <div className="bg-white/70 rounded-lg p-3 text-center">
                  <p className="text-2xl font-bold text-gray-900">
                    {channel.available ? '✓' : '✗'}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">API Status</p>
                </div>
              </div>

              {isManager && (
                <button
                  onClick={() => handleSync(channel.code)}
                  disabled={syncing === channel.code || !channel.available}
                  className="mt-4 w-full flex items-center justify-center gap-2 py-2.5 bg-white border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <RefreshCw className={`h-4 w-4 ${syncing === channel.code ? 'animate-spin' : ''}`} />
                  {syncing === channel.code ? 'Syncing...' : 'Sync Orders'}
                </button>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
