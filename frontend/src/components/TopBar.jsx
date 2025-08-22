// src/components/TopBar.jsx
import {useNavigate, useParams} from "react-router-dom";
import {useNotifications} from "../context/NotificationContext";

export default function TopBar({title = "My Stock App"}) {
  const nav = useNavigate();
  const { ticker } = useParams();
  const { items, unread, open, setOpen, markAllRead, markRead, userKey } = useNotifications();

  const goBack = async () => {
    try {
      if (ticker) {
        await fetch(`/api/stocks/subscribe/${ticker}`, {
          method: "DELETE",
          headers: { "X-User-Key": userKey }
        }).catch(()=>{});
      }
    } finally {
      nav("/");
    }
  };

  return (
    <div style={styles.bar}>
      <button style={styles.back} onClick={goBack} aria-label="Back">← Back</button>

      <div style={styles.title}>{title}</div>

      <div style={styles.bellWrap}>
        <button style={styles.bellBtn} onClick={() => setOpen(v => !v)} aria-label="Notifications">
          🔔{unread > 0 && <span style={styles.badge}>{unread}</span>}
        </button>
        {open && (
          <div style={styles.panel}>
            <div style={styles.panelHeader}>
              <span>Notifications</span>
              <button style={styles.small} onClick={markAllRead}>Mark all read</button>
            </div>
            <div style={styles.list}>
              {items.length === 0 && <div style={styles.empty}>No notifications yet</div>}
              {items.map((n, i) => (
                <div key={n.id ?? i} style={{...styles.item, borderLeft: `4px solid ${levelColor(n.level)}`}}>
                  <div style={styles.itemTitle}>
                    <span>{n.title}</span>
                    <span style={styles.level(n.level)}>{n.level}</span>
                  </div>
                  <div style={styles.msg}>{n.message}</div>
                  <div style={styles.meta}>
                    <span style={styles.time}>{formatTime(n.createdAt)}</span>
                    {!n.readFlag && (
                      <button style={styles.small} onClick={() => markRead(n.id)}>Mark read</button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function levelColor(level){
  switch(String(level||"").toUpperCase()){
    case "ERROR": return "#ef4444";
    case "WARN": return "#f59e0b";
    default: return "#3b82f6";
  }
}
function formatTime(iso){
  if(!iso) return "";
  try { return new Date(iso).toLocaleString(); } catch { return ""; }
}

const styles = {
  bar: {
    position: "sticky", top: 0, zIndex: 50,
    width: "100%", height: 56, display: "flex", alignItems: "center",
    padding: "0 16px", background: "#0b0f17", borderBottom: "1px solid #1e293b"
  },
  back: {
    background: "transparent", border: "1px solid #334155", color: "#e2e8f0",
    padding: "6px 10px", borderRadius: 8, cursor: "pointer"
  },
  title: { flex: 1, textAlign: "center", color: "#e2e8f0", fontSize: 18, fontWeight: 700 },
  bellWrap: { position: "relative" },
  bellBtn: {
    position: "relative", background: "transparent", border: "1px solid #334155",
    color: "#e2e8f0", padding: "6px 10px", borderRadius: 999, cursor: "pointer"
  },
  badge: {
    position: "absolute", top: -6, right: -6,
    background: "#ef4444", color: "white", borderRadius: 999, fontSize: 10, padding: "2px 5px"
  },
  panel: {
    position: "absolute", right: 0, marginTop: 8, width: 380,
    background: "#0f172a", border: "1px solid #334155", borderRadius: 12, boxShadow: "0 8px 24px rgba(0,0,0,.35)"
  },
  panelHeader: {
    display: "flex", justifyContent: "space-between", alignItems: "center",
    padding: "10px 12px", borderBottom: "1px solid #1f2937", color: "#e5e7eb"
  },
  small: { fontSize: 12, background: "transparent", color: "#93c5fd", border: "none", cursor: "pointer" },
  list: { maxHeight: 380, overflowY: "auto" },
  empty: { padding: 16, color: "#94a3b8" },
  item: { padding: "10px 12px", borderBottom: "1px solid #1f2937" },
  itemTitle: { display: "flex", justifyContent: "space-between", color: "#e5e7eb", fontWeight: 600 },
  level: (l)=>({ fontSize: 11, color: levelColor(l) }),
  msg: { color: "#cbd5e1", marginTop: 4 },
  meta: { display:"flex", justifyContent:"space-between", alignItems:"center", marginTop: 6 },
  time: { color: "#64748b", fontSize: 12 }
};
