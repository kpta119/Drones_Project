"use client";

export function OperatorRegisterIntroModule({
  onNext,
}: {
  onNext: () => void;
}) {
  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 p-2 lg:p-10">
        <h2 className="text-2xl lg:text-4xl font-bold mb-4 lg:mb-6 leading-tight">
          Zakładasz konto operatora dronów
        </h2>
        <p className="text-gray-700 mb-4 text-base lg:text-lg">
          Od chwili zmiany statusu konta, odblokujesz następujące funkcje:
        </p>
        <ul className="space-y-3 mb-6 text-gray-700 text-sm lg:text-lg">
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

      <div className="flex justify-between gap-4 pt-6 border-t mt-auto">
        <button
          disabled
          className="px-4 lg:px-6 py-2 bg-gray-200 text-gray-400 rounded-lg cursor-not-allowed text-sm"
        >
          Wróć
        </button>
        <button
          onClick={onNext}
          className="px-6 lg:px-8 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-all font-bold"
        >
          Dalej
        </button>
      </div>
    </div>
  );
}
