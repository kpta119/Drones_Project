"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [isAuthorized, setIsAuthorized] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const checkAuth = () => {
      const role = localStorage.getItem("role");
      if (role !== "ADMIN") {
        router.push("/login");
        return;
      }
      setIsAuthorized(true);
      setIsLoading(false);
    };

    checkAuth();

    window.addEventListener("authChanged", checkAuth);
    return () => window.removeEventListener("authChanged", checkAuth);
  }, [router]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        Loading...
      </div>
    );
  }

  if (!isAuthorized) {
    return null;
  }

  return (
    <div className="flex min-h-screen">
      <aside className="w-64 bg-gray-100 p-5 border-r border-gray-300">
        <h2 className="text-xl font-bold">Admin Panel</h2>
        <nav className="mt-5">
          <ul className="list-none p-0 space-y-4">
            <li>
              <Link
                href="/admin"
                className="text-blue-600 no-underline hover:text-blue-800"
              >
                Dashboard
              </Link>
            </li>
            <li>
              <Link
                href="/admin/users"
                className="text-blue-600 no-underline hover:text-blue-800"
              >
                Użytkownicy
              </Link>
            </li>
            <li>
              <Link
                href="/admin/orders"
                className="text-blue-600 no-underline hover:text-blue-800"
              >
                Zlecenia
              </Link>
            </li>
          </ul>
        </nav>
      </aside>

      <main className="flex-1 p-5">{children}</main>
    </div>
  );
}
