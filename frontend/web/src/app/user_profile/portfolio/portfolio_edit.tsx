"use client";

import { useEffect, useState, useRef } from "react";
import { FaTimes } from "react-icons/fa";

interface Photo {
    id: number;
    name: string;
    url: string;
}

export default function EditPortfolio({ onClose }: { onClose: () => void }) {
    const [portfolioData, setPortfolioData] = useState({
        title: "",
        description: "",
        photos: [] as Photo[],
    });
    const [newPhotos, setNewPhotos] = useState<File[]>([]);
    const [newPhotoNames, setNewPhotoNames] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState("");
    const [isNew, setIsNew] = useState(false);
    const [photosToDelete, setPhotosToDelete] = useState<number[]>([]);
    const fileInputRef = useRef<HTMLInputElement | null>(null);

    useEffect(() => {
        const fetchPortfolio = async () => {
            try {
                const token = localStorage.getItem("token");
                const operatorId = localStorage.getItem("userId");
                if (!token || !operatorId) return;

                const res = await fetch(`/operators/getOperatorProfile/${operatorId}`, {
                    headers: { "X-USER-TOKEN": `Bearer ${token}` },
                });

                if (!res.ok) throw new Error("Błąd pobierania danych");
                const data = await res.json();

                if (!data.portfolio) {
                    setPortfolioData({ title: "", description: "", photos: [] });
                    setIsNew(true);
                } else {
                    setPortfolioData({
                        title: data.portfolio.title || "",
                        description: data.portfolio.description || "",
                        photos: data.portfolio.photos || [],
                    });
                    setIsNew(false);
                }
            } catch {
                setPortfolioData({ title: "", description: "", photos: [] });
                setIsNew(true);
            } finally {
                setLoading(false);
            }
        };
        fetchPortfolio();
    }, []);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setPortfolioData((prev) => ({ ...prev, [name]: value }));
    };

    const handleRemovePhoto = (id: number) => {
        setPortfolioData((prev) => ({
            ...prev,
            photos: prev.photos.filter((p) => p.id !== id),
        }));
        setPhotosToDelete((prev) => [...prev, id]);
    };

    const handleNewPhotos = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (!e.target.files) return;
        const files = Array.from(e.target.files);
        setNewPhotos((prev) => [...prev, ...files]);
        const names = files.map((f) => f.name);
        setNewPhotoNames((prev) => [...prev, ...names]);
        if (fileInputRef.current) fileInputRef.current.value = "";
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        setMessage("");

        try {
            const token = localStorage.getItem("token");
            if (!token) throw new Error("Brak tokenu");

            let portfolioIdExists = !isNew;

            if (isNew) {
                const res = await fetch("/operators/addPortfolio", {
                    method: "POST",
                    headers: { "X-USER-TOKEN": `Bearer ${token}`, "Content-Type": "application/json" },
                    body: JSON.stringify({ title: portfolioData.title, description: portfolioData.description }),
                });
                if (!res.ok) throw new Error("Błąd tworzenia portfolio");
                const created = await res.json();
                setPortfolioData((prev) => ({
                    ...prev,
                    title: created.title,
                    description: created.description,
                    photos: created.photos || [],
                }));
                setIsNew(false);
                portfolioIdExists = true;
            } else {
                const res = await fetch("/operators/editPortfolio", {
                    method: "PATCH",
                    headers: { "X-USER-TOKEN": `Bearer ${token}`, "Content-Type": "application/json" },
                    body: JSON.stringify({ title: portfolioData.title, description: portfolioData.description }),
                });
                if (!res.ok) throw new Error("Błąd aktualizacji portfolio");
                const updated = await res.json();
                setPortfolioData((prev) => ({ ...prev, title: updated.title, description: updated.description }));
            }

            if (photosToDelete.length > 0) {
                const res = await fetch("/photos/deletePhotos", {
                    method: "DELETE",
                    headers: { "X-USER-TOKEN": `Bearer ${token}`, "Content-Type": "application/json" },
                    body: JSON.stringify(photosToDelete),
                });
                if (!res.ok) throw new Error("Błąd usuwania zdjęć");
                setPhotosToDelete([]);
            }

            if (newPhotos.length > 0 && portfolioIdExists) {
                const formData = new FormData();
                newPhotos.forEach((file) => formData.append("images", file));
                formData.append("names", JSON.stringify(newPhotoNames));

                const res = await fetch("/photos/addPortfolioPhotos", {
                    method: "POST",
                    headers: { "X-USER-TOKEN": `Bearer ${token}` },
                    body: formData,
                });
                if (!res.ok) throw new Error("Błąd dodawania zdjęć");
                const data = await res.json();
                setPortfolioData((prev) => ({ ...prev, photos: [...prev.photos, ...data.photos] }));
                setNewPhotos([]);
                setNewPhotoNames([]);
            }

            onClose();
            window.location.reload();
        } catch {
            setMessage("Wystąpił błąd podczas zapisywania.");
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm animate-fadeIn">
                <div className="bg-white w-full max-w-3xl rounded-[3rem] shadow-2xl p-8 text-center">
                    <p className="text-lg font-bold text-gray-600 animate-pulse">Ładowanie portfolio...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm animate-fadeIn">
            <div className="bg-white w-full max-w-3xl max-h-[90vh] rounded-[3rem] shadow-2xl overflow-hidden relative border border-white/20">
                <button
                    onClick={onClose}
                    className="absolute top-6 right-6 text-gray-400 hover:text-black transition-colors z-10"
                >
                    <FaTimes size={24} />
                </button>

                <form
                    onSubmit={handleSubmit}
                    className="p-8 overflow-y-auto max-h-[90vh] flex flex-col gap-6"
                >
                    <h2 className="text-3xl font-bold text-gray-900 tracking-tight">
                        {isNew ? "Dodaj nowe portfolio" : "Edytuj portfolio"}
                    </h2>

                    {message && (
                        <div className="p-4 bg-green-50 border-l-4 border-green-500 text-green-700 rounded-r-2xl font-bold">
                            {message}
                        </div>
                    )}

                    <label className="flex flex-col gap-2">
                        Tytuł:
                        <input
                            type="text"
                            name="title"
                            value={portfolioData.title}
                            onChange={handleChange}
                            className="w-full p-3 border border-gray-200 rounded-2xl focus:outline-none focus:ring-2 focus:ring-primary-400"
                            required
                        />
                    </label>

                    <label className="flex flex-col gap-2">
                        Opis:
                        <textarea
                            name="description"
                            value={portfolioData.description}
                            onChange={handleChange}
                            className="w-full p-3 border border-gray-200 rounded-2xl focus:outline-none focus:ring-2 focus:ring-primary-400 min-h-[120px]"
                            required
                        />
                    </label>

                    <div>
                        <p className="font-bold mb-2">Obecne zdjęcia:</p>
                        <div className="flex flex-wrap gap-4">
                            {portfolioData.photos.map((photo) => (
                                <div key={photo.id} className="relative">
                                    <img
                                        src={photo.url}
                                        alt={photo.name}
                                        className="w-28 h-20 object-cover rounded-2xl border border-gray-200"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => handleRemovePhoto(photo.id)}
                                        className="absolute -top-2 -right-2 bg-red-500 text-white w-6 h-6 rounded-full flex items-center justify-center text-sm"
                                    >
                                        ×
                                    </button>
                                </div>
                            ))}
                            {newPhotos.map((file, index) => (
                                <div key={index} className="relative">
                                    <img
                                        src={URL.createObjectURL(file)}
                                        alt={file.name}
                                        className="w-28 h-20 object-cover rounded-2xl border border-gray-200"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setNewPhotos((prev) => prev.filter((_, i) => i !== index));
                                            setNewPhotoNames((prev) => prev.filter((_, i) => i !== index));
                                        }}
                                        className="absolute -top-2 -right-2 bg-red-500 text-white w-6 h-6 rounded-full flex items-center justify-center text-sm"
                                    >
                                        ×
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>

                    <label className="flex flex-col gap-2 mt-4">
                        Dodaj nowe zdjęcia:
                        <input
                            type="file"
                            multiple
                            onChange={handleNewPhotos}
                            className="border border-gray-200 rounded-2xl p-2"
                            ref={fileInputRef}
                        />
                    </label>

                    <button
                        type="submit"
                        disabled={saving}
                        className={`px-6 py-3 bg-primary-400 text-white rounded-2xl font-bold hover:bg-primary-500 transition-all ${saving ? "opacity-50 cursor-not-allowed" : ""}`}
                    >
                        {saving ? "Zapisuję..." : isNew ? "Dodaj portfolio" : "Zapisz zmiany"}
                    </button>
                </form>
            </div>
        </div>
    );
}
