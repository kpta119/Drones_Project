"use client";

import { useState } from "react";
import { ClientDto } from "./client_dto";
import OperatorRegisterModule from "./operator_register/operator_register_module";
import ReviewsView from "@/src/app/orders/utils/reviews_view";
import { FaStar, FaUser, FaPhone, FaEnvelope } from "react-icons/fa";

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
  reviews,
  averageRating,
}: {
  data: ClientDto;
  isOwnProfile: boolean;
  reviews: Review[];
  averageRating: number;
}) {
  const [showRegister, setShowRegister] = useState(false);
  const [showReviews, setShowReviews] = useState(false);

  const recentReviews = reviews.slice(0, 3);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 py-10">
      <div className="max-w-7xl mx-auto px-6 space-y-6">
        {/* Główna karta profilu */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Lewa kolumna - info o użytkowniku */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-3xl p-8 shadow-lg border border-gray-200">
              <div className="flex flex-col items-center">
                {/* Avatar */}
                <div className="w-40 h-40 bg-gradient-to-br from-primary-100 to-primary-200 rounded-full flex items-center justify-center mb-4 border-4 border-primary-300 shadow-md">
                  <FaUser className="text-6xl text-primary-600" />
                </div>

                {/* Nazwa i username */}
                <h2 className="text-2xl font-bold text-gray-900 text-center mb-1">
                  {data.name} {data.surname}
                </h2>
                <p className="text-primary-600 font-semibold text-sm mb-1">@{data.username}</p>
                <p className="text-xs text-gray-500 mb-4">Klient</p>

                {/* Rating */}
                <div className="flex gap-1 mb-6">
                  {[...Array(5)].map((_, i) => (
                    <FaStar
                      key={i}
                      size={20}
                      className={
                        i < Math.floor(averageRating)
                          ? "text-amber-400"
                          : "text-gray-300"
                      }
                    />
                  ))}
                </div>

                {/* Kontakt */}
                <div className="w-full space-y-2 mb-6">
                  <div className="flex items-center gap-3 bg-gray-50 rounded-xl p-3 border border-gray-200">
                    <FaPhone className="text-primary-600" size={14} />
                    <p className="text-gray-700 text-sm">{data.phone_number}</p>
                  </div>
                  <div className="flex items-center gap-3 bg-gray-50 rounded-xl p-3 border border-gray-200">
                    <FaEnvelope className="text-primary-600" size={14} />
                    <p className="text-gray-700 text-sm break-all">{data.email}</p>
                  </div>
                </div>

                {/* Przyciski akcji */}
                {isOwnProfile && data.role !== "ADMIN" && (
                  <button
                    onClick={() => setShowRegister(true)}
                    className="w-full bg-primary-600 hover:bg-primary-700 text-white rounded-xl py-2.5 font-semibold transition-all"
                  >
                    Zostań operatorem
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Prawa kolumna - Opinie */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-3xl p-8 shadow-lg border border-gray-200">
              <h3 className="text-xl font-bold text-gray-900 mb-6">
                Najnowsze opinie na temat {data.username}
              </h3>

              <div className="space-y-4 max-h-96 overflow-y-auto pr-2">
                {recentReviews?.length > 0 ? (
                  recentReviews.map((review, idx) => (
                    <div
                      key={idx}
                      className="bg-gray-50 p-4 rounded-2xl border border-gray-200"
                    >
                      <div className="flex items-center gap-2 mb-2">
                        {[...Array(5)].map((_, i) => (
                          <FaStar
                            key={i}
                            size={14}
                            className={
                              i < review.stars ? "text-amber-400" : "text-gray-300"
                            }
                          />
                        ))}
                      </div>
                      <p className="font-semibold text-sm text-gray-800 mb-2">
                        {review.author_name ||
                          (review.name && review.surname
                            ? `${review.name} ${review.surname}`
                            : review.author_username ||
                              review.username ||
                              "Anonimowy")}
                      </p>
                      <p className="text-gray-700 leading-relaxed">{review.body}</p>
                    </div>
                  ))
                ) : (
                  <div className="flex items-center justify-center h-40">
                    <p className="text-gray-500 italic">Brak opinii</p>
                  </div>
                )}
              </div>

              <button
                onClick={() => setShowReviews(true)}
                className="w-full bg-gray-100 hover:bg-gray-200 text-gray-900 rounded-xl py-2.5 font-semibold transition-all border border-gray-300 mt-6"
              >
                Sprawdź więcej opinii
              </button>
            </div>
          </div>
        </div>
      </div>

      {showRegister && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-3xl w-full max-w-5xl h-[90vh] overflow-auto p-8 relative shadow-2xl">
            <button
              onClick={() => setShowRegister(false)}
              className="absolute top-6 right-6 text-gray-500 hover:text-gray-900 text-2xl font-bold"
            >
              ✕
            </button>
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
    </div>
  );
}
