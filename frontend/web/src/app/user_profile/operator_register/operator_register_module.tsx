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
      setError("Error acquiring token");
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
      const response = await fetch("/api/operators/createOperatorProfile", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-USER-TOKEN": `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        onClose();
        window.location.reload();
      } else {
        const errorData = await response.json().catch(() => ({}));
        setError(errorData.message || `Server error: ${response.status}`);
      }
    } catch (err) {
      setError("Connection error");
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
    <div className="grid grid-cols-3 gap-12 h-full p-8">
      <div className="col-span-2 relative">
        <button
          onClick={onClose}
          className="absolute top-0 right-0 text-gray-500 hover:text-gray-700 text-2xl font-bold"
        >
          ✕
        </button>

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
          <div className="flex flex-col items-center justify-center h-full text-center">
            <h2 className="text-3xl font-bold mb-4">Finalizacja</h2>
            <p className="text-gray-600 mb-8">
              Czy wszystkie dane są poprawne?
            </p>

            {error && (
              <div className="mb-6 p-4 bg-red-50 text-red-600 border border-red-200 rounded-lg">
                {error}
              </div>
            )}

            <div className="flex gap-4">
              <button
                onClick={() => setStep(3)}
                disabled={isSubmitting}
                className="px-8 py-3 bg-gray-200 text-gray-700 rounded-xl font-semibold"
              >
                Wróć
              </button>
              <button
                onClick={handleSubmit}
                disabled={isSubmitting}
                className="px-12 py-3 bg-primary-600 text-white rounded-xl font-bold text-lg hover:bg-primary-700 disabled:opacity-50 transition-all"
              >
                {isSubmitting ? "Wysyłanie..." : "Zatwierdź i zarejestruj"}
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-col justify-center">
        <ol className="space-y-3">
          {steps.map((s) => (
            <li key={s.number}>
              <div
                className={`w-full p-4 border-2 rounded-lg flex items-center justify-between transition-all ${
                  step >= s.number
                    ? "bg-green-50 border-green-400 text-green-800"
                    : "bg-white border-gray-300 text-gray-600"
                }`}
              >
                <h3 className="font-semibold text-sm">
                  {s.number + 1}. {s.label}
                </h3>
                {step > s.number && (
                  <svg
                    className="w-5 h-5"
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
