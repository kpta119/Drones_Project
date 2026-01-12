export async function getAddressFromCoordinates(
  coords: string
): Promise<{ city: string; street: string; country: string }> {
  try {
    const [lat, lng] = coords.split(",").map((c) => c.trim());
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&addressdetails=1`,
      { headers: { "Accept-Language": "pl" } }
    );
    const data = await res.json();

    return {
      city:
        data.address.city ||
        data.address.town ||
        data.address.village ||
        "Nieznane",
      street: data.address.road
        ? `${data.address.road} ${data.address.house_number || ""}`
        : "Brak nazwy ulicy",
      country: data.address.country || "Nieznany kraj",
    };
  } catch {
    return { city: "Błąd", street: "lokalizacji", country: "Nieznany" };
  }
}
