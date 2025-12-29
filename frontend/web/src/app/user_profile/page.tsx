"use client";

import { useEffect, useState } from "react";
import OperatorLayout from "./operator_layout";
import ClientLayout from "./client_layout";
import type { OperatorDto } from "./operator_dto";
import type { ClientDto } from "./client_dto";

export default function ProfilePage() {
  const [profileData, setProfileData] = useState<
    ClientDto | OperatorDto | null
  >(null);
  const [isOperator, setIsOperator] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const token = localStorage.getItem("token");

        const userResponse = await fetch(`/api/user/getUserData`, {
          headers: {
            "X-USER-TOKEN": `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        if (!userResponse.ok) {
          throw new Error("Failed to get user data");
        }

        const userData = await userResponse.json();

        if (userData.role === "OPERATOR") {
          setIsOperator(true);
        } else {
          setIsOperator(false);
        }

        setProfileData(userData);
      } catch (err) {
        setError(err instanceof Error ? err.message : "An error occurred");
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        Ładowanie...
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen text-red-600">
        {error}
      </div>
    );
  }

  if (!profileData) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        Brak danych
      </div>
    );
  }

  return isOperator ? <OperatorLayout /> : <ClientLayout />;
}
