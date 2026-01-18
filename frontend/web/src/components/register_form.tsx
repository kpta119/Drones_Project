"use client";

import { useState } from "react";

export default function RegisterForm() {
  const [regUsername, setRegUsername] = useState("");
  const [regPassword, setRegPassword] = useState("");
  const [regName, setRegName] = useState("");
  const [regSurname, setRegSurname] = useState("");
  const [regEmail, setRegEmail] = useState("");
  const [regPhone, setRegPhone] = useState("");
  const [regMessage, setRegMessage] = useState("");

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setRegMessage("");
    try {
      const res = await fetch(`/api/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: regUsername,
          password: regPassword,
          name: regName,
          surname: regSurname,
          email: regEmail,
          phone_number: regPhone,
        }),
      });

      if (res.status === 201) {
        setRegMessage("Rejestracja zakończona sukcesem!");
      } else {
        const data = await res.json();
        setRegMessage(`Błąd: ${data.message || res.status}`);
      }
    } catch {
      setRegMessage("Błąd sieci");
    }
  };

  return (
    <div className="p-8 max-w-md mx-auto bg-white rounded-2xl shadow-md border border-gray-200">
      <h2 className="text-2xl font-semibold mb-6 text-center">Rejestracja</h2>
      <form
        onSubmit={handleRegister}
        className="flex flex-col h-full gap-4"
        style={{ minHeight: "400px" }}
      >
        <div className="flex flex-col gap-4">
          <input
            type="text"
            placeholder="Nazwa użytkownika"
            value={regUsername}
            onChange={(e) => setRegUsername(e.target.value)}
            className="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-400 transition"
            required
          />
          <input
            type="password"
            placeholder="Hasło"
            value={regPassword}
            onChange={(e) => setRegPassword(e.target.value)}
            className="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-400 transition"
            required
          />
          <input
            type="text"
            placeholder="Imię"
            value={regName}
            onChange={(e) => setRegName(e.target.value)}
            className="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-400 transition"
            required
          />
          <input
            type="text"
            placeholder="Nazwisko"
            value={regSurname}
            onChange={(e) => setRegSurname(e.target.value)}
            className="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-400 transition"
            required
          />
          <input
            type="email"
            placeholder="Email"
            value={regEmail}
            onChange={(e) => setRegEmail(e.target.value)}
            className="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-400 transition"
            required
          />
          <input
            type="tel"
            placeholder="Numer telefonu"
            value={regPhone}
            onChange={(e) => setRegPhone(e.target.value)}
            className="p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-400 transition"
            required
          />
          {regMessage && (
            <p
              className={`text-center text-sm ${
                regMessage.includes("sukces")
                  ? "text-green-500"
                  : "text-red-500"
              }`}
            >
              {regMessage}
            </p>
          )}
        </div>
        <button
          type="submit"
          className="mt-4 bg-linear-to-r from-primary-600 to-primary-700 active:ring-0 text-white font-semibold p-3 rounded-lg hover:ring-2 hover:ring-primary-300 hover:shadow-lg transition-all disabled:opacity-50"
        >
          Zarejestruj
        </button>
      </form>
    </div>
  );
}
