"use client";

import Image from "next/image";
import { OperatorApplicantDto } from "../types";

interface OpMatchProps {
  applicant: OperatorApplicantDto;
  onAccept: (id: string) => void;
  onReject: (id: string) => void;
  onClose: () => void;
}

export default function OpMatch({
  applicant,
  onAccept,
  onReject,
  onClose,
}: OpMatchProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="relative w-full max-w-5xl bg-slate-900 rounded-[3rem] overflow-hidden shadow-2xl p-6 lg:p-10 text-white flex flex-col lg:flex-row gap-10">
        <button
          onClick={onClose}
          className="absolute top-8 right-8 text-white/50 hover:text-white text-2xl"
        >
          ✕
        </button>

        <div className="w-full lg:w-1/2 aspect-square lg:aspect-auto bg-gray-800 rounded-[2.5rem] overflow-hidden border-4 border-white/10 relative">
          <Image
            src={applicant.photoUrl || "/api/placeholder/600/800"}
            alt="Profile"
            fill
            className="object-cover"
          />
        </div>

        <div className="w-full lg:w-1/2 flex flex-col justify-between">
          <div>
            <div className="text-right">
              <h2 className="text-4xl font-bold">
                {applicant.name} {applicant.surname}
              </h2>
              <p className="text-primary-500 font-medium text-lg">
                @{applicant.username}
              </p>
              <div className="flex justify-end text-yellow-400 text-2xl mt-2">
                {"★".repeat(Math.floor(applicant.rating))}
                {"☆".repeat(5 - Math.floor(applicant.rating))}
              </div>
            </div>

            <div className="mt-8">
              <h4 className="text-gray-400 text-lg mb-2">O operatorze:</h4>
              <p className="text-gray-300 leading-relaxed italic">
                "{applicant.description}"
              </p>
            </div>
          </div>

          <div className="mt-12 flex gap-4">
            <button
              onClick={() => onReject(applicant.id)}
              className="flex-1 py-4 bg-red-600 rounded-2xl font-bold text-xl"
            >
              Odrzuć
            </button>
            <button
              onClick={() => onAccept(applicant.id)}
              className="flex-1 py-4 bg-green-500 rounded-2xl font-bold text-xl"
            >
              Zatrudnij
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
