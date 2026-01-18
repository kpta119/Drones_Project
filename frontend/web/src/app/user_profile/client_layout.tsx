"use client";

import { useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { ClientDto } from "./client_dto";
import OperatorRegisterButton from "@/src/components/operator_register_button";
import OperatorRegisterModule from "./operator_register/operator_register_module";
import ReviewsView from "@/src/app/orders/utils/reviews_view";
import { FaStar, FaUser, FaPhone, FaEnvelope } from "react-icons/fa";
import { API_URL } from "../config";

interface Review {
  body: string;
  stars: number;
  name?: string;
  surname?: string;
  username?: string;
  author_name?: string;
  author_username?: string;
}

export default function ClientLayout({
  data,
  isOwnProfile,
}: {
  data: ClientDto;
  isOwnProfile: boolean;
}) {
  const searchParams = useSearchParams();
  const displayedUserId = searchParams.get("user_id");

  const [showRegister, setShowRegister] = useState(false);
  const [showReviews, setShowReviews] = useState(false);
  const [recentReviews, setRecentReviews] = useState<Review[]>([]);
  const [reviewsLoading, setReviewsLoading] = useState(true);
  const [reviewsError, setReviewsError] = useState<string | null>(null);
  const [averageRating, setAverageRating] = useState(0);
  const [totalReviewsCount, setTotalReviewsCount] = useState(0);

  useEffect(() => {
    const fetchRecentReviews = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token || !displayedUserId) {
          setReviewsLoading(false);
          return;
        }

        const res = await fetch(
          `${API_URL}/api/reviews/getUserReviews/${displayedUserId}`,
          {
            headers: {
              "X-USER-TOKEN": `Bearer ${token}`,
              "Content-Type": "application/json",
            },
          }
        );

        if (res.ok) {
          const allReviews = await res.json();
          setRecentReviews(allReviews.slice(0, 3));
          setTotalReviewsCount(allReviews.length);

          if (allReviews && allReviews.length > 0) {
            const avgRating =
              allReviews.reduce(
                (sum: number, review: Review) => sum + review.stars,
                0
              ) / allReviews.length;
            setAverageRating(Math.round(avgRating * 10) / 10);
          } else {
            setAverageRating(0);
          }

          setReviewsError(null);
        } else {
          setRecentReviews([]);
          setAverageRating(0);
          setTotalReviewsCount(0);
          setReviewsError(null);
        }
      } catch (err) {
        console.error("Error fetching reviews:", err);
        setRecentReviews([]);
        setAverageRating(0);
        setTotalReviewsCount(0);
        setReviewsError(null);
      } finally {
        setReviewsLoading(false);
      }
    };

    fetchRecentReviews();
  }, [displayedUserId]);

  return (
    <>
      <div
        className="grid grid-cols-2 grid-rows-1 justify-center items-center pl-10 pr-10 pt-10 pb-10 m-auto font-montserrat w-7xl"
        style={{ height: "85vh" }}
      >
        <style>{`
        @keyframes colorShine {
          0%, 100% {
            color: var(--color-primary-500);
          }
          50% {
            color: var(--color-primary-900);
          }
        }

        .shine-text {
          animation: colorShine 5s ease-in-out infinite;
        }
      `}</style>

        <div className="flex flex-col gap-3 items-center min-h-[400px] lg:min-h-0">
          <div className="flex flex-col items-center">
            <div className="w-40 h-40 lg:w-48 lg:h-48 bg-[#D9D9D9] rounded-full flex items-center justify-center shrink-0 drop-shadow-lg/40 hover:ring-4 hover:ring-[#D9D9D9] transition-all hover:drop-shadow-xl/50">
              <FaUser className="text-5xl lg:text-6xl text-gray-600" />
            </div>
            <div className="flex text-black text-2xl lg:text-3xl pt-2 gap-1">
              {[...Array(5)].map((_, i) => (
                <FaStar
                  key={i}
                  size={24}
                  className={
                    i < Math.floor(averageRating)
                      ? "text-primary-400"
                      : "text-gray-300"
                  }
                />
              ))}
            </div>
          </div>

          <div className="flex flex-col justify-center items-center flex-1 text-center">
            <h2 className="text-2xl lg:text-3xl font-light">
              {data.name} {data.surname}
            </h2>
            <p className="text-gray-600 text-lg mb-4">@{data.username}</p>
            <div className="space-y-1 mb-6">
              <div className="flex items-center justify-center gap-2">
                <FaPhone className="text-primary-700" size={16} />
                <p>{data.phone_number}</p>
              </div>
              <div className="flex items-center justify-center gap-2">
                <FaEnvelope className="text-primary-700" size={16} />
                <p className="break-all">{data.email}</p>
              </div>
            </div>
            {isOwnProfile && (
              <OperatorRegisterButton onClick={() => setShowRegister(true)} />
            )}
          </div>
        </div>

        <div className="bg-[#D9D9D9] rounded-4xl p-6 h-full w-[90%] flex flex-col">
          <h3 className="font-light text-lg mb-3 text-center">
            Najnowsze opinie na temat {data.username}
          </h3>

          <div className="flex-1 overflow-y-auto pr-2">
            {reviewsLoading ? (
              <div className="flex items-center justify-center h-full">
                <p className="text-gray-600 text-sm italic animate-pulse">
                  Ładowanie opinii...
                </p>
              </div>
            ) : recentReviews?.length > 0 ? (
              recentReviews.map((review, idx) => (
                <div
                  key={idx}
                  className="mb-4 text-sm bg-white/30 p-3 rounded-xl"
                >
                  <div className="flex items-center gap-2 mb-2">
                    {[...Array(5)].map((_, i) => (
                      <FaStar
                        key={i}
                        size={12}
                        className={
                          i < review.stars ? "text-yellow-500" : "text-gray-300"
                        }
                      />
                    ))}
                  </div>
                  <p className="font-semibold text-xs text-gray-600 mb-1">
                    {review.author_name ||
                      (review.name && review.surname
                        ? `${review.name} ${review.surname}`
                        : review.author_username ||
                          review.username ||
                          "Anonimowy")}
                  </p>
                  <p className="text-gray-800 leading-relaxed">{review.body}</p>
                </div>
              ))
            ) : (
              <div className="flex items-center justify-center h-full">
                <p className="text-gray-600 text-sm italic">Brak opinii</p>
              </div>
            )}
          </div>

          <button
            onClick={() => setShowReviews(true)}
            className="w-full bg-gray-700 text-white rounded-2xl py-3 font-semibold hover:cursor-pointer hover:bg-primary-800 hover:ring-2 hover:ring-primary-800 transition-all text-sm mt-4"
          >
            Sprawdź więcej opinii
          </button>
        </div>
      </div>

      {showRegister && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fadeIn p-4">
          <div className="bg-white rounded-2xl p-6 lg:p-12 w-full max-w-6xl h-[90vh] lg:h-4/5 overflow-y-auto relative animate-scaleIn">
            <OperatorRegisterModule onClose={() => setShowRegister(false)} />
          </div>
        </div>
      )}

      {showReviews && (
        <ReviewsView
          userName={`${data.name} ${data.surname}`}
          onClose={() => setShowReviews(false)}
        />
      )}
    </>
  );
}
