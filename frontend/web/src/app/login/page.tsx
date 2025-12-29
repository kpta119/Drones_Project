"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import LoginForm from "@/src/components/login_form";
import RegisterForm from "@/src/components/register_form";

export default function AuthPage() {
  const router = useRouter();
  const [activeForm, setActiveForm] = useState<"none" | "login" | "register">(
    "none"
  );

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (token) {
      router.push("/user_profile");
    }
  }, [router]);

  return (
    <div
      style={{ height: "calc(100vh - 120px)" }}
      className="max-w-full w-screen max-h-full  flex p-8 gap-6"
    >
      <div className="w-2/5 rounded-2xl flex flex-col items-center justify-center p-8">
        {activeForm === "none" && (
          <div className="flex flex-col gap-4 w-full max-w-xs opacity-0 animate-fadeIn">
            <button
              onClick={() => setActiveForm("login")}
              className="relative w-full h-12 px-6 rounded-lg font-semibold text-lg border-2 border-primary-600 text-primary-900 overflow-hidden group flex items-center justify-center"
            >
              <span className="absolute inset-0 bg-primary-600 translate-y-full transition-transform duration-300 ease-in-out group-hover:translate-y-0"></span>
              <span className="relative z-10 transition-colors duration-300 ease-in-out group-hover:text-white">
                Logowanie
              </span>
            </button>
            <button
              onClick={() => setActiveForm("register")}
              className="relative w-full px-6 py-3 text-white bg-gray-900 font-bold text-lg overflow-hidden rounded-lg group h-12"
            >
              <span className="absolute inset-0 flex items-center justify-center transition-transform duration-300 ease-in-out group-hover:-translate-x-full">
                Rejestracja
              </span>
              <span className="absolute inset-0 bg-primary-600 translate-x-full transition-transform duration-300 ease-in-out group-hover:translate-x-0 flex items-center justify-center font-bold text-lg text-white">
                Dołącz do nas!
              </span>
            </button>
          </div>
        )}
        {activeForm === "login" && (
          <div className="flex flex-col gap-4 w-full max-w-xs opacity-0 animate-fadeIn">
            <LoginForm />
            <button
              onClick={() => setActiveForm("none")}
              className="text-black hover:text-primary-500 font-medium text-center transition-all"
            >
              ← Wróć
            </button>
          </div>
        )}
        {activeForm === "register" && (
          <div className="flex flex-col gap-4 w-full max-w-xs opacity-0 animate-fadeIn">
            <RegisterForm />
            <button
              onClick={() => setActiveForm("none")}
              className="text-black hover:text-primary-500 font-medium text-center transition-all"
            >
              ← Wróć
            </button>
          </div>
        )}
      </div>
      <div className="w-3/5 bg-linear-to-br from-primary-500 to-primary-50 rounded-2xl flex items-center justify-center p-12 animate-gradient">
        <div className="text-center">
          <h1 className="text-7xl font-bold text-black mb-6">Dronex</h1>
          <p className="text-2xl text-gray-800 leading-relaxed">
            Najlepszy sposób na znalezienie współpracy z operatorami dronów!
          </p>
        </div>
      </div>
    </div>
  );
}
