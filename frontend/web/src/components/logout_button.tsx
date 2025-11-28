'use client';

import { useRouter } from "next/navigation";

export default function LogoutButton() {
    const router = useRouter();

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        router.push('/login');
    };

    return (
        <button
            onClick={handleLogout}
            style={{ marginTop: '1rem', padding: '0.5rem 1rem' }}
        >
            Logout
        </button>
    );
}