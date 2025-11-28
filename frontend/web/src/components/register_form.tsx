'use client';

import { useState } from 'react';

export default function RegisterForm() {
    const [regUsername, setRegUsername] = useState('');
    const [regPassword, setRegPassword] = useState('');
    const [regName, setRegName] = useState('');
    const [regSurname, setRegSurname] = useState('');
    const [regEmail, setRegEmail] = useState('');
    const [regPhone, setRegPhone] = useState('');
    const [regMessage, setRegMessage] = useState('');

    const handleRegister = async (e: React.FormEvent) => {
        e.preventDefault();
        setRegMessage('');
        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
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
                setRegMessage('Rejestracja zakończona sukcesem!');
            } else {
                const data = await res.json();
                setRegMessage(`Błąd: ${data.message || res.status}`);
            }
        } catch (err) {
            setRegMessage('Błąd sieci');
        }
    };

    return (
        <div style={{ display: 'flex', gap: '2rem', padding: '2rem' }}>
            <form onSubmit={handleRegister} style={{ flex: 1 }}>
                <h2>Rejestracja</h2>
                <input
                    type="text"
                    placeholder="Nazwa użytkownika"
                    value={regUsername}
                    onChange={(e) => setRegUsername(e.target.value)}
                    required
                />
                <br />
                <input
                    type="password"
                    placeholder="Hasło"
                    value={regPassword}
                    onChange={(e) => setRegPassword(e.target.value)}
                    required
                />
                <br />
                <input
                    type="text"
                    placeholder="Imię"
                    value={regName}
                    onChange={(e) => setRegName(e.target.value)}
                    required
                />
                <br />
                <input
                    type="text"
                    placeholder="Nazwisko"
                    value={regSurname}
                    onChange={(e) => setRegSurname(e.target.value)}
                    required
                />
                <br />
                <input
                    type="email"
                    placeholder="Email"
                    value={regEmail}
                    onChange={(e) => setRegEmail(e.target.value)}
                    required
                />
                <br />
                <input
                    type="tel"
                    placeholder="Numer telefonu"
                    value={regPhone}
                    onChange={(e) => setRegPhone(e.target.value)}
                    required
                />
                <br />
                <button type="submit">Zarejestruj</button>
                {regMessage && <p>{regMessage}</p>}
            </form>
        </div>
    );
}
