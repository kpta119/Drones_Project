"use client";

import { useState } from "react";
import { API_URL } from '../app/config';

export default function LoginForm() {
  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError("");
    setIsLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: loginEmail, password: loginPassword }),
      });

      if (res.status === 200) {
        const data = await res.json();
        localStorage.setItem("token", data.token);
        localStorage.setItem("role", data.role);
        localStorage.setItem("userId", data.userId);
        localStorage.setItem("name", loginEmail);
        window.location.reload();
      } else if (res.status === 401) {
        setLoginError("Nieprawidłowe hasło");
      } else if (res.status === 403) {
        setLoginError("Konto zablokowane");
      } else {
        setLoginError("Błąd logowania");
      }
    } catch {
      setLoginError("Błąd sieci");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md bg-white rounded-2xl shadow-lg border border-gray-200 p-8">
      <h2 className="text-2xl font-semibold mb-6 text-center text-gray-800">
        Logowanie
      </h2>
      <form onSubmit={handleLogin} className="flex flex-col gap-4">
        <input
          type="email"
          placeholder="Email"
          value={loginEmail}
          onChange={(e) => setLoginEmail(e.target.value)}
          className="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-400 transition"
          required
          disabled={isLoading}
        />
        <input
          type="password"
          placeholder="Hasło"
          value={loginPassword}
          onChange={(e) => setLoginPassword(e.target.value)}
          className="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-400 transition"
          required
          disabled={isLoading}
        />
        {loginError && (
          <p className="text-red-500 text-sm mt-2">{loginError}</p>
        )}

        <button
          type="submit"
          disabled={isLoading}
          className="mt-4 bg-linear-to-r from-primary-600 to-primary-700 text-white font-semibold p-3 rounded-lg active:ring-0 hover:ring-2 hover:ring-primary-300 hover:shadow-lg transition-all disabled:opacity-50"
        >
          {isLoading ? "Logowanie..." : "Zaloguj"}
        </button>
      </form>
    </div>
  );
}
