"use client";

import { useState } from "react";
import { ClientDto } from "./client_dto";
import OperatorRegisterButton from "@/src/components/operator_register_button";
import OperatorRegisterModule from "./operator_register/operator_register_module";
import ReviewsView from "@/src/app/orders/utils/reviews_view";

export default function ClientLayout({
  data,
  isOwnProfile,
}: {
  data: ClientDto;
  isOwnProfile: boolean;
}) {
  const [showRegister, setShowRegister] = useState(false);
  const [showReviews, setShowReviews] = useState(false);

  return (
    <>
      <div className="grid grid-cols-1 lg:grid-cols-2 justify-center items-center gap-10 p-5 lg:pl-10 lg:pr-10 lg:pt-10 lg:pb-10 m-auto font-montserrat w-full max-w-7xl h-auto lg:h-[85vh]">
        <style>{`
          @keyframes colorShine {
            0%, 100% { color: var(--color-primary-500); }
            50% { color: var(--color-primary-900); }
          }
          .shine-text { animation: colorShine 5s ease-in-out infinite; }

          @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
          @keyframes scaleIn { from { opacity: 0; transform: scale(0.95); } to { opacity: 1; transform: scale(1); } }

          .animate-fadeIn { animation: fadeIn 0.3s ease-in-out; }
          .animate-scaleIn { animation: scaleIn 0.3s ease-in-out; }
        `}</style>

        <div className="flex flex-col gap-3 items-center min-h-[400px] lg:min-h-0">
          <div className="flex flex-col items-center">
            <div className="w-40 h-40 lg:w-48 lg:h-48 bg-[#D9D9D9] rounded-full flex items-center justify-center shrink-0 drop-shadow-lg/40 hover:ring-4 hover:ring-[#D9D9D9] transition-all hover:drop-shadow-xl/50">
              <span className="text-6xl lg:text-7xl">👤</span>
            </div>
            <div className="flex text-black text-2xl lg:text-3xl pt-2">
              {[...Array(5)].map((_, i) => (
                <span key={i}>
                  {i < Math.floor(data.rating || 0) ? "★" : "☆"}
                </span>
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
                <span>📞</span>
                <p>{data.phone_number}</p>
              </div>
              <div className="flex items-center justify-center gap-2">
                <span>✉️</span>
                <p className="break-all">{data.email}</p>
              </div>
            </div>
            {isOwnProfile && (
              <OperatorRegisterButton onClick={() => setShowRegister(true)} />
            )}
          </div>
        </div>

        <div className="bg-[#D9D9D9] rounded-3xl lg:rounded-4xl p-6 h-full w-full lg:w-[90%] flex flex-col min-h-[350px] lg:min-h-0 mx-auto">
          <h3 className="font-light text-lg mb-4 text-center">
            Najnowsze opinie na temat {data.username}
          </h3>

          <div className="flex-1 overflow-y-auto pr-2">
            {data.reviews?.length > 0 ? (
              data.reviews.slice(0, 3).map((review, idx) => (
                <div
                  key={idx}
                  className="mb-4 text-sm bg-white/30 p-3 rounded-xl"
                >
                  <p className="font-semibold text-xs text-gray-500 mb-1">
                    Autor ID: {review.author_id}
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
          userId={data.id}
          userName={`${data.name} ${data.surname}`}
          onClose={() => setShowReviews(false)}
        />
      )}
    </>
  );
}
