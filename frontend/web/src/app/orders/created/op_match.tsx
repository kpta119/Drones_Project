"use client";

import { OperatorApplicantDto } from "../types";
import { FaStar, FaTimes, FaExternalLinkAlt, FaUser } from "react-icons/fa";

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
  const id = applicant.user_id;

  const handleViewProfile = (e: React.MouseEvent) => {
    e.preventDefault();
    if (id) {
      window.open(`/user_profile?user_id=${id}`, "_blank");
    }
  };

  return (
    <div className="fixed inset-0 z-99999 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md animate-fadeIn font-montserrat text-white">
      <div className="relative w-full max-w-3xl bg-slate-900 rounded-[3rem] overflow-hidden shadow-2xl p-6 lg:p-8 flex flex-col lg:flex-row gap-6 items-center border border-white/5">
        <button
          onClick={onClose}
          className="absolute top-6 right-6 text-white/40 hover:text-white transition-colors z-20"
        >
          <FaTimes size={20} />
        </button>

        <div className="w-full lg:w-1/3 flex flex-col items-center justify-center">
          <div className="w-32 h-32 bg-[#D9D9D9] rounded-full flex items-center justify-center shrink-0 shadow-xl border-4 border-white/10 relative overflow-hidden">
            <FaUser className="text-4xl text-gray-600" />
          </div>
          <div className="mt-3 flex text-yellow-400 text-base gap-1">
            {[...Array(5)].map((_, i) => (
              <FaStar
                key={i}
                className={
                  i < Math.floor(applicant.rating || 0)
                    ? "text-yellow-400"
                    : "text-gray-600"
                }
              />
            ))}
          </div>
        </div>

        <div className="w-full lg:w-2/3 flex flex-col">
          <div className="text-right">
            <h2 className="text-xl lg:text-2xl font-bold tracking-tight text-white uppercase leading-none">
              {applicant.name} {applicant.surname}
            </h2>
            <p className="text-primary-400 text-sm font-bold mt-1">
              @{applicant.username}
            </p>

            <button
              type="button"
              onClick={handleViewProfile}
              className="mt-2 inline-flex items-center gap-2 text-white/50 hover:text-primary-300 text-[9px] font-bold uppercase tracking-widest transition-colors underline underline-offset-4 cursor-pointer"
            >
              <FaExternalLinkAlt size={8} />
              Szczegóły o operatorze
            </button>
          </div>

          <div className="mt-4 space-y-2">
            <div className="text-left">
              <h4 className="text-white/40 text-[9px] font-bold uppercase tracking-widest mb-1">
                O operatorze:
              </h4>
              <p className="text-gray-200 text-xs leading-relaxed italic bg-white/5 p-4 rounded-2xl border border-white/10">
                "
                {applicant.description ||
                  "Ten operator nie dodał jeszcze opisu do swojego profilu."}
                "
              </p>
            </div>
            {applicant.certificates.length > 0 && (
              <div className="text-left">
                <h4 className="text-white/40 text-[9px] font-bold uppercase tracking-widest mb-1">
                  Certyfikaty:
                </h4>
                <div className="flex flex-wrap gap-1">
                  {applicant.certificates.map((cert, idx) => (
                    <span
                      key={idx}
                      className="text-[8px] bg-primary-900/30 text-primary-200 px-2 py-0.5 rounded-md border border-primary-500/20"
                    >
                      {cert}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div className="mt-6 flex gap-3 w-full">
            <button
              type="button"
              onClick={() => id && onReject(id)}
              className="flex-1 py-3 bg-red-600 hover:bg-red-700 rounded-xl shadow-lg font-bold text-base transition-all active:scale-95 uppercase tracking-tighter text-white cursor-pointer"
            >
              Odrzuć
            </button>
            <button
              type="button"
              onClick={() => id && onAccept(id)}
              className="flex-1 py-3 bg-green-500 hover:bg-green-600 rounded-xl shadow-lg font-bold text-base transition-all active:scale-95 uppercase tracking-tighter text-white cursor-pointer"
            >
              Zatrudnij
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
