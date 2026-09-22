import { useEffect, useState, useCallback } from 'react';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export default function InventoryPanel() {
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchInventory = useCallback(async () => {
    try {
      setLoading(true);
      const res = await fetch(`${API_URL}/inventory`);
      if (!res.ok) throw new Error(`Request failed: ${res.status}`);
      setInventory(await res.json());
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchInventory();
    // Inventory isn't pushed over WebSocket in this setup, so poll it
    // at a modest interval instead.
    const id = setInterval(fetchInventory, 15000);
    return () => clearInterval(id);
  }, [fetchInventory]);

  if (loading) return <p>Loading inventory…</p>;
  if (error) return <p style={{ color: '#b71c1c' }}>Failed to load inventory: {error}</p>;

  return (
    <div>
      <h2>Inventory</h2>
      {inventory.length === 0 ? (
        <p>No inventory records.</p>
      ) : (
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {inventory.map((item) => {
            const low = item.availableQuantity < 10;
            return (
              <li
                key={item.productId}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  padding: '8px 4px',
                  borderBottom: '1px solid #f0f0f0',
                }}
              >
                <span>{item.productName}</span>
                <span style={{ color: low ? '#b71c1c' : '#333', fontWeight: low ? 600 : 400 }}>
                  {item.availableQuantity} available
                  {item.reservedQuantity ? ` (${item.reservedQuantity} reserved)` : ''}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
