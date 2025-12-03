"use client";

import { useState } from "react";

interface Operator {
  id: number;
  name: string;
  surname: string;
  username: string;
  certificates: Array<string>;
  services: Array<string>;
}

interface MatchResult {
  operatorId: number;
  decision: "accept" | "reject" | "pending";
}

export default function OperatorMatchingPage() {
  const exampleOperator: Operator = {
    id: 1,
    name: "Marek",
    surname: "Droński",
    username: "mdronski",
    certificates: ["A2"],
    services: ["ortofotomapa, fotogrametria"],
  };

  const [currentOperator, setCurrentOperator] =
    useState<Operator>(exampleOperator);
  const [matchResult, setMatchResult] = useState<MatchResult | null>(null);
  const [showResult, setShowResult] = useState(false);

  const accept = () => {
    const res: MatchResult = {
      operatorId: currentOperator.id,
      decision: "accept",
    };
    setMatchResult(res);
    setShowResult(true);
  };

  const reject = () => {
    const res: MatchResult = {
      operatorId: currentOperator.id,
      decision: "reject",
    };
    setMatchResult(res);
    setShowResult(true);
  };

  const handleNextOperator = () => {
    setShowResult(false);
    setMatchResult(null);
    console.log("Fetching next operator...");
  };

  const handleCheckUserAccount = () => {
    window.location.href = `/marek_profile/`;
  };

  return (
    <div
      className="p-6 mx-auto my-8 min-h-[450px]
    max-w-2xl font-opensans
    border border-gray-200
    shadow-lg rounded-3xl bg-gray-100
    transition hover:bg-gray-50 hover:shadow-xl flex flex-col justify-between"
    >
      <h1 className="font-montserrat test-xs">
        Zaakceptuj / odrzuć chętnych operatorów
      </h1>

      {!showResult ? (
        <div className="flex flex-col grow justify-between">
          <div className="space-y-4">
            <h2 className="text-center font-opensans font-bold text-2xl p-5">
              {currentOperator.name} {currentOperator.surname} (
              {currentOperator.username})
            </h2>
          </div>

          <div>
            <h3 className="text-lg space-y-2">Specjalizuje się w: </h3>
            <p className="py font-extralight">
              {currentOperator.services.join(", ")}
            </p>
          </div>

          <div>
            <h3 className="text-lg space-y-2">Certyfikaty: </h3>
            <p className="py-0.5 font-light">
              {currentOperator.certificates.join(", ")}
            </p>
          </div>

          <div className="mt-8 space-y-4">
            <button
              onClick={handleCheckUserAccount}
              className="w-full py-2 text-lg font-medium
                           border border-blue-200 shadow-2xs
                           bg-blue-200 text-blue-800 rounded-lg hover:bg-blue-300 transition"
            >
              Szczegóły operatora
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
      ) : (
        <div className="mt-4">
          {matchResult?.decision === "accept" ? (
            <div className="space-y-2">
              <h2 className="text-2xl font-bold">Zaakceptowano operatora! </h2>
              <p>Zostałeś sparowany z {currentOperator.username}!</p>
              <p className="text-sm italic text-gray-500">
                Operator zostanie wkrótce powiadomiony o akceptacji.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              <h2 className="text-2xl font-bold">Odrzucono operatora.</h2>
              <p>Odrzucono prośbę od {currentOperator.username}.</p>
            </div>
          )}

          <button
            onClick={handleNextOperator}
            className="w-full mt-4 py-2 text-white bg-orange-500 rounded-lg hover:bg-orange-600 transition"
          >
            Następny operator
          </button>
        </div>
      )}

      {matchResult && (
        <p style={{ color: "red" }}>
          debug: id: {matchResult.operatorId} / decision: {matchResult.decision}
        </p>
      )}
    </div>
  );
}
