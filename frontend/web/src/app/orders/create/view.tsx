"use client";

import { useState, useEffect } from "react";
import dynamic from "next/dynamic";
import { OrderResponse } from "../types";
import SpecificationsEditor from "../utils/specifications_editor";

const OrderLocationPicker = dynamic(() => import("../utils/order_location"), {
  ssr: false,
  loading: () => (
    <div className="h-[450px] w-full bg-primary-50 animate-pulse rounded-2rem flex items-center justify-center text-primary-900 font-bold">
      Ładowanie mapy...
    </div>
  ),
});

interface CreateOrderViewProps {
  onCancel: () => void;
  onSuccess: () => void;
  editData: OrderResponse | null;
}

export default function CreateOrderView({
  onCancel,
  onSuccess,
  editData,
}: CreateOrderViewProps) {
  const [services, setServices] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [step, setStep] = useState(1);

  const [formData, setFormData] = useState({
    title: editData?.title || "",
    description: editData?.description || "",
    service: editData?.service || "",
    coordinates: editData?.coordinates
      ? {
          lat: parseFloat(editData.coordinates.split(",")[0]),
          lng: parseFloat(editData.coordinates.split(",")[1]),
        }
      : { lat: 52.237, lng: 21.017 },
    fromDate: editData?.from_date?.split(":").slice(0, 2).join(":") || "",
    toDate: editData?.to_date?.split(":").slice(0, 2).join(":") || "",
    parameters: editData?.parameters || {},
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

    const formatForJava = (dateStr: string) => {
      if (!dateStr) return null;
      return dateStr.length === 16 ? `${dateStr}:00` : dateStr;
    };

    const payload = {
      title: formData.title,
      description: formData.description,
      service: formData.service,
      coordinates: `${formData.coordinates.lat},${formData.coordinates.lng}`,
      from_date: formatForJava(formData.fromDate),
      to_date: formatForJava(formData.toDate),
      parameters: formData.parameters || {},
    };

    const url = editData
      ? `/api/orders/editOrder/${editData.id}`
      : "/api/orders/createOrder";

    const method = editData ? "PATCH" : "POST";

    try {
      const res = await fetch(url, {
        method: method,
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
        let message = "Błąd zapisu";
        try {
          const json = JSON.parse(responseText);
          message = json.message || message;
        } catch {
          message = responseText || message;
        }
        setError(message);
      }
    } catch {
      setError("Błąd połączenia z serwerem.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-4xl max-h-[90vh] bg-white border-2 border-primary-100 rounded-[3rem] p-8 lg:p-12 shadow-2xl animate-fadeIn text-black font-montserrat flex flex-col overflow-hidden">
      <div className="flex justify-between items-center mb-10">
        <h2 className="text-3xl font-bold text-primary-900 uppercase tracking-tight">
          {editData ? "Edytuj ofertę" : "Wystaw ofertę"}
        </h2>
        <div className="flex gap-3">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className={`h-2.5 w-10 rounded-full transition-all duration-500 ${
                step === i ? "bg-primary-500 shadow-md" : "bg-primary-100"
              }`}
            />
          ))}
        </div>
      </div>

      {error && (
        <div className="mb-8 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 rounded-r-2xl text-sm font-bold shadow-sm">
          ⚠️ {error}
        </div>
      )}

      <div className="flex-1 overflow-y-auto pr-4">
        {step === 1 && (
          <div className="space-y-6 animate-fadeIn">
            <div>
              <label className="block text-xs font-bold text-primary-800 mb-2 uppercase tracking-widest">
                Tytuł ogłoszenia
              </label>
              <input
                type="text"
                value={formData.title}
                onChange={(e) =>
                  setFormData({ ...formData, title: e.target.value })
                }
                className="w-full px-6 py-4 bg-gray-50 border-2 border-transparent focus:border-primary-300 focus:bg-white rounded-2xl outline-none font-bold transition-all text-black"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-primary-800 mb-2 uppercase tracking-widest">
                Rodzaj usługi
              </label>
              <select
                value={formData.service}
                onChange={(e) =>
                  setFormData({ ...formData, service: e.target.value })
                }
                className="w-full px-6 py-4 bg-gray-50 border-2 border-transparent focus:border-primary-300 focus:bg-white rounded-2xl outline-none appearance-none font-bold transition-all text-black"
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
              <label className="block text-xs font-bold text-primary-800 mb-2 uppercase tracking-widest">
                Szczegółowe wymagania
              </label>
              <textarea
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                rows={4}
                className="w-full px-6 py-4 bg-gray-50 border-2 border-transparent focus:border-primary-300 focus:bg-white rounded-2xl outline-none font-medium transition-all text-black"
              />
            </div>
            <div className="flex justify-end pt-4">
              <button
                onClick={() => setStep(2)}
                disabled={!formData.title || !formData.service}
                className="px-8 lg:px-16 py-3 lg:py-4 bg-primary-300 text-primary-900 rounded-2xl font-bold shadow-lg hover:bg-primary-400 disabled:opacity-30 transition-all uppercase tracking-widest text-xs lg:text-sm"
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
          <div className="space-y-8 animate-fadeIn text-black">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div>
                <label className="block text-xs font-bold text-primary-800 mb-2 uppercase tracking-widest">
                  Zlecenie od:
                </label>
                <input
                  type="datetime-local"
                  value={formData.fromDate}
                  onChange={(e) =>
                    setFormData({ ...formData, fromDate: e.target.value })
                  }
                  className="w-full px-6 py-4 bg-gray-50 rounded-2xl font-bold border-2 border-transparent focus:border-primary-300 outline-none text-black"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-primary-800 mb-2 uppercase tracking-widest">
                  Zlecenie do:
                </label>
                <input
                  type="datetime-local"
                  value={formData.toDate}
                  onChange={(e) =>
                    setFormData({ ...formData, toDate: e.target.value })
                  }
                  className="w-full px-6 py-4 bg-gray-50 rounded-2xl font-bold border-2 border-transparent focus:border-primary-300 outline-none text-black"
                />
              </div>
            </div>

            <SpecificationsEditor
              specifications={formData.parameters}
              onUpdate={(specs) =>
                setFormData({ ...formData, parameters: specs })
              }
            />

            <div className="flex justify-between items-center pt-10">
              <button
                onClick={() => setStep(2)}
                className="text-primary-800 font-bold uppercase tracking-widest text-xs hover:text-black transition-colors"
              >
                Wróć
              </button>
              <button
                onClick={handleSubmit}
                disabled={loading || !formData.fromDate || !formData.toDate}
                className="px-3 lg:px-20 py-3 lg:py-5 bg-primary-300 text-primary-900 rounded-2xl font-bold shadow-xl hover:bg-primary-400 disabled:opacity-30 transition-all uppercase tracking-widest text-xs lg:text-base"
              >
                {loading
                  ? "Wysyłanie..."
                  : editData
                  ? "Zapisz zmiany"
                  : "Opublikuj ogłoszenie"}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
