"use client";

import { useEffect, useState, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import OperatorLayout from "./operator_layout";
import ClientLayout from "./client_layout";
import type { OperatorDto } from "./operator_dto";
import type { ClientDto } from "./client_dto";

interface Review {
  body: string;
  stars: number;
  name?: string;
  surname?: string;
  username?: string;
  author_name?: string;
  author_username?: string;
}

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
  const [reviews, setReviews] = useState<Review[]>([]);
  const [averageRating, setAverageRating] = useState(0);

  useEffect(() => {
    // Reset state when user changes
    setLoading(true);
    setProfileData(null);
    setError(null);
    setReviews([]);
    setAverageRating(0);

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

        const url = `/user/getUserData?user_id=${userIdFromUrl}`;

        const userResponse = await fetch(url, {
          headers: {
            "X-USER-TOKEN": `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        if (!userResponse.ok) throw new Error("Profil nie odnaleziony");
        const userData = await userResponse.json();

        const isOwner = userIdFromUrl === myIdFromToken;

        // Admins cannot access their own profile, redirect to admin page
        if (isOwner && userData.role === "ADMIN") {
          router.replace("/admin");
          return;
        }

        // Fetch reviews
        const reviewsResponse = await fetch(
          `/reviews/getUserReviews/${userIdFromUrl}`,
          {
            headers: {
              "X-USER-TOKEN": `Bearer ${token}`,
              "Content-Type": "application/json",
            },
          }
        );

        if (reviewsResponse.ok) {
          const allReviews = await reviewsResponse.json();
          setReviews(allReviews || []);

          if (allReviews && allReviews.length > 0) {
            const avgRating =
              allReviews.reduce(
                (sum: number, review: Review) => sum + review.stars,
                0
              ) / allReviews.length;
            setAverageRating(Math.round(avgRating * 10) / 10);
          }
        }

        setIsOwnProfile(isOwner);
        setIsOperator(userData.role === "OPERATOR");
        setProfileData(userData);
      } catch (err: unknown) {
        const error = err instanceof Error ? err.message : "Nieznany błąd";
        setError(error);
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [userIdFromUrl, router]);

  if (loading)
    return (
      <div className="flex items-center justify-center min-h-screen font-montserrat text-black">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
          <span className="text-gray-600">Ładowanie profilu...</span>
        </div>
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
      reviews={reviews}
      averageRating={averageRating}
    />
  ) : (
    <ClientLayout
      data={profileData as ClientDto}
      isOwnProfile={isOwnProfile}
      reviews={reviews}
      averageRating={averageRating}
    />
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
