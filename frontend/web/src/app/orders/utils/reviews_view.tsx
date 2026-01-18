"use client";

import { useEffect, useState } from "react";

const Portfolio = () => {
  const [portfolioData, setPortfolioData] = useState<any>(null);
  const [currentPhotoIndex, setCurrentPhotoIndex] = useState(0);

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
        setPortfolioData(data.portfolio || { title: "", description: "", photos: [] });
      } catch (error) {
        console.error(error);
      }
    };

    fetchPortfolio();
  }, []);

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
    <div className="flex justify-center mt-8 mb-8 px-4">
      <div className="bg-white w-full max-w-5xl rounded-3xl shadow-xl flex gap-6 p-8">
        <div style={{ width: photoSize, height: photoSize, position: "relative", flexShrink: 0 }}>
          {hasPhotos ? (
            <img
              src={photos[currentPhotoIndex].url}
              alt={photos[currentPhotoIndex].name || "Portfolio photo"}
              style={{
                width: "100%",
                height: "100%",
                objectFit: "cover",
                objectPosition: "center",
                borderRadius: "1rem",
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
                backgroundColor: "#f0f0f0",
                borderRadius: "1rem",
                color: "#999",
                fontSize: "1.2rem",
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
                  left: "10px",
                  transform: "translateY(-50%)",
                  background: "rgba(0,0,0,0.5)",
                  color: "#fff",
                  border: "none",
                  padding: "0.5rem",
                  borderRadius: "50%",
                  cursor: "pointer",
                }}
              >
                ‹
              </button>
              <button
                onClick={nextPhoto}
                style={{
                  position: "absolute",
                  top: "50%",
                  right: "10px",
                  transform: "translateY(-50%)",
                  background: "rgba(0,0,0,0.5)",
                  color: "#fff",
                  border: "none",
                  padding: "0.5rem",
                  borderRadius: "50%",
                  cursor: "pointer",
                }}
              >
                ›
              </button>
            </>
          )}
        </div>

        <div className="flex-1 flex flex-col justify-start">
          <h2 className="text-3xl font-bold mb-4 break-words">{title || "Brak tytułu"}</h2>
          <p className="text-gray-700 text-base leading-relaxed break-words">{description || "Brak opisu"}</p>
        </div>
      </div>
    </div>
  );
};

export default Portfolio;
