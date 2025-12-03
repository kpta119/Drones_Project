
"use client";

import Image from "next/image";

export default function ProfilePage() {
    return (
        <div className="min-h-screen flex justify-center">
            <div className="w-full max-w-5xl bg-white shadow-lg rounded-lg p-6">
                {/* Header */}
                <div className="flex items-center space-x-6 mb-6">
                    <div className="w-24 h-24 relative">
                        <Image
                            src="/sara.png"
                            alt="Profile Avatar"
                            fill
                            className="rounded-full object-cover"
                        />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold">Sara Jeziorska</h1>
                        <p className="text-gray-500">sjeziorska@gmail.com</p>
                    </div>
                </div>
                <div className="space-y-4">
                    <div className="bg-gray-50 p-4 rounded">
                        <h2 className="font-semibold text-lg mb-1">O Mnie</h2>
                        <p className="text-gray-600">
                            Pracuję jako kierowniczka projektu w firmie BuildPro Development, gdzie odpowiadam za koordynację realizacji inwestycji oraz nadzór nad kluczowymi etapami budowy. W ramach swojej roli regularnie poszukuję rozwiązań, które usprawniają procesy i podnoszą jakość dokumentacji technicznej.
                        </p><p>
                            Obecnie jestem zainteresowana współpracą z doświadczonym operatorem drona, który może wspierać nasze projekty w zakresie inspekcji terenów budowy, monitorowania postępów prac, tworzenia ujęć do dokumentacji oraz generowania materiałów wizualnych wykorzystywanych w raportach i komunikacji z inwestorem. Zależy mi na osobie, która potrafi pracować w dynamicznym środowisku, zna procedury bezpieczeństwa i ma doświadczenie w realizowaniu lotów nad obszarami budowlanymi.

                            Cenię profesjonalizm, terminowość i wysoką jakość wykonania.
                        </p>
                    </div>

                </div>

            </div>
        </div>
    );
}
