"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import OperatorLayout from "./operator_layout";
import ClientLayout from "./client_layout";

interface UserResponse {
  userId: string;
  username: string;
  name: string;
  surname: string;
  email: string;
  phone: string;
  role: string[];
  rating: number;
  reviews: any[];
}

interface OperatorProfileDto extends UserResponse {
  portfolio: any;
  description: string;
  services: string[];
  certificates: string[];
}

export default function UserProfilePage() {
  const params = useParams();
  const username = params.username as string;

  const [userData, setUserData] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadUserData = async () => {
      try {
        const token = localStorage.getItem("token");

        const response = await fetch(`/api/user/getUserData`, {
          headers: {
            "X-USER-TOKEN": `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        if (!response.ok) {
          const errorData = await response.text();
          console.error("Error response:", errorData);
          throw new Error(`Failed to get user data: ${response.status}`);
        }

        const data: UserResponse = await response.json();
        setUserData(data);
        // console.log("User data:", data);
      } catch (err) {
        setError(err instanceof Error ? err.message : "An error occurred");
      } finally {
        setLoading(false);
      }
    };

    loadUserData();
  }, []);
  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  return (
    <div>
      <h1>
        {userData?.name} {userData?.surname}
      </h1>
      <p>{userData?.email}</p>
    </div>
  );
}
