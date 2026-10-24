// One place that knows the backend URL. Every later API call goes through here.
const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function getHealth() {
  const res = await fetch(`${API_URL}/api/health`);
  if (!res.ok) throw new Error(`Backend responded with ${res.status}`);
  return res.json();
}