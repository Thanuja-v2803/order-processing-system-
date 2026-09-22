import OrderList from './components/OrderList';
import InventoryPanel from './components/InventoryPanel';

export default function App() {
  return (
    <div style={{ maxWidth: 960, margin: '0 auto', padding: 24, fontFamily: 'sans-serif' }}>
      <h1>Order Processing System</h1>
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 32 }}>
        <OrderList />
        <InventoryPanel />
      </div>
    </div>
  );
}
