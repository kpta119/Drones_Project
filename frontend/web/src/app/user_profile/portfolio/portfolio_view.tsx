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

    const photoSize = "50vh";

    return (
        <div style={{ display: "flex", height: "55vh", width: "100%", gap: "2rem", alignItems: "flex-start" }}>
            <div
                style={{
                    position: "relative",
                    width: "30vw",
                    height: photoSize,
                    flexShrink: 0,
                    borderRadius: "12px",
                    overflow: "hidden",
                    boxShadow: "0 4px 20px rgba(0, 0, 0, 0.15)",
                }}
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
                    <div
                        style={{
                            width: "100%",
                            height: "100%",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            backgroundColor: "linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%)",
                            background: "linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%)",
                            color: "#888",
                            fontSize: "1.1rem",
                            fontWeight: 500,
                        }}
                    >
                        Brak zdjęć w portfolio
                    </div>
                )}

                {hasPhotos && (
                    <>
                        <button
                            onClick={prevPhoto}
                            style={{
                                position: "absolute",
                                top: "50%",
                                left: "12px",
                                transform: "translateY(-50%)",
                                background: "rgba(255, 255, 255, 0.9)",
                                color: "#333",
                                border: "none",
                                width: "40px",
                                height: "40px",
                                borderRadius: "50%",
                                cursor: "pointer",
                                fontSize: "1.4rem",
                                fontWeight: "bold",
                                boxShadow: "0 2px 8px rgba(0, 0, 0, 0.2)",
                                transition: "all 0.2s ease",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                            }}
                            onMouseEnter={(e) => (e.currentTarget.style.transform = "translateY(-50%) scale(1.1)")}
                            onMouseLeave={(e) => (e.currentTarget.style.transform = "translateY(-50%)")}
                        >
                            ‹
                        </button>
                        <button
                            onClick={nextPhoto}
                            style={{
                                position: "absolute",
                                top: "50%",
                                right: "12px",
                                transform: "translateY(-50%)",
                                background: "rgba(255, 255, 255, 0.9)",
                                color: "#333",
                                border: "none",
                                width: "40px",
                                height: "40px",
                                borderRadius: "50%",
                                cursor: "pointer",
                                fontSize: "1.4rem",
                                fontWeight: "bold",
                                boxShadow: "0 2px 8px rgba(0, 0, 0, 0.2)",
                                transition: "all 0.2s ease",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                            }}
                            onMouseEnter={(e) => (e.currentTarget.style.transform = "translateY(-50%) scale(1.1)")}
                            onMouseLeave={(e) => (e.currentTarget.style.transform = "translateY(-50%)")}
                        >
                            ›
                        </button>
                        <div
                            style={{
                                position: "absolute",
                                bottom: "12px",
                                left: "50%",
                                transform: "translateX(-50%)",
                                display: "flex",
                                gap: "8px",
                            }}
                        >
                            {photos.map((_: PortfolioPhoto, index: number) => (
                                <div
                                    key={index}
                                    onClick={() => setCurrentPhotoIndex(index)}
                                    style={{
                                        width: "10px",
                                        height: "10px",
                                        borderRadius: "50%",
                                        backgroundColor: index === currentPhotoIndex ? "#fff" : "rgba(255,255,255,0.5)",
                                        cursor: "pointer",
                                        transition: "all 0.2s ease",
                                        boxShadow: "0 1px 3px rgba(0,0,0,0.3)",
                                    }}
                                />
                            ))}
                        </div>
                    </>
                )}
            </div>

            <div
                style={{
                    flex: 1,
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "flex-start",
                    alignItems: "flex-start",
                    padding: "1.5rem",
                    backgroundColor: "#fafafa",
                    borderRadius: "12px",
                    boxShadow: "0 2px 8px rgba(0, 0, 0, 0.06)",
                    maxHeight: "50vh",
                    overflowY: "auto",
                }}
            >
                <h2
                    style={{
                        fontSize: "1.75rem",
                        fontWeight: 600,
                        marginBottom: "1rem",
                        color: "#1a1a1a",
                        wordBreak: "break-word",
                        lineHeight: 1.3,
                    }}
                >
                    {title || "Brak tytułu"}
                </h2>
                <p
                    style={{
                        fontSize: "1.05rem",
                        lineHeight: 1.7,
                        color: "#4a4a4a",
                        wordBreak: "break-word",
                        whiteSpace: "pre-wrap",
                        textAlign: "justify",
                    }}
                >
                    {description || "Brak opisu"}
                </p>
            </div>

        </div>
    );
};

export default Portfolio;
