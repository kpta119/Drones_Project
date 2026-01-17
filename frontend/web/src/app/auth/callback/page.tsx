"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Cookies from "js-cookie";

export default function AuthCallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState("Przetwarzanie logowania...");

  useEffect(() => {
    // all data from URL params

    const token = searchParams.get("token");
    const role = searchParams.get("role");
    const username = searchParams.get("username");
    const userId = searchParams.get("userId");
    // const email = searchParams.get("email");

    const decodedUsername = (username || "").replaceAll("+", " ");
    localStorage.setItem("token", token || "");
    localStorage.setItem("role", role || "");
    localStorage.setItem("username", decodedUsername);
    localStorage.setItem("userId", userId || "");
    try {
      [
        "auth_token",
        "auth_role",
        "auth_userid",
        "auth_email",
        "auth_username",
      ].forEach((cookieName) => Cookies.remove(cookieName));

      if (role === "INCOMPLETE") {
        router.push("/complete-profile");
      } else {
        router.push("/user_profile");
      }
    } catch (error) {
      console.error("Błąd przetwarzania logowania:", error);
    }
  }, [router]);

  return (
    <div className="flex h-screen w-full items-center justify-center bg-gray-50">
      <div className="text-center">
        <h2 className="text-xl font-semibold mb-2">Logowanie...</h2>
        <p className="text-gray-500">{status}</p>
        {/* Tu możesz wrzucić kręcący się spinner */}
        <div className="mt-4 h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent mx-auto"></div>
      </div>
    </div>
  );
}
