"use client";

interface OperatorRegisterButtonProps {
  onClick: () => void;
}

export default function OperatorRegisterButton({
  onClick,
}: OperatorRegisterButtonProps) {
  return (
    <div>
      <style>{`
        @keyframes ripple {
          0% {
            box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.1), 0 0 0 20px rgba(255, 255, 255, 0.1), 0 0 0 40px rgba(255, 255, 255, 0.1), 0 0 0 60px rgba(255, 255, 255, 0.1);
          }
          100% {
            box-shadow: 0 0 0 20px rgba(255, 255, 255, 0.1), 0 0 0 40px rgba(255, 255, 255, 0.1), 0 0 0 60px rgba(255, 255, 255, 0.1), 0 0 0 80px rgba(255, 255, 255, 0);
          }
        }

        .ripple-animation {
          animation: ripple 0.6s linear infinite;
        }
      `}</style>

      <button
        onClick={onClick}
        className="outline-none inline-flex items-center justify-between bg-primary-600 min-w-[200px] border-0 rounded-xl px-5 py-4 text-white text-xs font-semibold tracking-widest uppercase overflow-hidden cursor-pointer shadow-lg hover:opacity-80 transition-opacity"
      >
        <span className="ripple-animation rounded-full"></span>
        Zostań operatorem
        <span className="ripple-animation rounded-full"></span>
      </button>
    </div>
  );
}
