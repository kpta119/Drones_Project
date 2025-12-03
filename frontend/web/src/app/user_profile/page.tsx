'use client';

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import LogoutButton from "@/src/components/logout_button";

export default function UserProfilePage() {
    const router = useRouter();

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            router.push('/login');
        }
    }, [router]);

    return (
        <div style={{ padding: '2rem' }}>
            <h1>Welcome to your profile!</h1>
            <LogoutButton />
        </div>
    );
}
