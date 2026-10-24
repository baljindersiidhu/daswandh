import { useEffect, useState } from "react";
import { getHealth } from "../api/client";

export default function HomePage() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getHealth()
      .then(setHealth)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <main className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-white rounded-xl shadow p-6">
        <h1 className="text-3xl font-bold text-slate-900">Daswandh</h1>
        <p className="text-slate-600 mt-1">Every rupee, accounted for.</p>

        <div className="mt-6 text-sm">
          {error && <p className="text-red-600">Backend unreachable: {error}</p>}
          {!error && !health && <p className="text-slate-500">Checking backend…</p>}
          {health && (
            <ul className="space-y-1">
              <li>Backend: <b>{health.status}</b></li>
              <li>Database: <b>{health.database}</b></li>
            </ul>
          )}
        </div>
      </div>
    </main>
  );
}