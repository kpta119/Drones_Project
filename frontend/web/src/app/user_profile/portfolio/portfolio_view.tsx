"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import Image from "next/image";

export interface PortfolioPhoto {
    url: string;
    name?: string;
}

export interface PortfolioData {
    title: string;
    description: string;
    photos: PortfolioPhoto[];
}

const Portfolio = () => {
    const searchParams = useSearchParams();
    const displayedUserId = searchParams.get("user_id");

    const [portfolioData, setPortfolioData] = useState<PortfolioData | null>(null);
    const [currentPhotoIndex, setCurrentPhotoIndex] = useState(0);

    useEffect(() => {
        const fetchPortfolio = async () => {
            try {
                const token = localStorage.getItem("token");
                // Użyj user_id z URL, a jeśli nie ma to użyj własnego userId
                const operatorId = displayedUserId || localStorage.getItem("userId");
                if (!token || !operatorId) return;

                const res = await fetch(`/operators/getOperatorProfile/${operatorId}`, {
                    headers: { "X-USER-TOKEN": `Bearer ${token}` },
                });

                if (!res.ok) throw new Error("Błąd pobierania danych");

                const data = await res.json();
                setPortfolioData(data.portfolio || { title: "", description: "", photos: [] });
            } catch (error) {
                console.error(error);
            }
        };

        fetchPortfolio();
    }, [displayedUserId]);

    if (!portfolioData) return <p></p>;

    const { photos, title, description } = portfolioData;
    const hasPhotos = photos && photos.length > 0;

    const prevPhoto = () => {
        if (!hasPhotos) return;
        setCurrentPhotoIndex((prev) => (prev === 0 ? photos.length - 1 : prev - 1));
    };

    const nextPhoto = () => {
        if (!hasPhotos) return;
        setCurrentPhotoIndex((prev) => (prev === photos.length - 1 ? 0 : prev + 1));
    };

    return (
        <div className="flex flex-col w-full gap-6 p-8">
            {/* Zdjęcie - poziomy layout dla poziomych zdjęć */}
            <div
                className="relative w-full rounded-2xl overflow-hidden shadow-lg"
                style={{ height: "500px" }}
            >
                {hasPhotos ? (
                    <Image
                        src={photos[currentPhotoIndex].url}
                        alt={photos[currentPhotoIndex].name || "Portfolio photo"}
                        fill
                        unoptimized
                        style={{
                            objectFit: "cover",
                            objectPosition: "center",
                        }}
                    />
                ) : (
                    <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 text-gray-500 text-lg font-medium">
                        Brak zdjęć w portfolio
                    </div>
                )}

                {hasPhotos && (
                    <>
                        <button
                            onClick={prevPhoto}
                            className="absolute top-1/2 left-3 -translate-y-1/2 bg-white/90 hover:bg-white text-gray-800 w-10 h-10 rounded-full shadow-lg transition-all hover:scale-110 flex items-center justify-center text-2xl font-bold"
                        >
                            ‹
                        </button>
                        <button
                            onClick={nextPhoto}
                            className="absolute top-1/2 right-3 -translate-y-1/2 bg-white/90 hover:bg-white text-gray-800 w-10 h-10 rounded-full shadow-lg transition-all hover:scale-110 flex items-center justify-center text-2xl font-bold"
                        >
                            ›
                        </button>
                        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-2">
                            {photos.map((_: PortfolioPhoto, index: number) => (
                                <div
                                    key={index}
                                    onClick={() => setCurrentPhotoIndex(index)}
                                    className={`w-2.5 h-2.5 rounded-full cursor-pointer transition-all shadow ${
                                        index === currentPhotoIndex ? "bg-white scale-125" : "bg-white/50"
                                    }`}
                                />
                            ))}
                        </div>
                    </>
                )}
            </div>

            {/* Opis pod zdjęciem */}
            <div className="flex flex-col bg-gray-50 rounded-2xl p-6 shadow-inner max-h-80 overflow-y-auto">
                <h2 className="text-2xl font-bold mb-4 text-gray-900 break-words leading-tight">
                    {title || "Brak tytułu"}
                </h2>
                <p className="text-base leading-relaxed text-gray-700 break-words whitespace-pre-wrap text-justify">
                    {description || "Brak opisu"}
                </p>
            </div>

        </div>
    );
};

export default Portfolio;
