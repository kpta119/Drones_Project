'use client';

import { useState } from 'react';

export default function LoginForm() {
    const [loginEmail, setLoginEmail] = useState('');
    const [loginPassword, setLoginPassword] = useState('');
    const [loginError, setLoginError] = useState('');

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoginError('');
        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: loginEmail, password: loginPassword }),
            });

            if (res.status === 200) {
                const data = await res.json();
                localStorage.setItem('token', data.token);
                localStorage.setItem('role', data.role);
                localStorage.setItem('name', loginEmail);
                window.location.reload();
            } else if (res.status === 401) {
                setLoginError('Nieprawidłowe hasło');
            } else if (res.status === 403) {
                setLoginError('Konto zablokowane');
            } else {
                setLoginError('Błąd logowania');
            }
        } catch {
            setLoginError('Błąd sieci');
        }
    };

    return (
        <div className="p-8 max-w-md mx-auto bg-white rounded-2xl shadow-md border border-gray-200">
            <h2 className="text-2xl font-semibold mb-6 text-center">Logowanie</h2>
            <form
                onSubmit={handleLogin}
                className="flex flex-col h-full gap-4"
                style={{ minHeight: '250px' }}
            >
                <div className="flex flex-col gap-4">
                    <input
                        type="email"
                        placeholder="Email"
                        value={loginEmail}
                        onChange={(e) => setLoginEmail(e.target.value)}
                        className="p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
                        required
                    />
                    <input
                        type="password"
                        placeholder="Hasło"
                        value={loginPassword}
                        onChange={(e) => setLoginPassword(e.target.value)}
                        className="p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
                        required
                    />
                    {loginError && <p className="text-red-500 text-sm">{loginError}</p>}
                </div>

                <button
                    type="submit"
                    className="mt-auto bg-yellow-500 text-white p-3 rounded-lg hover:bg-yellow-600 transition"
                >
                    Zaloguj
                </button>
            </form>
        </div>
    );
}
