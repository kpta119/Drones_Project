"use client";

import { useEffect, useState, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import OperatorLayout from "./operator_layout";
import ClientLayout from "./client_layout";
import type { OperatorDto } from "./operator_dto";
import type { ClientDto } from "./client_dto";
import { API_URL } from "../config";

function ProfileContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const userIdFromUrl = searchParams.get("user_id");

  const [profileData, setProfileData] = useState<
    ClientDto | OperatorDto | null
  >(null);
  const [isOperator, setIsOperator] = useState(false);
  const [isOwnProfile, setIsOwnProfile] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadProfile = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          router.replace("/login");
          return;
        }

        const payload = JSON.parse(atob(token.split(".")[1]));
        const myIdFromToken = payload.userId || payload.id || payload.sub;

        if (!userIdFromUrl) {
          router.replace(`/user_profile?user_id=${myIdFromToken}`);
          return;
        }

        const url = `${API_URL}/api/user/getUserData?user_id=${userIdFromUrl}`;

        const userResponse = await fetch(url, {
          headers: {
            "X-USER-TOKEN": `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        if (!userResponse.ok) throw new Error("Profil nie odnaleziony");
        const userData = await userResponse.json();

        const isOwner = userIdFromUrl === myIdFromToken;

        setIsOwnProfile(isOwner);
        setIsOperator(userData.role === "OPERATOR");
        setProfileData(userData);
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [userIdFromUrl, router]);

  if (loading)
    return (
      <div className="flex items-center justify-center min-h-screen font-montserrat text-black">
        Ładowanie...
      </div>
    );
  if (error)
    return (
      <div className="flex items-center justify-center min-h-screen text-red-600 font-montserrat">
        {error}
      </div>
    );
  if (!profileData) return null;

  return isOperator ? (
    <OperatorLayout
      data={profileData as OperatorDto}
      isOwnProfile={isOwnProfile}
    />
  ) : (
    <ClientLayout data={profileData as ClientDto} isOwnProfile={isOwnProfile} />
  );
}

export default function ProfilePage() {
  return (
    <Suspense fallback={null}>
      <div className="mt-9"></div>
      <ProfileContent />
    </Suspense>
  );
}
