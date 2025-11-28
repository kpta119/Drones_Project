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
                window.location.reload();
            } else if (res.status === 401) {
                setLoginError('Nieprawidłowe hasło');
            } else if (res.status === 403) {
                setLoginError('Konto zablokowane');
            } else {
                setLoginError('Błąd logowania');
            }
        } catch (err) {
            setLoginError('Błąd sieci');
        }
    };
    return (
        <div style={{ display: 'flex', gap: '2rem', padding: '2rem' }}>
            <form onSubmit={handleLogin} style={{ flex: 1 }}>
                <h2>Logowanie</h2>
                <input
                    type="email"
                    placeholder="Email"
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    required
                />
                <br />
                <input
                    type="password"
                    placeholder="Hasło"
                    value={loginPassword}
                    onChange={(e) => setLoginPassword(e.target.value)}
                    required
                />
                <br />
                <button type="submit">Zaloguj</button>
                {loginError && <p style={{ color: 'red' }}>{loginError}</p>}
            </form>
        </div>
    );
}
