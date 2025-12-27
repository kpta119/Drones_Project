"use client";

import Image from "next/image";

export default function ProfilePage() {
  return (
    <div
      className="grid grid-cols-2 grid-rows-2 gap-10 ps-5 pt-10 pb-10 m-auto font-montserrat w-7xl"
      style={{ height: "85vh" }}
    >
      <div className="rounded-2xl p-8 flex flex-col">
        <div className="flex gap-6">
          <div className="flex flex-col items-center gap-3">
            <div className="w-48 h-48 bg-[#D9D9D9] rounded-full flex items-center justify-center shrink-0 drop-shadow-lg/40 hover:ring-2 hover:ring-[#D9D9D9] transition-all hover:drop-shadow-xl/50">
              <span className="text-7xl">👤</span>
            </div>
            <div className="flex text-black text-3xl">★★★★☆</div>
            <p className="text-sm text-primary-800 font-semibold text-center">
              Operator dronów
            </p>
          </div>

          <div className="flex flex-col justify-start pt-6 pl-5 flex-1">
            <h2 className="text-4xl font-light">Jan Kowalski</h2>
            <p className="text-gray-600 text-lg mb-4">jkowalski</p>
            <div className="space-y-1 mb-6">
              <div className="flex items-center gap-2">
                <span>📞</span>
                <p className="">+48 123 456 789</p>
              </div>
              <div className="flex items-center gap-2">
                <span>✉️</span>
                <p className="">jkowalski@gmail.com</p>
              </div>
            </div>

            <div className="flex gap-2 w-full">
              <button className="flex-1 bg-[#D9D9D9] text-black rounded-full py-2 font-semibold hover:bg-gray-400 hover:ring-2 hover:ring-gray-400 transition-all text-sm">
                Czytaj opinię
              </button>
              <button className="flex-1 bg-primary-500 text-black rounded-full py-2 font-semibold hover:bg-primary-400 hover:ring-2 hover:ring-primary-400 transition-all text-sm">
                Napisz opinię
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-gray-300 rounded-2xl p-6">
        <h3 className="font-semibold mb-3">O mnie:</h3>
        <p className="text-gray-700 text-sm">test</p>
      </div>

      <div className="bg-gray-300 rounded-2xl overflow-hidden relative">
        <div className="absolute inset-0 bg-linear-to-t from-black/40 to-transparent flex items-center justify-center hover:bg-black/30 hover:cursor-pointer transition-all">
          <p className="text-white text-lg font-semibold">
            Sprawdź zdjęcia <span className="font-extrabold">jkowalski</span>
          </p>
        </div>
      </div>

      <div className="bg-gray-300 rounded-2xl p-6">
        <h3 className="font-semibold mb-3">Usługi:</h3>
        <ul className="text-sm space-y-2 text-gray-700"></ul>
      </div>
    </div>
  );
}
