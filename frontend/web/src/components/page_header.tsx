"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import LogoutButton from "./logout_button";
import { FaHome, FaEnvelope } from "react-icons/fa";

export default function PageHeader() {
  type User = {
    token: string;
    role: string;
    email: string;
  };
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");
    const nameAsEmail = localStorage.getItem("name");

    if (token && role && nameAsEmail) {
      setUser({
        token,
        role,
        email: nameAsEmail,
      });
    }
  }, []);

  return (
    <header className="bg-primary-300 w-full font-montserrat shadow-sm border-b border-primary-400">
      <div className="max-w-[1800px] mx-auto px-8 py-3.5 flex justify-between items-center relative text-black">
        <div className="flex items-center gap-6 z-10">
          <Link
            href="/orders"
            className="hover:text-primary-700 transition-colors flex items-center"
          >
            <FaHome size={20} />
          </Link>
          <Link
            href="/user_profile"
            className="bg-white/60 px-8 py-1.5 rounded-xl font-bold text-xs hover:bg-primary-400 transition-all shadow-sm border border-primary-500/20"
          >
            Moje konto
          </Link>
          <Link
            href="/orders"
            className="bg-white/60 px-8 py-1.5 rounded-xl font-bold text-xs hover:bg-primary-400 transition-all shadow-sm border border-primary-500/20"
          >
            Zlecenia
          </Link>
        </div>

        <div className="absolute left-1/2 -translate-x-1/2 text-3xl font-bold tracking-tighter text-black ">
          Dronex
        </div>

        <div className="flex items-center gap-6 z-10">
          {user ? (
            <>
              <div className="flex items-center gap-2 font-bold text-xs md:flex text-primary-900 bg-primary-400/20 px-3 py-1.5 rounded-lg border border-primary-500/10">
                <FaEnvelope size={14} className="text-primary-700" />
                <span>{user.email}</span>
              </div>
              <LogoutButton setUser={setUser} />
            </>
          ) : (
            <Link href="/login">
              <button className="bg-white/60 px-8 py-1.5 rounded-xl font-bold text-xs hover:bg-primary-400 transition-all border border-primary-500/20">
                Zaloguj się
              </button>
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
