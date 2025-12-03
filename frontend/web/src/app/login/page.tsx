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
    <div className="flex flex-col md:flex-row gap-6 mx-auto max-w-4xl p-6">
      <div className="flex-1 bg-white rounded-2xl ">
        <LoginForm />
      </div>
      <div className="flex-1 bg-white rounded-2xl ">

        <RegisterForm />
      </div>
    </div>

  );
}