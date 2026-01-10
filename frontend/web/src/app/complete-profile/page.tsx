'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function CompleteProfilePage() {
  const router = useRouter();

  // 1. Dajemy stany dla obu pól
  const [phoneNumber, setPhoneNumber] = useState('');
  const [username, setUsername] = useState('');
  const [isLoading, setIsLoading] = useState(false);


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    const token = localStorage.getItem('token');

    if (!token) {
        alert("Błąd: Brak tokena. Zaloguj się ponownie.");
        router.push('/login');
        return;
    }

    try {
      const res = await fetch('/api/user/editUserData', {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'X-USER-TOKEN': `Bearer ${token}`
        },
        body: JSON.stringify({
          username: username,
          phone_number: phoneNumber
        })
      });

      if (res.ok) {
        const data = await res.json();

        localStorage.setItem('name', data.username);
        localStorage.setItem('role', data.role);
        window.location.href = '/user_profile';
        return;
      } else {
        const errorText = await res.text();
        console.error("Błąd backendu:", errorText);
        alert(`Błąd zapisu danych: ${res.status}`);
      }
    } catch (err) {
      console.error(err);
      alert('Błąd połączenia z serwerem');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto mt-20 p-6 bg-white rounded-lg shadow-md">
      <h1 className="text-2xl font-bold mb-4">Uzupełnij profil</h1>
      <p className="mb-6 text-gray-600">Aby korzystać z aplikacji, podaj nazwę użytkownika oraz numer telefonu.</p>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Pole Username */}
        <div>
          <label className="block text-sm font-medium text-gray-700">Nazwa wyświetlana</label>
          <input
            type="text"
            required
            className="mt-1 block w-full rounded-md border border-gray-300 p-2"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Np. Jan Kowalski"
          />
        </div>

        {/* Pole Telefon */}
        <div>
          <label className="block text-sm font-medium text-gray-700">Numer telefonu</label>
          <input
            type="tel"
            required
            className="mt-1 block w-full rounded-md border border-gray-300 p-2"
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
            placeholder="+48 123 456 789"
          />
        </div>

        <button
          type="submit"
          disabled={isLoading}
          className="w-full bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700 disabled:bg-blue-300 transition-colors"
        >
          {isLoading ? 'Zapisywanie...' : 'Zapisz i wejdź'}
        </button>
      </form>
    </div>
  );
}