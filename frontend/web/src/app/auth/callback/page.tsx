'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Cookies from 'js-cookie';

export default function AuthCallbackPage() {
  const router = useRouter();
  const [status, setStatus] = useState('Przetwarzanie logowania...');

  useEffect(() => {
    const token = Cookies.get('auth_token');
    const role = Cookies.get('auth_role');
    const userId = Cookies.get('auth_userid');
    const username = Cookies.get('auth_username');
    const email = Cookies.get('auth_email');
    console.log('Odebrane ciasteczka:', { token, role, userId, username, email });

    if (!token) {
      setStatus('Błąd: Brak tokena logowania. Spróbuj ponownie.');
      setTimeout(() => router.push('/login'), 3000);
      return;
    }

    try {
      localStorage.setItem('token', token);
      localStorage.setItem('role', role || '');
      localStorage.setItem('user', JSON.stringify({
        id: userId,
        email: email,
        username: username
      }));
      ['auth_token', 'auth_role', 'auth_userid', 'auth_email', 'auth_username']
        .forEach(cookieName => Cookies.remove(cookieName));

      if (role === 'INCOMPLETE') {
        console.log('Nowy użytkownik - przekierowanie do formularza');
        router.push('/complete-profile');
      } else {
        console.log('Logowanie udane - witamy z powrotem');
        router.push('/');
      }

    } catch (error) {
      console.error("Błąd przetwarzania logowania:", error);
      setStatus('Wystąpił błąd podczas logowania.');
    }
  }, [router]);

  return (
    <div className="flex h-screen w-full items-center justify-center bg-gray-50">
      <div className="text-center">
        <h2 className="text-xl font-semibold mb-2">Logowanie...</h2>
        <p className="text-gray-500">{status}</p>
        {/* Tu możesz wrzucić kręcący się spinner */}
        <div className="mt-4 h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent mx-auto"></div>
      </div>
    </div>
  );
}