'use client';

import { useRouter } from "next/navigation";

type Props = {
    setUser: (user: null) => void;
};

export default function LogoutButton({ setUser }: Props) {
    const router = useRouter();

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem("name");
        setUser(null);
        router.push('/login');
    };

    return (
        <button className="bg-white text-yellow-300 px-3 py-1 rounded hover:bg-gray-100"
            onClick={handleLogout}
        >
            Logout
        </button>
    );
}