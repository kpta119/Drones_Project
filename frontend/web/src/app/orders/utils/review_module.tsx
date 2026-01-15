"use client";

import { useState } from "react";
import { FaTimes, FaStar } from "react-icons/fa";

interface ReviewModuleProps {
  operatorId: string;
  operatorName: string;
  orderId: string;
  onClose: () => void;
  onSubmit: (review: { rating: number; comment: string }) => Promise<void>;
  isClientReview?: boolean;
}

export default function ReviewModule({
  operatorId,
  operatorName,
  orderId,
  onClose,
  onSubmit,
  isClientReview = false,
}: ReviewModuleProps) {
  const [rating, setRating] = useState(0);
  const [hoveredRating, setHoveredRating] = useState(0);
  const [comment, setComment] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (rating === 0) {
      setError("Musisz wybrać ocenę");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await onSubmit({
        rating,
        comment,
      });
      onClose();
    } catch (err: any) {
      const errorMessage =
        err?.message || "Błąd przy wysyłaniu recenzji. Spróbuj ponownie.";
      setError(errorMessage);
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-99999 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn font-montserrat text-black">
      <div className="bg-white w-full max-w-2xl rounded-[3rem] overflow-hidden shadow-2xl relative border border-white/20 p-8 lg:p-12">
        <button
          onClick={onClose}
          className="absolute top-8 right-8 text-gray-400 hover:text-black transition-colors"
        >
          <FaTimes size={24} />
        </button>

        <div className="mb-8">
          <h2 className="text-3xl font-bold text-gray-900 tracking-tight mb-2">
            {isClientReview ? "Oceń klienta" : "Oceń operatora"}
          </h2>
          <p className="text-gray-600 font-medium">
            {isClientReview
              ? "Twoja opinia pomaga innym operatorom"
              : "Twoja opinia pomaga innym klientom"}
          </p>
        </div>

        <div className="bg-primary-50 p-6 rounded-2xl border border-primary-200 mb-8">
          <p className="text-sm font-bold text-primary-700 uppercase tracking-widest mb-2">
            {isClientReview ? "Klient" : "Wykonawca"}
          </p>
          <p className="text-2xl font-bold text-gray-900">{operatorName}</p>
        </div>

        <div className="mb-8">
          <label className="block text-xs font-bold text-primary-800 mb-4 uppercase tracking-widest">
            Twoja ocena
          </label>
          <div className="flex gap-4 justify-center">
            {[1, 2, 3, 4, 5].map((star) => (
              <button
                key={star}
                onMouseEnter={() => setHoveredRating(star)}
                onMouseLeave={() => setHoveredRating(0)}
                onClick={() => setRating(star)}
                className="transition-transform hover:scale-110 active:scale-95"
              >
                <FaStar
                  size={48}
                  className={`${
                    star <= (hoveredRating || rating)
                      ? "text-primary-400"
                      : "text-gray-200"
                  } transition-colors`}
                />
              </button>
            ))}
          </div>
          {rating > 0 && (
            <p className="text-center mt-4 text-sm font-bold text-primary-700">
              Twoja ocena: {rating} / 5 ★
            </p>
          )}
        </div>

        <div className="mb-8">
          <label className="block text-xs font-bold text-primary-800 mb-2 uppercase tracking-widest">
            Komentarz (opcjonalny)
          </label>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Podziel się swoimi wrażeniami..."
            rows={4}
            className="w-full px-6 py-4 bg-gray-50 border-2 border-transparent focus:border-primary-300 focus:bg-white rounded-2xl outline-none font-medium transition-all text-black"
          />
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 rounded-r-2xl text-sm font-bold">
            ⚠️ {error}
          </div>
        )}

        <div className="flex gap-4">
          <button
            onClick={onClose}
            disabled={loading}
            className="flex-1 py-4 bg-gray-100 text-gray-500 rounded-2xl font-bold hover:bg-gray-200 disabled:opacity-30 transition-all uppercase tracking-widest text-sm"
          >
            Anuluj
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading || rating === 0}
            className="flex-1 py-4 bg-primary-300 text-primary-900 rounded-2xl font-bold shadow-lg hover:bg-primary-400 disabled:opacity-30 transition-all uppercase tracking-widest text-sm"
          >
            {loading ? "Wysyłanie..." : "Wyślij recenzję"}
          </button>
        </div>
      </div>
    </div>
  );
}
