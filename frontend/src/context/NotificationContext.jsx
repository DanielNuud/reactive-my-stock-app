import React, {createContext, useContext, useEffect, useRef, useState} from "react";
import {ensureUserKey} from "../hooks/useUserKey";

const Ctx = createContext(null);
export const useNotifications = () => useContext(Ctx);

export function NotificationsProvider({children}) {
  const userKey = ensureUserKey();
  const [items, setItems] = useState([]);
  const [open, setOpen] = useState(false);
  const lastSinceRef = useRef(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const qs = new URLSearchParams({ userKey });
        if (lastSinceRef.current) qs.set("since", lastSinceRef.current);

        const res = await fetch(`/api/notifications?${qs.toString()}`);
        if (!res.ok) return;
        const fresh = await res.json();

        if (!cancelled && Array.isArray(fresh) && fresh.length) {
          setItems(prev => {
            const known = new Set(prev.map(n => n.id));
            const merged = [...fresh.filter(n => !known.has(n.id)), ...prev];
            return merged.sort((a,b) => new Date(b.createdAt) - new Date(a.createdAt));
          });
          const maxIso = fresh.map(n => n.createdAt).filter(Boolean).sort().slice(-1)[0];
          if (maxIso) lastSinceRef.current = maxIso;
        }
        if (!lastSinceRef.current) lastSinceRef.current = new Date().toISOString();
      } catch (_) {}
    }

    load();
    const id = setInterval(load, 12000);
    return () => { cancelled = true; clearInterval(id); };
  }, [userKey]);

  const unread = items.filter(n => !n.readFlag).length;

  const markRead = async (id) => {
    try { await fetch(`/api/notifications/${id}/read`, { method: "PATCH" }); } catch {}
    setItems(prev => prev.map(n => n.id === id ? {...n, readFlag: true} : n));
  };

  const markAllRead = async () => {
    const ids = items.filter(n => !n.readFlag).map(n => n.id);
    await Promise.all(ids.map(id => fetch(`/api/notifications/${id}/read`, { method: "PATCH" }).catch(()=>{})));
    setItems(prev => prev.map(n => ({...n, readFlag: true})));
  };

  return (
    <Ctx.Provider value={{ items, unread, open, setOpen, markRead, markAllRead, userKey }}>
      {children}
    </Ctx.Provider>
  );
}
