export async function getAddressFromCoordinates(
  coordinates: string
): Promise<{ city: string; street: string }> {
  try {
    const [lat, lng] = coordinates.split(",").map((c) => c.trim());
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
        "Nieznane miasto",
      street: data.address.road
        ? `${data.address.road} ${data.address.house_number || ""}`
        : "Brak nazwy ulicy",
    };
  } catch {
    return { city: "Błąd", street: "lokalizacji" };
  }
}
