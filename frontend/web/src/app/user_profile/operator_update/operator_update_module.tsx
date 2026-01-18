"use client";

import { useEffect, useState } from "react";
import dynamic from "next/dynamic";
import { CertificatesInputModule } from "../operator_register/cert_input_module";
import { ServicesInputModule } from "../operator_register/services_input_module";
import { useRouter } from "next/navigation";

const LocationInputModule = dynamic(
  () =>
    import("../operator_register/location_module").then(
      (mod) => mod.LocationInputModule
    ),
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

interface OperatorDataApiResponse {
  certificates?: string[];
  operator_services?: string[];
  coordinates?: string;
  radius?: number;
}

export const checkInput = (value: string): string => {
  const pattern = /[^a-zA-Z0-9\s\-]/g;
  return value.replace(pattern, "");
};

interface OperatorRegisterModuleProps {
  onClose: () => void;
  userIdFromUrl?: string;
}

const mapApiResponseToRegistrationData = (response: OperatorDataApiResponse): RegistrationData => {
  const coords = response.coordinates ?? "52.237,21.017";
  const [lat, lng] = coords.split(",").map((v: string) => Number(v.trim()));

  return {
    certificates: response.certificates ?? [],
    services: response.operator_services ?? [],
    coordinates: {
      lat: isNaN(lat) ? 52.237 : lat,
      lng: isNaN(lng) ? 21.017 : lng,
    },
    radius: response.radius ?? 5000,
  };
};

export default function OperatorRegisterModule({ onClose, userIdFromUrl }: OperatorRegisterModuleProps) {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<RegistrationData>({
    certificates: [],
    services: [],
    coordinates: { lat: 52.237, lng: 21.017 },
    radius: 5000,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOperatorData = async () => {
      try {
        const token = localStorage.getItem("token");
        if (!token) {
          router.replace("/login");
          return;
        }

        const url = `/operators/getOperatorProfile/${localStorage.getItem("userId")}`;

        const response = await fetch(url, {
          headers: {
            "X-USER-TOKEN": `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        if (!response.ok) throw new Error("Profil operatora nie odnaleziony");

        const operatorData = await response.json();
        setData(mapApiResponseToRegistrationData(operatorData));
      } catch (err) {
        if (err instanceof Error) {
          console.error(err);
          setError(err.message || "Błąd pobierania danych operatora");
        }
      } finally {
        setLoading(false);
      }
    };

    fetchOperatorData();
  }, [router, userIdFromUrl]);

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
      const response = await fetch(`/operators/editOperatorProfile`, {
        method: "PATCH",
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
        setError(errorData.message || `Błąd serwera: ${response.status}`);
      }
    } catch {
      setError("Błąd połączenia");
    } finally {
      setIsSubmitting(false);
    }
  };

  const steps = [
    { number: 0, label: "Certyfikaty" },
    { number: 1, label: "Usługi" },
    { number: 2, label: "Lokalizacja" },
    { number: 3, label: "Podsumowanie" },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full w-full">
        Ładowanie danych operatora...
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:gap-12 h-full p-4 lg:p-8 overflow-y-auto">
      <div className="lg:col-span-2 relative flex flex-col h-full">
        <div className="flex-1 mt-8 lg:mt-0">
          {step === 0 && (
            <CertificatesInputModule
              onNext={() => setStep(1)}
              onPrev={() => setStep(0)}
              data={data}
              setData={setData}
            />
          )}
          {step === 1 && (
            <ServicesInputModule
              onNext={() => setStep(2)}
              onPrev={() => setStep(0)}
              data={data}
              setData={setData}
            />
          )}
          {step === 2 && (
            <LocationInputModule
              onNext={() => setStep(3)}
              onPrev={() => setStep(1)}
              data={data}
              setData={setData}
            />
          )}
          {step === 3 && (
            <div className="flex flex-col items-center justify-center h-full mt-6 gap-4">
              <p className="text-lg font-semibold">Czy zapisać dane?</p>
              {error && <p className="text-red-500">{error}</p>}
              <div className="flex gap-4">
                <button
                  onClick={handleSubmit}
                  disabled={isSubmitting}
                  className="bg-green-500 hover:bg-green-600 text-white px-6 py-2 rounded font-semibold disabled:opacity-50"
                >
                  Zapisz
                </button>
                <button
                  onClick={onClose}
                  className="bg-gray-300 hover:bg-gray-400 text-black px-6 py-2 rounded font-semibold"
                >
                  Anuluj
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
                className={`p-3 lg:p-4 border-2 rounded-lg flex items-center gap-3 transition-all ${step >= s.number
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
