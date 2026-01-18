"use client";

import { useRouter } from "next/navigation";

type Props = {
  setUser: (user: null) => void;
};

export default function LogoutButton({ setUser }: Props) {
  const router = useRouter();

  const handleLogout = async () => {
    const token = localStorage.getItem("token");

    try {
      await fetch(`/api/auth/logout`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });
    } catch (error) {
      console.error("Logout API error:", error);
    }

    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("name");

    setUser(null);
    router.push("/login");
  };

  return (
    <button
      className="bg-white text-yellow-300 px-3 py-1 rounded hover:bg-gray-100"
      onClick={handleLogout}
    >
      Wyloguj się
    </button>
  );
}
