"use client";

import { useState } from "react";
import { OperatorDto } from "./operator_dto";
import ReviewsView from "@/src/app/orders/utils/reviews_view";
import OperatorUpdateModule from "./operator_update/operator_update_module";

export default function OperatorLayout({
  data,
  isOwnProfile,
}: {
  data: OperatorDto;
  isOwnProfile: boolean;
}) {
  const [showReviews, setShowReviews] = useState(false);
  const [showEdit, setShowEdit] = useState(false);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 lg:grid-rows-2 gap-8 lg:gap-10 p-5 lg:ps-5 lg:pt-10 lg:pb-10 m-auto font-montserrat w-full max-w-7xl h-auto lg:h-[85vh]">
      <style>{`
        @keyframes colorShine {
          0%, 100% { color: var(--color-primary-500); }
          50% { color: var(--color-primary-900); }
        }
        .shine-text {
          animation: colorShine 5s ease-in-out infinite;
        }
      `}</style>

      <div className="order-1 rounded-2xl p-4 lg:p-8 flex flex-col min-h-[300px] lg:min-h-0">
        <div className="flex flex-col sm:flex-row gap-6 items-center sm:items-start lg:items-center h-full">
          <div className="flex flex-col items-center gap-1">
            <div className="w-40 h-40 lg:w-48 lg:h-48 bg-[#D9D9D9] rounded-full flex items-center justify-center shrink-0 drop-shadow-lg ring-2 ring-primary-700 hover:ring-4 hover:ring-[#D9D9D9] transition-all">
              <span className="text-6xl lg:text-7xl">👤</span>
            </div>
            <div className="flex text-black text-2xl lg:text-3xl pt-2">
              {[...Array(5)].map((_, i) => (
                <span key={i}>
                  {i < Math.floor(data.rating || 0) ? "★" : "☆"}
                </span>
              ))}
            </div>
            <p className="text-sm font-semibold text-center shine-text">
              Operator dronów
            </p>
          </div>

          <div className="flex flex-col justify-center text-center sm:text-left sm:pl-5 flex-1 ms-0 sm:ms-4 h-full">
            <h2 className="text-2xl lg:text-3xl font-light">
              {data.name} {data.surname}
            </h2>
            <p className="text-gray-600 text-lg mb-4">@{data.username}</p>
            <div className="space-y-1 mb-6">
              <div className="flex items-center justify-center sm:justify-start gap-2">
                <span>📞</span>
                <p>{data.phone_number}</p>
              </div>
              <div className="flex items-center justify-center sm:justify-start gap-2">
                <span>✉️</span>
                <p className="break-all">{data.email}</p>
              </div>
            </div>

            <div className="flex flex-col w-full pt-4 gap-2">
              <button
                onClick={() => setShowReviews(true)}
                className="flex-1 bg-[#D9D9D9] text-black rounded-xl py-1 font-semibold hover:bg-gray-400 hover:ring-2 hover:ring-gray-400 transition-all text-sm"
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
            </div>
          </div>
        </div>
      </div>

      <div className="order-2 bg-gray-300 rounded-2xl p-6 min-h-[200px] lg:min-h-0">
        <h3 className="font-semibold mb-3">O mnie:</h3>
        <p className="text-gray-700 text-sm">
          {data.description || "Brak opisu."}
        </p>
      </div>

      <div className="order-3 lg:order-4 bg-gray-300 rounded-2xl p-6 min-h-[200px] lg:min-h-0">
        <h3 className="font-semibold mb-3">Usługi:</h3>
        <ul className="text-sm space-y-2 text-gray-700">
          {data.operatorServices?.length ? (
            data.operatorServices.map((service) => (
              <li key={service.id}>• {service.serviceName}</li>
            ))
          ) : (
            <li>Brak zdefiniowanych usług.</li>
          )}
        </ul>
      </div>

      <div className="order-4 lg:order-3 bg-gray-300 rounded-2xl overflow-hidden relative min-h-[250px] lg:min-h-0">
        <div className="absolute inset-0 bg-linear-to-t from-black/40 to-transparent flex items-center justify-center hover:bg-black/30 hover:cursor-pointer transition-all">
          <p className="text-white text-lg font-semibold text-center px-4">
            Sprawdź zdjęcia{" "}
            <span className="font-extrabold">{data.username}</span>
          </p>
        </div>
      </div>

      {showReviews && (
        <ReviewsView
          userName={`${data.name} ${data.surname}`}
          onClose={() => setShowReviews(false)}
        />
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
            <OperatorUpdateModule
              onClose={() => setShowEdit(false)}
            />
          </div>
        </div>
      )}
    </div>
  );
}