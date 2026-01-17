"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { FaTimes, FaStar } from "react-icons/fa";
import { API_URL } from "../../config";

interface ReviewData {
  body: string;
  stars: number;
  author_id?: string;
  author_name?: string;
  author_username?: string;
  name?: string;
  surname?: string;
  username?: string;
}

interface ReviewsViewProps {
  userName: string;
  onClose: () => void;
}

export default function ReviewsView({ userName, onClose }: ReviewsViewProps) {
  const searchParams = useSearchParams();
  const displayedUserId = searchParams.get("user_id");

  const [reviews, setReviews] = useState<ReviewData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [rating, setRating] = useState(0);
  const [totalReviews, setTotalReviews] = useState(0);

  useEffect(() => {
    const fetchReviews = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          console.warn("No token available");
          setError("Brak tokenu autoryzacji");
          setLoading(false);
          return;
        }

        if (!displayedUserId) {
          console.warn("No user_id in URL");
          setError("Brak identyfikatora użytkownika");
          setLoading(false);
          return;
        }

        console.log("Fetching reviews for userId:", displayedUserId);
        const res = await fetch(
          `${API_URL}/api/reviews/getUserReviews/${displayedUserId}`,
          {
            headers: {
              "X-USER-TOKEN": `Bearer ${token}`,
              "Content-Type": "application/json",
            },
          }
        );

        console.log("Response status:", res.status);

        if (res.ok) {
          const data = await res.json();
          console.log("Reviews data:", data);
          setReviews(data);

          if (data && data.length > 0) {
            const avgRating =
              data.reduce(
                (sum: number, review: ReviewData) => sum + review.stars,
                0
              ) / data.length;
            setRating(Math.round(avgRating * 10) / 10);
            setTotalReviews(data.length);
          } else {
            setRating(0);
            setTotalReviews(0);
          }
          setError(null);
        } else {
          const errorText = await res.text();
          console.error("Failed to fetch reviews:", res.status, errorText);
          setReviews([]);
          setError(`Błąd ładowania opinii (${res.status})`);
        }
      } catch (err) {
        console.error("Error fetching reviews:", err);
        setReviews([]);
        setError("Błąd przy ładowaniu opinii");
      } finally {
        setLoading(false);
      }
    };

    fetchReviews();
  }, [displayedUserId]);

  if (loading) {
    return (
      <div className="fixed inset-0 z-99999 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn font-montserrat text-black">
        <div className="bg-white w-full max-w-4xl rounded-[3rem] overflow-hidden shadow-2xl p-8 lg:p-12">
          <div className="text-center py-12 animate-pulse">
            <p className="text-lg font-bold text-gray-600">
              Ładowanie opinii...
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-99999 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn font-montserrat text-black">
      <div className="bg-white w-full max-w-4xl max-h-[90vh] rounded-[3rem] overflow-hidden shadow-2xl relative border border-white/20">
        <button
          onClick={onClose}
          className="absolute top-8 right-8 text-gray-400 hover:text-black transition-colors z-10"
        >
          <FaTimes size={24} />
        </button>

        <div className="p-8 lg:p-12 overflow-y-auto max-h-[90vh]">
          <div className="mb-10">
            <h2 className="text-3xl font-bold text-gray-900 tracking-tight mb-2">
              Opinie na temat {userName}
            </h2>
            <div className="flex items-center gap-4 mt-6">
              <div>
                <div className="flex items-center gap-2 mb-2">
                  {[...Array(5)].map((_, i) => (
                    <FaStar
                      key={i}
                      size={24}
                      className={`${i < Math.floor(rating)
                          ? "text-primary-400"
                          : "text-gray-200"
                        }`}
                    />
                  ))}
                </div>
                <p className="text-2xl font-bold text-gray-900">
                  {rating.toFixed(1)}{" "}
                  <span className="text-sm text-gray-500">
                    ({totalReviews} {totalReviews === 1 ? "opinia" : "opinii"})
                  </span>
                </p>
              </div>
            </div>
          </div>

          {error && (
            <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 rounded-r-2xl text-sm font-bold">
              ⚠️ {error}
            </div>
          )}

          {reviews.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-400 text-lg">
                Brak opinii na temat tego użytkownika
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {reviews.map((review, index) => (
                <div
                  key={index}
                  className="bg-gray-50 border border-gray-200 rounded-2xl p-6 hover:border-primary-300 transition-all"
                >
                  <div className="flex items-start justify-between mb-3">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        {[...Array(5)].map((_, i) => (
                          <FaStar
                            key={i}
                            size={16}
                            className={`${i < review.stars
                                ? "text-primary-400"
                                : "text-gray-200"
                              }`}
                          />
                        ))}
                      </div>
                      <p className="text-xs text-gray-500 font-bold uppercase tracking-widest">
                        {review.author_name ||
                          (review.name && review.surname
                            ? `${review.name} ${review.surname}`
                            : review.author_username ||
                            review.username ||
                            "Anonimowy użytkownik")}
                      </p>
                    </div>
                  </div>

                  <p className="text-gray-700 text-sm leading-relaxed">
                    {review.body || "Brak komentarza"}
                  </p>
                </div>
              ))}
            </div>
          )}

          <div className="mt-10 flex justify-end">
            <button
              onClick={onClose}
              className="px-12 py-3 bg-gray-100 text-gray-700 rounded-2xl font-bold hover:bg-gray-200 transition-all uppercase tracking-widest text-sm"
            >
              Zamknij
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
