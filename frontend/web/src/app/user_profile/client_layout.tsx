"use client";

export default function ClientLayout() {
  return (
    <div
      className="grid grid-cols-2 grid-rows-1 justify-center items-center pl-10 pr-10 pt-10 pb-10 m-auto font-montserrat w-7xl"
      style={{ height: "85vh" }}
    >
      <style>{`
        @keyframes colorShine {
          0%, 100% {
            color: var(--color-primary-500);
          }
          50% {
            color: var(--color-primary-900);
          }
        }

        .shine-text {
          animation: colorShine 5s ease-in-out infinite;
        }
      `}</style>

      <div className="flex flex-col gap-3 items-center">
        <div className="flex flex-col items-center">
          <div className="w-48 h-48 bg-[#D9D9D9] rounded-full flex items-center justify-center shrink-0 drop-shadow-lg/40 hover:ring-4 hover:ring-[#D9D9D9] transition-all hover:drop-shadow-xl/50">
            <span className="text-7xl">👤</span>
          </div>
          <div className="flex text-black text-3xl pt-2">★★★★☆</div>
        </div>

        <div className="flex flex-col justify-center items-center flex-1">
          <h2 className="text-3xl font-light">Jan Kowalski</h2>
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
        </div>
      </div>

      <div className="bg-[#D9D9D9] rounded-4xl p-6 h-full w-[90%] flex flex-col">
        <h3 className="font-light text-lg mb-3 text-center">
          Najnowsze opinie na temat jkowalski
        </h3>

        <div className="flex-1">{/* Reviews content goes here */}</div>

        <button className="w-full bg-gray-700 text-white rounded-2xl py-2 font-semibold hover:cursor-pointer hover:bg-primary-800 hover:ring-2 hover:ring-primary-800 transition-all text-sm mt-4">
          Sprawdź więcej opinii
        </button>
      </div>
    </div>
  );
}
