"use client";

import { useState } from "react";
import dynamic from "next/dynamic";
import { CertificatesInputModule } from "./cert_input_module";
import { OperatorRegisterIntroModule } from "./intro_module";
import { ServicesInputModule } from "./services_input_module";

const LocationInputModule = dynamic(
  () => import("./location_module").then((mod) => mod.LocationInputModule),
  {
    ssr: false,
    loading: () => (
      <div className="h-[400px] w-full bg-gray-100 animate-pulse flex items-center justify-center">
        Ładowanie mapy...
      </div>
    ),
  }
);

export interface RegistrationData {
  certificates: string[];
  services: string[];
  coordinates: { lat: number; lng: number };
  radius: number;
}

export const checkInput = (value: string): string => {
  const pattern = /[^a-zA-Z0-9\s\-]/g;
  return value.replace(pattern, "");
};

interface OperatorRegisterModuleProps {
  onClose: () => void;
}

export default function OperatorRegisterModule({
  onClose,
}: OperatorRegisterModuleProps) {
  const [step, setStep] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [data, setData] = useState<RegistrationData>({
    certificates: [],
    services: [],
    coordinates: { lat: 52.237, lng: 21.017 },
    radius: 5000,
  });

  const handleSubmit = async () => {
    setIsSubmitting(true);
    setError(null);

    const token = localStorage.getItem("token");
    if (!token) {
      setError("Błąd autoryzacji");
      setIsSubmitting(false);
      return;
    }

    const payload = {
      certificates: data.certificates,
      services: data.services,
      coordinates: `${data.coordinates.lat},${data.coordinates.lng}`,
      radius: data.radius,
    };

    try {
      const response = await fetch(
        `/operators/createOperatorProfile`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-USER-TOKEN": `Bearer ${token}`,
          },
          body: JSON.stringify(payload),
        }
      );

      if (response.ok) {
        onClose();
        window.location.reload();
      } else {
        const errorData = await response.json().catch(() => ({}));
        setError(errorData.message || `Błąd serwera: ${response.status}`);
      }
    } catch {
      setError("Błąd połączenia");
    } finally {
      setIsSubmitting(false);
    }
  };

  const steps = [
    { number: 0, label: "Wprowadzenie" },
    { number: 1, label: "Certyfikaty" },
    { number: 2, label: "Usługi" },
    { number: 3, label: "Lokalizacja" },
    { number: 4, label: "Podsumowanie" },
  ];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:gap-12 h-full p-4 lg:p-8 overflow-y-auto">
      <div className="lg:col-span-2 relative flex flex-col h-full">
        <div className="flex-1">
          {step === 0 && (
            <OperatorRegisterIntroModule onNext={() => setStep(1)} />
          )}
          {step === 1 && (
            <CertificatesInputModule
              onNext={() => setStep(2)}
              onPrev={() => setStep(0)}
              data={data}
              setData={setData}
            />
          )}
          {step === 2 && (
            <ServicesInputModule
              onNext={() => setStep(3)}
              onPrev={() => setStep(1)}
              data={data}
              setData={setData}
            />
          )}
          {step === 3 && (
            <LocationInputModule
              onNext={() => setStep(4)}
              onPrev={() => setStep(2)}
              data={data}
              setData={setData}
            />
          )}
          {step === 4 && (
            <div className="flex flex-col items-center justify-center h-full text-center p-4">
              <h2 className="text-2xl lg:text-3xl font-bold mb-4 text-black">
                Finalizacja
              </h2>
              <p className="text-gray-600 mb-8">
                Czy wszystkie dane są poprawne?
              </p>

              {error && (
                <p className="text-red-500 mb-6 font-medium">{error}</p>
              )}

              <div className="flex flex-col sm:flex-row gap-4 w-full justify-center">
                <button
                  onClick={() => setStep(3)}
                  disabled={isSubmitting}
                  className="px-8 py-3 bg-gray-200 text-gray-700 rounded-xl font-semibold disabled:opacity-50"
                >
                  Wróć
                </button>
                <button
                  onClick={handleSubmit}
                  disabled={isSubmitting}
                  className="px-8 py-3 bg-primary-600 text-white rounded-xl font-bold disabled:opacity-50 transition-all"
                >
                  {isSubmitting ? "Wysyłanie..." : "Zatwierdź"}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="flex flex-col justify-start lg:justify-center border-t lg:border-t-0 lg:border-l border-gray-100 pt-6 lg:pt-0 lg:pl-8">
        <h4 className="text-xs font-bold uppercase text-gray-400 mb-4 lg:hidden">
          Postęp:
        </h4>
        <ol className="flex flex-row lg:flex-col gap-2 lg:space-y-3 overflow-x-auto pb-4 lg:pb-0">
          {steps.map((s) => (
            <li key={s.number} className="shrink-0 lg:w-full">
              <div
                className={`p-3 lg:p-4 border-2 rounded-lg flex items-center gap-3 transition-all ${
                  step >= s.number
                    ? "bg-green-50 border-green-400 text-green-800"
                    : "bg-white border-gray-300 text-gray-600"
                }`}
              >
                <span className="text-xs lg:text-sm font-semibold whitespace-nowrap">
                  {s.number + 1}. {s.label}
                </span>
                {step > s.number && (
                  <svg
                    className="w-4 h-4 hidden lg:block"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    strokeWidth="3"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M5 13l4 4L19 7"
                    />
                  </svg>
                )}
              </div>
            </li>
          ))}
        </ol>
      </div>
    </div>
  );
}
