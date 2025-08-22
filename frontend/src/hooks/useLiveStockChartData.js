import { useEffect, useRef } from "react";

const API = "http://localhost:8080";
const WS_URL = "ws://localhost:8080/ws/prices";

/**
 * Держит одно WS-соединение и кладёт приходящие бары в setLatestData.
 * REST-подписка/отписка выполняется при смене ticker.
 */
export default function useLiveStockChartData(ticker, setLatestData) {
    const wsRef = useRef(null);
    const stopRef = useRef(false);
    const backoffRef = useRef(1000);
    const lastTickerRef = useRef(null);

    useEffect(() => {
        stopRef.current = false;

        const connect = () => {
            const ws = new WebSocket(WS_URL);
            wsRef.current = ws;

            ws.onopen = () => {
                backoffRef.current = 1000;
            };

            ws.onmessage = (e) => {
                try {
                    const dto = JSON.parse(e.data); // {ticker, price, timestamp}
                    setLatestData?.(dto);
                } catch (_) {}
            };

            ws.onerror = () => ws.close();
            ws.onclose = () => {
                if (stopRef.current) return;
                const delay = Math.min(backoffRef.current, 15000);
                setTimeout(connect, delay);
                backoffRef.current = Math.min(delay * 2, 15000);
            };
        };

        connect();
        return () => {
            stopRef.current = true;
            try { wsRef.current?.close(); } catch {}
        };
    }, [setLatestData]);

    useEffect(() => {
        if (!ticker) return;
        lastTickerRef.current = ticker;
        fetch(`${API}/api/stocks/subscribe/${encodeURIComponent(ticker)}`, { method: "POST" }).catch(() => {});
        return () => {
            const t = lastTickerRef.current;
            if (t) fetch(`${API}/api/stocks/unsubscribe/${encodeURIComponent(t)}`, { method: "POST" }).catch(() => {});
        };
    }, [ticker]);
}
