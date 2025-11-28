'use client';

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import LoginForm from "@/src/components/login_form";
import RegisterForm from "@/src/components/register_form";

export default function AuthPage() {
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      router.push('/user_profile');
    }
  }, [router]);

  return (
    <div>
      <LoginForm />
      <RegisterForm />
    </div>
  );
}