"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import LogoutButton from "./logout_button";



export default function PageHeader() {
    type User = {
        token: string;
        role: string;
        name: string;
    };
    const [user, setUser] = useState<User | null>(null);

    useEffect(() => {
        const token = localStorage.getItem("token");
        const role = localStorage.getItem("role");
        const name = localStorage.getItem("name")

        if (token && role && name) {
            setUser({ token, role, name });
        }
    }, []);

    return (
        <header className="bg-yellow-200 w-full shadow-md mb-8">
            <div className="max-w-7xl mx-auto px-4 py-3 flex justify-between items-center">
                <Link href="/login">
                    <span className="font-bold text-lg cursor-pointer">Dronex</span>
                </Link>

                <nav>
                    {user ? (
                        <div className="flex items-center space-x-4">
                            <span className="font-medium">{user.name}</span>
                            <LogoutButton setUser={setUser} />
                        </div>
                    ) : (
                        <div className="flex space-x-4">
                            <Link href="/login">
                                <button className="bg-white text-yellow-300 px-3 py-1 rounded hover:bg-gray-100">
                                    Login
                                </button>
                            </Link>
                        </div>
                    )}
                </nav>
            </div>
        </header>
    );
}