"use client";

interface AddToCalButtonProps {
  orderId: string;
}

export default function AddToCalButton({ orderId }: AddToCalButtonProps) {
  const handleAddToCalendar = async () => {
    console.log("Dodawanie do kalendarza zlecenia:", orderId);
  };

  return (
    <button
      onClick={handleAddToCalendar}
      className="flex items-center gap-2 px-4 py-2 bg-gray-700 hover:bg-green-600 text-white rounded-xl transition-all text-sm font-medium"
    >
      <span>📅</span>
      Dodaj do Google Calendar
    </button>
  );
}
