"use client";

export function OperatorRegisterIntroModule({
  onNext,
}: {
  onNext: () => void;
}) {
  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 m-10">
        <h2 className="text-4xl font-bold mb-6">
          Zakładasz konto operatora dronów
        </h2>
        <p className="text-gray-700 mb-4 text-lg">
          Od chwili zmiany statusu konta na status operatora, odblokujesz
          następujące funkcje portalu:
        </p>
        <ul className="space-y-2 mb-6 text-gray-700 text-lg">
          <li className="flex items-start gap-2">
            <span>•</span>
            <span>wystawianie recenzji pracodawcom</span>
          </li>
          <li className="flex items-start gap-2">
            <span>•</span>
            <span>
              możliwość wprowadzenia danych o własnym portfolio, w tym:
            </span>
          </li>
          <li className="flex items-start gap-4 ml-4">
            <span>•</span>
            <span>własnych zdjęć</span>
          </li>
          <li className="flex items-start gap-4 ml-4">
            <span>•</span>
            <span>specjalności oraz kwalifikacji</span>
          </li>
          <li className="flex items-start gap-4 ml-4">
            <span>•</span>
            <span>opisu konta lub swojej działalności</span>
          </li>
        </ul>
        <p className="text-gray-700 text-lg">
          Jako operator, nadal będziesz móc wystawiać oferty pracy.
        </p>
      </div>

      <div className="flex justify-between gap-4 pt-8 border-t">
        <button
          disabled
          className="px-6 py-2 bg-gray-300 text-gray-600 rounded-lg cursor-not-allowed"
        >
          Wróć
        </button>
        <button
          onClick={onNext}
          className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-all"
        >
          Dalej
        </button>
      </div>
    </div>
  );
}
