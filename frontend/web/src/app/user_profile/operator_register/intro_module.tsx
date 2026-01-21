"use client";

export function OperatorRegisterIntroModule({
  onNext,
}: {
  onNext: () => void;
}) {
  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 p-6">
        <h2 className="text-3xl font-bold mb-6 text-gray-900">
          Zakładasz konto operatora dronów
        </h2>
        <p className="text-gray-700 mb-6 text-lg leading-relaxed">
          Od chwili zmiany statusu konta, odblokujesz następujące funkcje:
        </p>
        <ul className="space-y-4 mb-8 text-gray-700 text-base">
          <li className="flex items-start gap-3">
            <span className="text-primary-600 font-bold">•</span>
            <span>Wystawianie recenzji pracodawcom</span>
          </li>
          <li className="flex items-start gap-3">
            <span className="text-primary-600 font-bold">•</span>
            <span>Możliwość wprowadzenia danych o własnym portfolio:</span>
          </li>
          <li className="flex items-start gap-3 ml-8">
            <span className="text-primary-400">•</span>
            <span>własne zdjęcia</span>
          </li>
          <li className="flex items-start gap-3 ml-8">
            <span className="text-primary-400">•</span>
            <span>specjalności oraz kwalifikacje</span>
          </li>
          <li className="flex items-start gap-3 ml-8">
            <span className="text-primary-400">•</span>
            <span>opis konta lub swojej działalności</span>
          </li>
        </ul>
        <p className="text-gray-700 text-base bg-gray-50 p-4 rounded-xl border border-gray-200">
          💡 Jako operator, nadal będziesz móc wystawiać oferty pracy.
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
