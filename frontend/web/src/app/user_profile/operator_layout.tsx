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
    <div>
      <div
        className="grid grid-cols-2 grid-rows-1 gap-10 ps-5 pt-10 pb-10 m-auto font-montserrat w-7xl"
        style={{ height: "50vh" }}
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

        <div className="rounded-2xl p-8 flex flex-col">
          <div className="flex gap-6">
            <div className="flex flex-col items-center gap-1">
              <div className="w-40 h-40 lg:w-48 lg:h-48 bg-[#D9D9D9] rounded-full flex items-center justify-center shrink-0 drop-shadow-lg ring-2 ring-primary-700 hover:ring-4 hover:ring-[#D9D9D9] transition-all">
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
              <p className="text-sm font-semibold text-center shine-text">
                Operator dronów
              </p>
            </div>

            <div className="flex flex-col justify-center pl-5 flex-1 ms-4">
              <h2 className="text-3xl font-light">
                {data.name} {data.surname}
              </h2>
              <p className="text-gray-600 text-lg mb-4">@{data.username}</p>
              <div className="space-y-1 mb-6">
                <div className="flex items-center justify-center sm:justify-start gap-2">
                  <FaPhone className="text-primary-700" size={16} />
                  <p>{data.phone_number}</p>
                </div>
                <div className="flex items-center justify-center sm:justify-start gap-2">
                  <FaEnvelope className="text-primary-700" size={16} />
                  <p className="break-all">{data.email}</p>
                </div>
              </div>

              <div className="flex flex-col w-full pt-4 gap-2">
                <button
                  onClick={() => setShowReviews(true)}
                  className="flex-1 bg-gray-200 text-black rounded-xl py-1 font-semibold hover:bg-gray-300 transition-all text-sm"
                >
                  Czytaj opinie
                </button>

                {isOwnProfile && (
                  <button
                    onClick={() => setShowEdit(true)}
                    className="flex-1 bg-primary-600 text-white rounded-xl py-1 font-semibold hover:bg-primary-700 transition-all text-sm"
                  >
                    Edytuj dane
                  </button>
                )}
                {isOwnProfile && (
                  <button
                    onClick={() => setShowEditPortfolio(true)}
                    className="flex-1 bg-primary-600 text-white rounded-xl py-1 font-semibold hover:bg-primary-700 transition-all text-sm"
                  >
                    Edytuj portfolio
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="bg-gray-300 rounded-2xl p-6">
          <h3 className="font-semibold mb-3">O mnie:</h3>
          <p className="text-gray-700 text-sm">{data.description}</p>
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
            <div className="bg-white rounded-2xl w-full max-w-5xl h-[90vh] overflow-auto p-6 relative">
              <button
                onClick={() => setShowEdit(false)}
                className="absolute top-4 right-4 text-gray-500 hover:text-gray-700 text-2xl font-bold"
              >
                ✕
              </button>
              <OperatorUpdateModule onClose={() => { setShowEdit(false); }} />
            </div>
          </div>
        )}
      </div>
      <div
        className="grid grid-cols-1 grid-rows-1 gap-10 ps-5 pt-10 pb-10 m-auto font-montserrat w-7xl"
        style={{ height: "50vh" }}

      ><Portfolio /></div>
    </div >
  );
}
