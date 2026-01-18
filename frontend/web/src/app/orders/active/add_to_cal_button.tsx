"use client";

import { useState } from "react";
import { FaCalendarPlus, FaCheck, FaTimes } from "react-icons/fa";

interface AddToCalButtonProps {
  orderId: string;
  alreadyAdded?: boolean | null;
}

export default function AddToCalButton({
  orderId,
  alreadyAdded = null,
}: AddToCalButtonProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [isAdded, setIsAdded] = useState(alreadyAdded === true);
  const [showNotification, setShowNotification] = useState(false);
  const [notificationMessage, setNotificationMessage] = useState("");

  const isNotGoogleConnected =
    alreadyAdded === null || alreadyAdded === undefined;

  const handleAddToCalendar = async () => {
    if (isAdded) {
      return;
    }

    if (isNotGoogleConnected) {
      setNotificationMessage(
        "Zaloguj się przez Google aby dodawać zdarzenia do kalendarza"
      );
      setShowNotification(true);
      setTimeout(() => setShowNotification(false), 4000);
      return;
    }

    setIsLoading(true);

    try {
      const token = localStorage.getItem("token");
      const res = await fetch(`/api/calendar/addEvent/${orderId}`, {
        method: "POST",
        headers: {
          "X-USER-TOKEN": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });

      if (res.ok) {
        const link = await res.text();
        setIsAdded(true);
        setNotificationMessage("Dodano do Google Calendar! 🎉");
        setShowNotification(true);
        setTimeout(() => setShowNotification(false), 3000);
        window.open(link, "_blank");
      } else if (res.status === 400) {
        setNotificationMessage(
          "Zaloguj się przez Google aby dodawać zdarzenia do kalendarza"
        );
        setShowNotification(true);
        setTimeout(() => setShowNotification(false), 4000);
      } else {
        setNotificationMessage("Nie udało się dodać do kalendarza");
        setShowNotification(true);
        setTimeout(() => setShowNotification(false), 4000);
      }
    } catch (err) {
      setNotificationMessage("Błąd połączenia");
      setShowNotification(true);
      setTimeout(() => setShowNotification(false), 4000);
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const baseClasses =
    "flex items-center gap-2 px-4 py-2 rounded-xl transition-all text-sm font-medium";

  if (isAdded) {
    return (
      <button
        disabled
        className={`${baseClasses} bg-green-100 text-green-700 border border-green-300 cursor-default`}
      >
        <FaCheck size={14} />W kalendarzu
      </button>
    );
  }

  return (
    <>
      <button
        onClick={handleAddToCalendar}
        disabled={isLoading}
        className={`${baseClasses} ${
          isNotGoogleConnected
            ? "bg-yellow-100 text-yellow-700 hover:bg-yellow-200 border border-yellow-300"
            : "bg-gray-700 hover:bg-green-600 text-white"
        } disabled:opacity-50 disabled:cursor-not-allowed`}
      >
        <FaCalendarPlus size={14} />
        {isLoading ? "Dodawanie..." : "Google Calendar"}
      </button>

      {showNotification && (
        <div
          className={`fixed bottom-4 right-4 flex items-center gap-3 px-6 py-4 rounded-lg shadow-lg animate-fadeIn text-sm font-medium z-50 ${
            isNotGoogleConnected || notificationMessage.includes("Zaloguj")
              ? "bg-yellow-50 text-yellow-800 border border-yellow-200"
              : notificationMessage.includes("Dodano")
              ? "bg-green-50 text-green-800 border border-green-200"
              : "bg-red-50 text-red-800 border border-red-200"
          }`}
        >
          <span>{notificationMessage}</span>
          <button
            onClick={() => setShowNotification(false)}
            className="ml-2 hover:opacity-70 transition-opacity"
          >
            <FaTimes size={14} />
          </button>
        </div>
      )}
    </>
  );
}
