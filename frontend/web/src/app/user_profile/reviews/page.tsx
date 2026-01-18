"use client";

import { useEffect, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import ReviewsView from "@/src/app/orders/utils/reviews_view";

function ReviewsContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const userIdFromUrl = searchParams.get("user_id");
  const userNameFromUrl = searchParams.get("user_name");

  useEffect(() => {
    if (!userIdFromUrl || !userNameFromUrl) {
      router.replace("/");
    }
  }, [userIdFromUrl, userNameFromUrl, router]);

  if (!userIdFromUrl || !userNameFromUrl) return null;

  return (
    <ReviewsView
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
