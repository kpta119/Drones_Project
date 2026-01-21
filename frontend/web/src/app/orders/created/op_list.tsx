"use client";

import { OperatorApplicantDto } from "../types";
import {
  FaStar,
  FaTimes,
  FaExternalLinkAlt,
  FaUser,
  FaCheck,
  FaBan,
} from "react-icons/fa";

interface OpListProps {
  applicants: OperatorApplicantDto[];
  onAccept: (id: string) => void;
  onReject: (id: string) => void;
  onClose: () => void;
}

export default function OpList({
  applicants,
  onAccept,
  onReject,
  onClose,
}: OpListProps) {
  const handleViewProfile = (id: string, e: React.MouseEvent) => {
    e.preventDefault();
    if (id) {
      window.open(`/user_profile?user_id=${id}`, "_blank");
    }
  };

  return (
    <div className="fixed inset-0 z-99999 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md animate-fadeIn font-montserrat text-white">
      <div className="relative w-full max-w-4xl max-h-[85vh] bg-slate-900 rounded-[2rem] overflow-hidden shadow-2xl border border-white/5 flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-white/10">
          <h2 className="text-xl font-bold uppercase tracking-widest">
            Chętni operatorzy ({applicants.length})
          </h2>
          <button
            onClick={onClose}
            className="text-white/40 hover:text-white transition-colors"
          >
            <FaTimes size={20} />
          </button>
        </div>

        {/* List */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {applicants.map((applicant) => {
            const id = applicant.user_id;
            const rating = applicant.average_stars;

            return (
              <div
                key={id}
                className="bg-white/5 rounded-2xl p-5 border border-white/10 hover:border-primary-500/30 transition-all"
              >
                <div className="flex flex-col md:flex-row gap-4 items-center">
                  {/* Avatar & Rating */}
                  <div className="flex flex-col items-center shrink-0">
                    <div className="w-20 h-20 bg-[#D9D9D9] rounded-full flex items-center justify-center shadow-lg border-2 border-white/10 overflow-hidden">
                      <FaUser className="text-2xl text-gray-600" />
                    </div>
                    {rating != null && rating > 0 ? (
                      <>
                        <div className="mt-2 flex text-yellow-400 text-base gap-0.5">
                          {[...Array(5)].map((_, i) => (
                            <FaStar
                              key={i}
                              className={
                                i < Math.floor(rating)
                                  ? "text-yellow-400"
                                  : "text-gray-600"
                              }
                            />
                          ))}
                        </div>
                        <span className="text-sm text-gray-400 mt-1">
                          {rating.toFixed(1)} / 5
                        </span>
                      </>
                    ) : (
                      <span className="text-sm text-gray-500 mt-2">
                        Brak ocen
                      </span>
                    )}
                  </div>

                  {/* Info */}
                  <div className="flex-1 text-center md:text-left">
                    <h3 className="text-xl font-bold text-white">
                      {applicant.name} {applicant.surname}
                    </h3>
                    <p className="text-primary-400 text-base font-medium">
                      @{applicant.username}
                    </p>

                    <p className="text-gray-300 text-sm mt-2 line-clamp-2">
                      {applicant.description ||
                        "Ten operator nie dodał jeszcze opisu."}
                    </p>

                    {applicant.certificates.length > 0 && (
                      <div className="flex flex-wrap gap-2 mt-3 justify-center md:justify-start">
                        {applicant.certificates.slice(0, 3).map((cert, idx) => (
                          <span
                            key={idx}
                            className="text-sm bg-primary-900/30 text-primary-200 px-3 py-1 rounded-lg border border-primary-500/20"
                          >
                            {cert}
                          </span>
                        ))}
                        {applicant.certificates.length > 3 && (
                          <span className="text-sm text-gray-400">
                            +{applicant.certificates.length - 3} więcej
                          </span>
                        )}
                      </div>
                    )}

                    <button
                      type="button"
                      onClick={(e) => handleViewProfile(id, e)}
                      className="mt-3 inline-flex items-center gap-2 text-white/50 hover:text-primary-300 text-xs font-bold uppercase tracking-widest transition-colors underline underline-offset-4 cursor-pointer"
                    >
                      <FaExternalLinkAlt size={10} />
                      Zobacz profil
                    </button>
                  </div>

                  {/* Actions */}
                  <div className="flex md:flex-col gap-2 shrink-0">
                    <button
                      type="button"
                      onClick={() => id && onAccept(id)}
                      className="flex items-center gap-2 px-5 py-3 bg-green-500 hover:bg-green-600 rounded-xl shadow-lg font-bold text-sm transition-all active:scale-95 uppercase tracking-tight text-white cursor-pointer"
                    >
                      <FaCheck size={14} />
                      Zatrudnij
                    </button>
                    <button
                      type="button"
                      onClick={() => id && onReject(id)}
                      className="flex items-center gap-2 px-5 py-3 bg-red-600 hover:bg-red-700 rounded-xl shadow-lg font-bold text-sm transition-all active:scale-95 uppercase tracking-tight text-white cursor-pointer"
                    >
                      <FaBan size={14} />
                      Odrzuć
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}