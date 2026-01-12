"use client";

import { useState, useEffect } from "react";
import dynamic from "next/dynamic";

const OrderLocationPicker = dynamic(() => import("../utils/order_location"), {
  ssr: false,
  loading: () => (
    <div className="h-[450px] w-full bg-gray-50 animate-pulse rounded-[2rem] flex items-center justify-center text-black">
      Inicjalizacja mapy...
    </div>
  ),
});

interface CreateOrderViewProps {
  onCancel: () => void;
  onSuccess: () => void;
}

export default function CreateOrderView({
  onCancel,
  onSuccess,
}: CreateOrderViewProps) {
  const [services, setServices] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [step, setStep] = useState(1);

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    service: "",
    coordinates: { lat: 52.237, lng: 21.017 },
    fromDate: "",
    toDate: "",
    parameters: {},
  });

  useEffect(() => {
    const fetchServices = async () => {
      const token = localStorage.getItem("token");
      try {
        const res = await fetch("/api/services/getServices", {
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        });
        if (res.ok) setServices(await res.json());
      } catch {
        setServices([]);
      }
    };
    fetchServices();
  }, []);
  const handleSubmit = async () => {
    setLoading(true);
    setError(null);
    const token = localStorage.getItem("token");

    // Precyzyjny format dla Java LocalDateTime: YYYY-MM-DDTHH:mm:ss
    const formatForJava = (dateStr: string) => {
      if (!dateStr) return null;
      // datetime-local zwraca "YYYY-MM-DDTHH:mm" -> dodajemy ":00"
      return dateStr.includes(":") && dateStr.split(":").length === 2
        ? `${dateStr}:00`
        : dateStr;
    };

    // Zmieniamy klucze na snake_case, ponieważ Twój backend (sądząc po mapperze)
    // prawdopodobnie tak ma skonfigurowany Jackson Naming Strategy
    const payload = {
      title: formData.title,
      description: formData.description,
      service: formData.service,
      coordinates: `${formData.coordinates.lat},${formData.coordinates.lng}`,
      from_date: formatForJava(formData.fromDate), // klucz: from_date
      to_date: formatForJava(formData.toDate), // klucz: to_date
      parameters: formData.parameters || {},
    };

    console.log("Wysyłany Payload (do sprawdzenia):", payload);

    try {
      const res = await fetch("/api/orders/createOrder", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-USER-TOKEN": `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      if (res.ok) {
        onSuccess();
      } else {
        const responseText = await res.text();
        console.error("Surowy błąd z serwera:", responseText);

        // Próba wyciągnięcia komunikatu o błędzie
        let errorMessage = `Błąd serwera (${res.status})`;
        try {
          const json = JSON.parse(responseText);
          // Jeśli backend zwraca listę błędów walidacji, wyciągnij pierwszy
          errorMessage = json.message || json.error || errorMessage;
        } catch {
          errorMessage = responseText || errorMessage;
        }

        setError(errorMessage);
      }
    } catch (err) {
      console.error("Błąd sieci:", err);
      setError("Błąd połączenia z serwerem.");
    } finally {
      setLoading(false);
    }
  };
  return (
    <div className="w-full max-w-4xl bg-white border-2 border-gray-100 rounded-[3rem] p-8 lg:p-12 shadow-xl animate-fadeIn text-black font-montserrat">
      <div className="flex justify-between items-center mb-10">
        <h2 className="text-3xl font-bold">Wystaw ofertę</h2>
        <div className="flex gap-2">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className={`h-2 w-8 rounded-full transition-all ${
                step === i ? "bg-primary-500" : "bg-gray-200"
              }`}
            />
          ))}
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl text-sm font-bold animate-shake">
          ⚠️ {error}
        </div>
      )}

      {step === 1 && (
        <div className="space-y-6 animate-fadeIn">
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider">
              Tytuł ogłoszenia
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) =>
                setFormData({ ...formData, title: e.target.value })
              }
              placeholder="np. Zdjęcia z drona - budowa domu"
              className="w-full px-6 py-4 bg-gray-50 rounded-2xl focus:ring-2 focus:ring-primary-500 outline-none font-medium"
            />
          </div>
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider">
              Rodzaj usługi
            </label>
            <select
              value={formData.service}
              onChange={(e) =>
                setFormData({ ...formData, service: e.target.value })
              }
              className="w-full px-6 py-4 bg-gray-50 rounded-2xl focus:ring-2 focus:ring-primary-500 outline-none appearance-none font-medium"
            >
              <option value="">Wybierz usługę...</option>
              {services.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider">
              Szczegółowy opis
            </label>
            <textarea
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
              rows={4}
              placeholder="Opisz dokładnie zakres prac..."
              className="w-full px-6 py-4 bg-gray-50 rounded-2xl focus:ring-2 focus:ring-primary-500 outline-none font-medium"
            />
          </div>
          <div className="flex justify-end pt-4">
            <button
              onClick={() => setStep(2)}
              disabled={!formData.title || !formData.service}
              className="px-12 py-4 bg-primary-600 text-white rounded-2xl font-bold shadow-lg hover:bg-primary-700 disabled:opacity-50 transition-all"
            >
              Dalej
            </button>
          </div>
        </div>
      )}

      {step === 2 && (
        <OrderLocationPicker
          coordinates={formData.coordinates}
          setCoordinates={(coords) =>
            setFormData({ ...formData, coordinates: coords })
          }
          onNext={() => setStep(3)}
          onPrev={() => setStep(1)}
        />
      )}

      {step === 3 && (
        <div className="space-y-8 animate-fadeIn">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider">
                Zlecenie od:
              </label>
              <input
                type="datetime-local"
                value={formData.fromDate}
                onChange={(e) =>
                  setFormData({ ...formData, fromDate: e.target.value })
                }
                className="w-full px-6 py-4 bg-gray-50 rounded-2xl font-medium border-2 border-transparent focus:border-primary-500 outline-none"
              />
            </div>
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider">
                Zlecenie do:
              </label>
              <input
                type="datetime-local"
                value={formData.toDate}
                onChange={(e) =>
                  setFormData({ ...formData, toDate: e.target.value })
                }
                className="w-full px-6 py-4 bg-gray-50 rounded-2xl font-medium border-2 border-transparent focus:border-primary-500 outline-none"
              />
            </div>
          </div>

          <div className="flex justify-between items-center pt-10">
            <button
              onClick={() => setStep(2)}
              className="text-gray-400 font-bold hover:text-black transition-colors"
            >
              Wróć
            </button>
            <button
              onClick={handleSubmit}
              disabled={loading || !formData.fromDate || !formData.toDate}
              className="px-16 py-4 bg-primary-600 text-white rounded-2xl font-bold shadow-xl hover:bg-primary-700 disabled:opacity-50 transition-all"
            >
              {loading ? "Przetwarzanie..." : "Opublikuj ogłoszenie"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
