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
      <div className="max-w-[1800px] mx-auto px-4 md:px-8 py-3.5 flex items-center justify-between text-black">
        <div className="flex items-center gap-2 md:gap-6 flex-1">
          <Link
            href="/orders"
            className="hover:text-primary-700 transition-colors flex items-center shrink-0"
          >
            <FaHome size={20} />
          </Link>
          <Link
            href="/user_profile"
            className="bg-white/60 px-3 md:px-8 py-1.5 rounded-xl font-bold text-[10px] md:text-xs hover:bg-primary-400 transition-all shadow-sm border border-primary-500/20 whitespace-nowrap"
          >
            Moje konto
          </Link>
          <Link
            href="/orders"
            className="bg-white/60 px-3 md:px-8 py-1.5 rounded-xl font-bold text-[10px] md:text-xs hover:bg-primary-400 transition-all shadow-sm border border-primary-500/20 whitespace-nowrap"
          >
            Zlecenia
          </Link>
        </div>

        <div className="flex-none px-4 text-center">
          <span className="text-2xl md:text-3xl font-bold tracking-tighter text-black">
            Dronex
          </span>
        </div>

        <div className="flex items-center justify-end gap-2 md:gap-6 flex-1">
          {user ? (
            <>
              <div className="hidden sm:flex items-center gap-2 font-bold text-[10px] md:text-xs text-primary-900 bg-primary-400/20 px-2 md:px-3 py-1.5 rounded-lg border border-primary-500/10 truncate max-w-[150px] lg:max-w-none">
                <FaEnvelope size={14} className="text-primary-700 shrink-0" />
                <span className="truncate">{user.email}</span>
              </div>
              <LogoutButton setUser={setUser} />
            </>
          ) : (
            <Link href="/login">
              <button className="bg-white/60 px-4 md:px-8 py-1.5 rounded-xl font-bold text-[10px] md:text-xs hover:bg-primary-400 transition-all border border-primary-500/20 whitespace-nowrap">
                Zaloguj się
              </button>
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
