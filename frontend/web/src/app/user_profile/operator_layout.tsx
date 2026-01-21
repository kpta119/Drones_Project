"use client";

import { useState } from "react";
import { OperatorDto } from "./operator_dto";
import ReviewsView from "@/src/app/orders/utils/reviews_view";
import OperatorUpdateModule from "./operator_update/operator_update_module";
import { FaStar, FaUser, FaPhone, FaEnvelope } from "react-icons/fa";
import Portfolio from "./portfolio/portfolio_view";
import EditPortfolio from "./portfolio/portfolio_edit";

export default function OperatorLayout({
  data,
  isOwnProfile,
  averageRating,
}: {
  data: OperatorDto;
  isOwnProfile: boolean;
  reviews?: { body: string; stars: number }[];
  averageRating: number;
}) {
  const [showReviews, setShowReviews] = useState(false);
  const [showEdit, setShowEdit] = useState(false);
  const [showEditPortfolio, setShowEditPortfolio] = useState(false);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 py-10">
      <div className="max-w-7xl mx-auto px-6 space-y-6">
        {/* Główna karta profilu */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Lewa kolumna - info o użytkowniku */}
          <div className="lg:col-span-1 space-y-6">
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
                <p className="text-xs text-gray-500 mb-4">Operator dronów</p>

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
                <div className="w-full space-y-2">
                  <button
                    onClick={() => setShowReviews(true)}
                    className="w-full bg-gray-100 hover:bg-gray-200 text-gray-900 rounded-xl py-2.5 font-semibold transition-all border border-gray-300"
                  >
                    Czytaj opinie
                  </button>

                  {isOwnProfile && (
                    <>
                      <button
                        onClick={() => setShowEdit(true)}
                        className="w-full bg-primary-600 hover:bg-primary-700 text-white rounded-xl py-2.5 font-semibold transition-all"
                      >
                        Edytuj dane
                      </button>
                      <button
                        onClick={() => setShowEditPortfolio(true)}
                        className="w-full bg-primary-600 hover:bg-primary-700 text-white rounded-xl py-2.5 font-semibold transition-all"
                      >
                        Edytuj portfolio
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Portfolio - więcej miejsca */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-3xl shadow-lg border border-gray-200 overflow-hidden">
              <Portfolio />
            </div>
          </div>
        </div>
      </div>


        {showReviews && (
          <ReviewsView
            userName={`${data.name} ${data.surname}`}
            onClose={() => setShowReviews(false)}
          />
        )}

        {showEditPortfolio && (
          <EditPortfolio onClose={() => setShowEditPortfolio(false)} />
        )}

        {showEdit && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="bg-white rounded-3xl w-full max-w-5xl h-[90vh] overflow-auto p-8 relative shadow-2xl">
              <button
                onClick={() => setShowEdit(false)}
                className="absolute top-6 right-6 text-gray-500 hover:text-gray-900 text-2xl font-bold"
              >
                ✕
              </button>
              <OperatorUpdateModule onClose={() => { setShowEdit(false); }} />
            </div>
          </div>
        )}
    </div >
  );
}
