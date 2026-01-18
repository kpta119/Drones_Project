"use client";

import { useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const token = searchParams.get("token");
    const role = searchParams.get("role");
    const username = searchParams.get("username");
    const userId = searchParams.get("userid");

    const decodedUsername = (username || "").replaceAll("+", " ");
    localStorage.setItem("token", token || "");
    localStorage.setItem("role", role || "");
    localStorage.setItem("name", decodedUsername);
    localStorage.setItem("userId", userId || "");

    window.dispatchEvent(new Event("authChanged"));
    try {
      if (role === "INCOMPLETE") {
        router.push("/complete-profile");
      } else if (role === "ADMIN") {
        router.push("/admin");
      } else {
        router.replace("/user_profile");
      }
    } catch (error) {
      console.error("Błąd przetwarzania logowania:", error);
    }
  }, [router, searchParams]);

  return (
    <div className="flex h-screen w-full items-center justify-center bg-gray-50">
      <div className="text-center">
        <h2 className="text-xl font-semibold mb-2">Logowanie...</h2>
        <p className="text-gray-500">Przetwarzanie autentykacji...</p>
        <div className="mt-4 h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent mx-auto"></div>
      </div>
    </div>
  );
}

export default function AuthCallbackPage() {
  return (
    <div className="flex h-screen w-full items-center justify-center bg-gray-50">
      <Suspense
        fallback={
          <div className="text-center">
            <p className="text-gray-500">Inicjalizacja...</p>
          </div>
        }
      >
        <AuthCallbackContent />
      </Suspense>
    </div>
  );
}
