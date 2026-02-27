const statusConfig: Record<string, { bg: string; text: string }> = {
  PENDING:    { bg: 'bg-yellow-100', text: 'text-yellow-800' },
  CONFIRMED:  { bg: 'bg-blue-100',   text: 'text-blue-800' },
  PROCESSING: { bg: 'bg-indigo-100', text: 'text-indigo-800' },
  SHIPPED:    { bg: 'bg-purple-100', text: 'text-purple-800' },
  DELIVERED:  { bg: 'bg-green-100',  text: 'text-green-800' },
  CANCELLED:  { bg: 'bg-red-100',    text: 'text-red-800' },
  RETURNED:   { bg: 'bg-orange-100', text: 'text-orange-800' },
  REFUNDED:   { bg: 'bg-gray-100',   text: 'text-gray-800' },
  ACTIVE:     { bg: 'bg-green-100',  text: 'text-green-800' },
  INACTIVE:   { bg: 'bg-gray-100',   text: 'text-gray-800' },
  MAINTENANCE:{ bg: 'bg-yellow-100', text: 'text-yellow-800' },
};

export default function StatusBadge({ status }: { status: string }) {
  const config = statusConfig[status] || { bg: 'bg-gray-100', text: 'text-gray-800' };
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.bg} ${config.text}`}>
      {status}
    </span>
  );
}
