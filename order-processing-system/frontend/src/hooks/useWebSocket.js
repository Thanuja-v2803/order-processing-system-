import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

/**
 * Connects to the backend's STOMP-over-SockJS endpoint and subscribes
 * to one or more topics. Returns the latest message per topic plus the
 * connection status, and cleans up the subscription/client on unmount.
 *
 * @param {string[]} topics - e.g. ['/topic/orders']
 */
export default function useWebSocket(topics) {
  const [connected, setConnected] = useState(false);
  const [messages, setMessages] = useState({});
  const clientRef = useRef(null);

  const handleMessage = useCallback((topic, frame) => {
    try {
      const payload = JSON.parse(frame.body);
      setMessages((prev) => ({ ...prev, [topic]: payload }));
    } catch (err) {
      console.error('Failed to parse WS message', err);
    }
  }, []);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        topics.forEach((topic) => {
          client.subscribe(topic, (frame) => handleMessage(topic, frame));
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => {
        console.error('STOMP error', frame.headers['message'], frame.body);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
    // topics is expected to be a stable array (e.g. defined outside the
    // component or memoized) - re-subscribing on every render would
    // otherwise churn the connection.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { connected, messages };
}
