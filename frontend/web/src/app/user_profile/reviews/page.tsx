"use client";

import { useEffect, useState, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import ReviewsView from "@/src/app/orders/utils/reviews_view";

function ReviewsContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const userIdFromUrl = searchParams.get("user_id");
  const userNameFromUrl = searchParams.get("user_name");

  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!userIdFromUrl || !userNameFromUrl) {
      router.replace("/");
      return;
    }
    setIsLoading(false);
  }, [userIdFromUrl, userNameFromUrl, router]);

  if (isLoading) return null;
  if (!userIdFromUrl || !userNameFromUrl) return null;

  return (
    <ReviewsView
      userId={userIdFromUrl}
      userName={decodeURIComponent(userNameFromUrl)}
      onClose={() => router.back()}
    />
  );
}

export default function ReviewsPage() {
  return (
    <Suspense fallback={null}>
      <ReviewsContent />
    </Suspense>
  );
}
