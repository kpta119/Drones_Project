"use client";

import { useState } from "react";

export default function AdminServices() {
  const [services, setServices] = useState<string[]>([""]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleAddServiceField = () => {
    setServices([...services, ""]);
  };

  const handleRemoveServiceField = (index: number) => {
    const newServices = services.filter((_, i) => i !== index);
    setServices(newServices.length > 0 ? newServices : [""]);
  };

  const handleServiceChange = (index: number, value: string) => {
    const newServices = [...services];
    newServices[index] = value;
    setServices(newServices);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    // Filter out empty services
    const validServices = services.filter((s) => s.trim() !== "");

    if (validServices.length === 0) {
      setError("Dodaj przynajmniej jedną usługę");
      return;
    }

    setLoading(true);

    try {
      const token = localStorage.getItem("token");
      if (!token) {
        setError("Brak autoryzacji");
        return;
      }

      const response = await fetch("/services", {
        method: "POST",
        headers: {
          "X-USER-TOKEN": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(validServices),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          errorData.message || `Błąd serwera: ${response.status}`
        );
      }

      const createdServices = await response.json();
      setSuccess(
        `Pomyślnie dodano ${createdServices.length} usług: ${createdServices.join(", ")}`
      );
      setServices([""]);
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error ? err.message : "Nie udało się dodać usług";
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Zarządzanie Usługami</h1>

      <div className="bg-white border border-gray-300 rounded-lg p-6 max-w-2xl">
        <h2 className="text-xl font-semibold mb-4">Dodaj Nowe Usługi</h2>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {error}
          </div>
        )}

        {success && (
          <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">
            {success}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="space-y-3 mb-4">
            {services.map((service, index) => (
              <div key={index} className="flex gap-2">
                <input
                  type="text"
                  value={service}
                  onChange={(e) => handleServiceChange(index, e.target.value)}
                  placeholder="Nazwa usługi (np. Geodezja)"
                  className="flex-1 px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500 outline-none"
                />
                {services.length > 1 && (
                  <button
                    type="button"
                    onClick={() => handleRemoveServiceField(index)}
                    className="px-3 py-2 bg-red-500 text-white rounded hover:bg-red-600 transition-colors"
                  >
                    Usuń
                  </button>
                )}
              </div>
            ))}
          </div>

          <button
            type="button"
            onClick={handleAddServiceField}
            className="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300 transition-colors mb-4"
          >
            + Dodaj kolejną usługę
          </button>

          <div className="flex gap-3">
            <button
              type="submit"
              disabled={loading}
              className="px-6 py-2 bg-blue-600 text-white rounded font-semibold hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? "Dodawanie..." : "Dodaj Usługi"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
