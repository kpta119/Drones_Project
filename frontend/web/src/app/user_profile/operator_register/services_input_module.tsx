import { useState, useEffect } from "react";
import { RegistrationData } from "./operator_register_module";

interface ServicesInputModuleProps {
  onNext: () => void;
  onPrev: () => void;
  data: RegistrationData;
  setData: (data: RegistrationData) => void;
}

export function ServicesInputModule({
  onNext,
  onPrev,
  data,
  setData,
}: ServicesInputModuleProps) {
  const [availableServices, setAvailableServices] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchServices = async () => {
      try {
        const token = localStorage.getItem("token");
        const response = await fetch("/api/services/getServices", {
          headers: { "X-USER-TOKEN": `Bearer ${token}` },
        });
        const resData = await response.json();
        setAvailableServices(Array.isArray(resData) ? resData : []);
      } catch (err) {
        setAvailableServices([]);
      } finally {
        setLoading(false);
      }
    };
    fetchServices();
  }, []);

  const toggleService = (service: string) => {
    const services = data.services.includes(service)
      ? data.services.filter((s) => s !== service)
      : [...data.services, service];
    setData({ ...data, services });
  };

  return (
    <div className="flex flex-col h-full">
      <div className="flex-1">
        <h2 className="text-2xl font-bold mb-6">Wybierz usługi</h2>
        {loading ? (
          <p>Ładowanie...</p>
        ) : (
          <div className="space-y-3">
            {availableServices.map((service) => (
              <label
                key={service}
                className="flex items-center gap-3 cursor-pointer p-3 border rounded-lg hover:bg-gray-50"
              >
                <input
                  type="checkbox"
                  checked={data.services.includes(service)}
                  onChange={() => toggleService(service)}
                  className="w-5 h-5"
                />
                <span>{service}</span>
              </label>
            ))}
          </div>
        )}
      </div>
      <div className="flex justify-between gap-4 pt-8 border-t">
        <button onClick={onPrev} className="px-6 py-2 bg-gray-300 rounded-lg">
          Wróć
        </button>
        <button
          onClick={onNext}
          disabled={data.services.length === 0}
          className="px-6 py-2 bg-primary-600 text-white rounded-lg disabled:opacity-50"
        >
          Dalej
        </button>
      </div>
    </div>
  );
}
