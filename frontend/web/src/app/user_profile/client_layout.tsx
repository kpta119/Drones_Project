"use client";

import { useState } from "react";
import { ClientDto } from "./client_dto";
import OperatorRegisterButton from "@/src/components/operator_register_button";
import OperatorRegisterModule from "./operator_register/operator_register_module";

export default function ClientLayout({ data }: { data: ClientDto }) {
  const [showRegister, setShowRegister] = useState(false);

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

          @keyframes fadeIn {
            from {
              opacity: 0;
            }
            to {
              opacity: 1;
            }
          }

          @keyframes scaleIn {
            from {
              opacity: 0;
              transform: scale(0.95);
            }
            to {
              opacity: 1;
              transform: scale(1);
            }
          }

          .animate-fadeIn {
            animation: fadeIn 0.3s ease-in-out;
          }

          .animate-scaleIn {
            animation: scaleIn 0.3s ease-in-out;
          }
        `}</style>

        <div className="flex flex-col gap-3 items-center">
          <div className="flex flex-col items-center">
            <div className="w-48 h-48 bg-[#D9D9D9] rounded-full flex items-center justify-center shrink-0 drop-shadow-lg/40 hover:ring-4 hover:ring-[#D9D9D9] transition-all hover:drop-shadow-xl/50">
              <span className="text-7xl">👤</span>
            </div>
            <div className="flex text-black text-3xl pt-2">
              {[...Array(5)].map((_, i) => (
                <span key={i}>
                  {i < Math.floor(data.rating || 0) ? "★" : "☆"}
                </span>
              ))}
            </div>
          </div>

          <div className="flex flex-col justify-center items-center flex-1">
            <h2 className="text-3xl font-light">
              {data.name} {data.surname}
            </h2>
            <p className="text-gray-600 text-lg mb-4">@{data.username}</p>
            <div className="space-y-1 mb-6">
              <div className="flex items-center gap-2">
                <span>📞</span>
                <p>{data.phone_number}</p>
              </div>
              <div className="flex items-center gap-2">
                <span>✉️</span>
                <p>{data.email}</p>
              </div>
            </div>
            <OperatorRegisterButton onClick={() => setShowRegister(true)} />
          </div>
        </div>

        <div className="bg-[#D9D9D9] rounded-4xl p-6 h-full w-[90%] flex flex-col">
          <h3 className="font-light text-lg mb-3 text-center">
            Najnowsze opinie na temat {data.username}
          </h3>

          <div className="flex-1">
            {data.reviews?.length > 0 ? (
              data.reviews.slice(0, 3).map((review, idx) => (
                <div key={idx} className="mb-3 text-sm">
                  <p className="font-semibold">{review.author_id}</p>
                  <p className="text-gray-700">{review.body}</p>
                </div>
              ))
            ) : (
              <p className="text-gray-600 text-sm">Brak opinii</p>
            )}
          </div>

          <button className="w-full bg-gray-700 text-white rounded-2xl py-2 font-semibold hover:cursor-pointer hover:bg-primary-800 hover:ring-2 hover:ring-primary-800 transition-all text-sm mt-4">
            Sprawdź więcej opinii
          </button>
        </div>
      </div>

      {showRegister && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-fadeIn">
          <div className="bg-white rounded-2xl p-8 max-w-2xl w-full max-h-[90vh] overflow-y-auto relative animate-scaleIn">
            <button
              onClick={() => setShowRegister(false)}
              className="absolute top-4 right-4 text-gray-500 hover:text-gray-700 text-2xl font-bold"
            >
              ✕
            </button>
            <OperatorRegisterModule onClose={() => setShowRegister(false)} />
          </div>
        </div>
      )}
    </>
  );
}
