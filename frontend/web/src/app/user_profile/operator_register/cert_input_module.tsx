import { useState } from "react";
import { checkInput } from "./operator_register_module";
interface CertificatesInputModuleProps {
  onNext: () => void;
  onPrev: () => void;
  data: { certificates: string[]; services: string[] };
  setData: (data: { certificates: string[]; services: string[] }) => void;
}

export function CertificatesInputModule({
  onNext,
  onPrev,
  data,
  setData,
}: CertificatesInputModuleProps) {
  const [inputValue, setInputValue] = useState("");

  const addCertificate = () => {
    if (inputValue.trim()) {
      setData({
        ...data,
        certificates: [...data.certificates, inputValue],
      });
      setInputValue("");
    }
  };

  const removeCertificate = (index: number) => {
    setData({
      ...data,
      certificates: data.certificates.filter((_, i) => i !== index),
    });
  };

  return (
    <div className="flex flex-col h-full">
      <div className="flex-1">
        <h2 className="text-2xl font-bold mb-6">Dodaj certyfikaty</h2>
        <div className="flex gap-2 mb-4">
          <input
            value={inputValue}
            onChange={(e) => setInputValue(checkInput(e.target.value))}
            onKeyPress={(e) => {
              if (e.key === "Enter") {
                addCertificate();
              }
            }}
            placeholder="Nazwa certyfikatu"
            className="flex-1 px-3 py-2 border rounded-lg"
          />
          <button
            onClick={addCertificate}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
          >
            Dodaj
          </button>
        </div>
        <div className="space-y-2">
          {data.certificates.map((c, i) => (
            <div
              key={i}
              className="inline-block bg-gray-200 px-3 py-1 rounded mr-2"
            >
              {c}
              <button
                onClick={() => removeCertificate(i)}
                className="ml-2 font-bold cursor-pointer"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      </div>

      <div className="flex justify-between gap-4 pt-8 border-t">
        <button
          onClick={onPrev}
          className="px-6 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition-all"
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
