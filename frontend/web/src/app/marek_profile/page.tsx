"use client";
import PageHeader from "../../components/page_header";
import Image from "next/image";

export default function ProfilePage() {
  return (
    <>
      <PageHeader />
      <div className="min-h-screen flex justify-center">
        <div className="w-full max-w-5xl bg-white shadow-lg rounded-lg p-6">
          {/* Header */}
          <div className="flex items-center space-x-6 mb-6">
            <div className="w-24 h-24 relative">
              <Image
                src="/dron.png"
                alt="Profile Avatar"
                fill
                className="rounded-full object-cover"
              />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Marek Droński</h1>
              <p className="text-gray-500">marekdrony@proton.me</p>
            </div>
          </div>
          <div className="space-y-4">
            <div className="bg-gray-50 p-4 rounded">
              <h2 className="font-semibold text-lg mb-1">O Mnie</h2>
              <p className="text-gray-600">
                Jestem certyfikowanym operatorem dronów specjalizującym się w
                wykonywaniu lotów zarówno rekreacyjnych, jak i komercyjnych. Na
                co dzień dbam o bezpieczeństwo operacji, zgodność z
                obowiązującymi przepisami oraz wysoką jakość realizowanych ujęć
                i danych.
              </p>
              <p>
                Posiadam doświadczenie w obsłudze różnych platform
                bezzałogowych, w tym modeli przeznaczonych do fotografii
                lotniczej, inspekcji technicznych oraz mapowania terenu. Zwracam
                szczególną uwagę na przygotowanie misji – od analizy przestrzeni
                powietrznej, przez planowanie trasy, aż po kontrolę warunków
                atmosferycznych.
              </p>
              <p>
                W swojej pracy koncentruję się na precyzji, odpowiedzialności i
                zachowaniu najwyższych standardów bezpieczeństwa. Stale
                poszerzam wiedzę techniczną oraz umiejętności praktyczne, aby
                efektywnie wykorzystywać drony w różnych zastosowaniach –
                zarówno komercyjnych, jak i specjalistycznych.
              </p>
            </div>
          </div>
          <div className="mt-6 grid grid-cols-3 gap-4">
            <div className="w-full h-40 relative">
              <Image
                src="/dron.png"
                alt="Image 1"
                fill
                className="object-cover rounded-lg"
              />
            </div>
            <div className="w-full h-40 relative">
              <Image
                src="/dron.png"
                alt="Image 2"
                fill
                className="object-cover rounded-lg"
              />
            </div>
            <div className="w-full h-40 relative">
              <Image
                src="/dron.png"
                alt="Image 3"
                fill
                className="object-cover rounded-lg"
              />
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
