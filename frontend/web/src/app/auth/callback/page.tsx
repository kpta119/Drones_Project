"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Cookies from "js-cookie";

export default function AuthCallbackPage() {
  const router = useRouter();
  const [status, setStatus] = useState("Przetwarzanie logowania...");

  useEffect(() => {
    const token = Cookies.get("auth_token");
    const role = Cookies.get("auth_role");
    const username = Cookies.get("auth_username");
    const userId = Cookies.get("auth_userid");

    const decodedUsername = (username || "").replaceAll("+", " ");
    localStorage.setItem("token", token || "");
    localStorage.setItem("role", role || "");
    localStorage.setItem("name", decodedUsername);
    localStorage.setItem("userId", userId || "");

    window.dispatchEvent(new Event("authChanged"));
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
        <div className="mt-4 h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent mx-auto"></div>
      </div>
    </div>
  );
}
