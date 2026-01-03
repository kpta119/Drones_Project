import { useState, useEffect } from "react";

interface ServicesInputModuleProps {
  onNext: () => void;
  onPrev: () => void;
  data: { certificates: string[]; services: string[] };
  setData: (data: { certificates: string[]; services: string[] }) => void;
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
          headers: {
            "X-USER-TOKEN": `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        console.log("Fetched services:", data);
        setAvailableServices(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Failed to fetch services:", err);
        setAvailableServices([]);
      } finally {
        setLoading(false);
      }
    };

    fetchServices();
  }, []);

  const toggleService = (service: string) => {
    if (data.services.includes(service)) {
      setData({
        ...data,
        services: data.services.filter((s) => s !== service),
      });
    } else {
      setData({
        ...data,
        services: [...data.services, service],
      });
    }
  };

  return (
    <div className="flex flex-col h-full">
      <div className="flex-1">
        <h2 className="text-2xl font-bold mb-6">Wybierz usługi</h2>

        {loading ? (
          <p className="text-gray-600">Ładowanie usług...</p>
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
                <span className="text-gray-700">{service}</span>
              </label>
            ))}
          </div>
        )}

        {data.services.length > 0 && (
          <div className="mt-6 p-4 bg-blue-50 rounded-lg">
            <p className="text-sm font-semibold text-blue-900 mb-2">
              Wybrane usługi:
            </p>
            <div className="flex flex-wrap gap-2">
              {data.services.map((service) => (
                <span
                  key={service}
                  className="inline-block bg-blue-200 text-blue-900 px-3 py-1 rounded"
                >
                  {service}
                </span>
              ))}
            </div>
          </div>
        )}
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
          className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-all disabled:opacity-50"
          disabled={data.services.length === 0}
        >
          Dalej
        </button>
      </div>
    </div>
  );
}
