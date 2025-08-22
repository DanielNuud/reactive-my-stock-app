import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";

const useLiveConvertedPrice = (ticker, currency) => {
  const [price, setPrice] = useState(null);
  const [timestamp, setTimestamp] = useState(null);

  useEffect(() => {
    if (!ticker || !currency) return;

    const client = new Client({
      brokerURL: "ws://localhost:8080/ws/stocks",
      reconnectDelay: 5000,
      debug: (str) => console.log(str),
    });

    client.onConnect = () => {
      console.log("Connected to converted price WebSocket");

      client.subscribe(`/topic/stocks/${ticker}/${currency}`, (message) => {
        const data = JSON.parse(message.body);
        setPrice(data.price);
        setTimestamp(data.timestamp);
      });
    };

    client.activate();

    return () => {
      console.log("Disconnecting converted price WebSocket");
      client.deactivate();
    };
  }, [ticker, currency]);

  return { price, timestamp };
};

export default useLiveConvertedPrice;
