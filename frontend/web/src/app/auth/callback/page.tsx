"use client";

import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState("Przetwarzanie logowania...");

  useEffect(() => {
    // Pobieranie danych z parametrów URL
    const token = searchParams.get("token");
    const role = searchParams.get("role");
    const username = searchParams.get("username");
    const userId = searchParams.get("userId");

    if (!token || !role) {
      setStatus("Błąd: Nie znaleziono danych uwierzytelniających.");
      return;
    }

    try {
      const decodedUsername = (username || "").replaceAll("+", " ");

      // Zapisywanie do localStorage
      localStorage.setItem("token", token);
      localStorage.setItem("role", role);
      localStorage.setItem("username", decodedUsername);
      localStorage.setItem("userId", userId || "");

      if (role === "INCOMPLETE") {
        router.replace("/complete-profile");
      } else {
        router.replace("/user_profile");
      }
    } catch (error) {
      console.error("Błąd przetwarzania logowania:", error);
      setStatus("Wystąpił błąd podczas logowania.");
    }
  }, [router, searchParams]);

  return (
    <div className="text-center">
      <h2 className="text-xl font-semibold mb-2">Logowanie...</h2>
      <p className="text-gray-500">{status}</p>
      <div className="mt-4 h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent mx-auto"></div>
    </div>
  );
}

export default function AuthCallbackPage() {
  return (
    <div className="flex h-screen w-full items-center justify-center bg-gray-50">
      <Suspense fallback={
        <div className="text-center">
          <p className="text-gray-500">Inicjalizacja...</p>
        </div>
      }>
        <AuthCallbackContent />
      </Suspense>
    </div>
  );
}