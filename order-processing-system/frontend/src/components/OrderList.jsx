import { useEffect, useState, useCallback } from 'react';
import useWebSocket from '../hooks/useWebSocket';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const STATUS_STYLES = {
  PENDING: { background: '#fff8e1', color: '#8a6100' },
  PROCESSING: { background: '#e3f2fd', color: '#0d47a1' },
  CONFIRMED: { background: '#e8f5e9', color: '#1b5e20' },
  FAILED: { background: '#ffebee', color: '#b71c1c' },
  CANCELLED: { background: '#f5f5f5', color: '#616161' },
};

function StatusBadge({ status }) {
  const style = STATUS_STYLES[status] || STATUS_STYLES.PENDING;
  return (
    <span
      style={{
        ...style,
        padding: '2px 10px',
        borderRadius: 12,
        fontSize: 12,
        fontWeight: 600,
      }}
    >
      {status}
    </span>
  );
}

export default function OrderList() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const { messages, connected } = useWebSocket(['/topic/orders']);

  const fetchOrders = useCallback(async () => {
    try {
      setLoading(true);
      const res = await fetch(`${API_URL}/orders`);
      if (!res.ok) throw new Error(`Request failed: ${res.status}`);
      const data = await res.json();
      setOrders(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // Patch the matching order in place whenever a status event arrives
  // over the /topic/orders broadcast, instead of refetching the list.
  useEffect(() => {
    const event = messages['/topic/orders'];
    if (!event) return;
    setOrders((prev) =>
      prev.map((o) => (o.id === event.orderId ? { ...o, status: event.status } : o))
    );
  }, [messages]);

  if (loading) return <p>Loading orders…</p>;
  if (error) return <p style={{ color: '#b71c1c' }}>Failed to load orders: {error}</p>;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Orders</h2>
        <span style={{ fontSize: 12, color: connected ? '#1b5e20' : '#b71c1c' }}>
          {connected ? '● live' : '○ disconnected'}
        </span>
      </div>

      {orders.length === 0 ? (
        <p>No orders yet.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ textAlign: 'left', borderBottom: '1px solid #ddd' }}>
              <th style={{ padding: '8px 4px' }}>ID</th>
              <th style={{ padding: '8px 4px' }}>Customer</th>
              <th style={{ padding: '8px 4px' }}>Items</th>
              <th style={{ padding: '8px 4px' }}>Total</th>
              <th style={{ padding: '8px 4px' }}>Status</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id} style={{ borderBottom: '1px solid #f0f0f0' }}>
                <td style={{ padding: '8px 4px' }}>#{order.id}</td>
                <td style={{ padding: '8px 4px' }}>{order.customerName}</td>
                <td style={{ padding: '8px 4px' }}>
                  {order.items?.map((i) => `${i.productName} x${i.quantity}`).join(', ')}
                </td>
                <td style={{ padding: '8px 4px' }}>${order.totalAmount}</td>
                <td style={{ padding: '8px 4px' }}>
                  <StatusBadge status={order.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
