"use client";

import { useState, useEffect } from "react";

interface Offer {
  id: number;
  clientName: string;
  clientSurname: string;
  clientUsername: string;
  shortDesc: string;
  location: string;
  service: string;
  endDate: Date;
}

interface MatchResult {
  offerId: number;
  decision: "accept" | "reject" | "pending";
}

export default function OfferMatchingPage() {
  const exampleOffer: Offer = {
    id: 1,
    clientName: "Andrzej",
    clientSurname: "Jeziorski",
    clientUsername: "ajezior",
    location: "api google maps",
    shortDesc: "Chcę zdjęć mojej działki",
    service: "ortofotomapa",
    endDate: new Date(2025, 10, 24, 23, 59),
  };

  const [currentOffer, setCurrentOffer] = useState<Offer>(exampleOffer);
  const [matchResult, setMatchResult] = useState<MatchResult | null>(null);
  const [showResult, setShowResult] = useState(false);
  const [formattedDate, setFormattedDate] = useState<string>("");

  useEffect(() => {
    setFormattedDate(currentOffer.endDate.toLocaleString());
  }, [currentOffer]);

  const accept = () => {
    const res: MatchResult = {
      offerId: currentOffer.id,
      decision: "accept",
    };
    setMatchResult(res);
    setShowResult(true);
  };

  const reject = () => {
    const res: MatchResult = {
      offerId: currentOffer.id,
      decision: "reject",
    };
    setMatchResult(res);
    setShowResult(true);
  };

  const handleNextOffer = () => {
    setShowResult(false);
    setMatchResult(null);
    console.log("Fetching next Offer...");
  };

  const handleCheckUserAccount = () => {
    window.location.href = `/sara_profile/`;
  };

  return (
    <div
      className="p-6 mx-auto my-8 min-h-[450px]
    max-w-2xl font-opensans
    border border-gray-200
    shadow-lg rounded-3xl bg-gray-100
    transition hover:bg-gray-50 hover:shadow-xl flex flex-col justify-between"
    >
      <h1 className="font-montserrat text-xs">
        Zgłoś chęć / brak chęci spełnienia zlecenia
      </h1>

      {!showResult ? (
        <div className="flex flex-col grow justify-between">
          <div className="space-y-4">
            <div>
              <h2 className="text-center font-opensans font-bold text-2xl p-5">
                Zlecenie od {currentOffer.clientName}{" "}
                {currentOffer.clientSurname} ({currentOffer.clientUsername})
              </h2>
            </div>

            <div className="text-lg space-y-2">
              <h3 className="py-1 font-light">
                Skrócony opis zlecenia: {currentOffer.shortDesc}
              </h3>
              <h3 className="py-1 font-light">
                Typ zlecenia: {currentOffer.service}
              </h3>
              <h3 className="py-1 font-light">
                Spodziewa się wykonania zlecenia do: {formattedDate}
              </h3>
            </div>
          </div>

          <div className="mt-8 space-y-4">
            <div>
              <button
                onClick={handleCheckUserAccount}
                className="w-full py-2 text-lg font-medium
                           border border-blue-200 shadow-2xs
                           bg-blue-200 text-blue-800 rounded-lg hover:bg-blue-300 transition"
              >
                Szczegóły zlecającego
              </button>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <button
                onClick={reject}
                className="py-2 border border-red-400 text-red-600 rounded-lg hover:bg-red-100 transition"
              >
                Odrzuć prośbę
              </button>
              <button
                onClick={accept}
                className="py-2 border border-green-400 bg-green-500 text-white rounded-lg hover:bg-green-600 transition"
              >
                Akceptuj prośbę
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div className="mt-4">
          {matchResult?.decision === "accept" ? (
            <div className="space-y-2">
              <h2 className="text-2xl font-bold">
                Zgłosiłeś się jako chętny!{" "}
              </h2>
              <p>Zlecający zostanie wkrótce powiadomiony o twoim zgłoszeniu.</p>
              <p className="text-sm italic text-gray-500">
                Zlecający musi zdecydować, czy chcę z tobą współpracować. Jeżeli
                zostaniesz wybrany do współpracy, powiadomimy cię o tym!
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              <h2 className="text-2xl font-bold">Odrzucono zlecenie.</h2>
              <p>Czy chcesz zobaczyć inne, dostępne zlecenia?</p>
            </div>
          )}

          <button
            onClick={handleNextOffer}
            className="w-full mt-4 py-2 text-white bg-orange-500 rounded-lg hover:bg-orange-600 transition"
          >
            Następne zlecenie
          </button>
        </div>
      )}

      {matchResult && (
        <p className="text-red-500 text-xs mt-4">
          debug: id: {matchResult.offerId} / decision: {matchResult.decision}
        </p>
      )}
    </div>
  );
}
