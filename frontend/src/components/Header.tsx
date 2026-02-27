import { useAuth } from '../context/AuthContext';

export default function Header({ title }: { title: string }) {
  const { user } = useAuth();

  return (
    <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-8">
      <h1 className="text-xl font-semibold text-gray-900">{title}</h1>
      <div className="flex items-center gap-4">
        <span className="text-sm text-gray-500">
          Welcome, <span className="font-medium text-gray-900">{user?.name}</span>
        </span>
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800">
          {user?.role}
        </span>
      </div>
    </header>
  );
}
