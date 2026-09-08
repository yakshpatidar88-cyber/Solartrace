import { useEffect, useState } from 'react';

export function useWebSocket() {
  const [isConnected, setIsConnected] = useState<boolean>(true);
  const [lastMessageTime, setLastMessageTime] = useState<Date | null>(new Date());

  useEffect(() => {
    const interval = setInterval(() => {
      setLastMessageTime(new Date());
    }, 30000);

    return () => clearInterval(interval);
  }, []);

  return { isConnected, lastMessageTime };
}
