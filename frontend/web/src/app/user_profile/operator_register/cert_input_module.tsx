import { useState } from "react";

export function CertificatesInputModule({ onNext }: { onNext: () => void }) {
  const [certs, setCerts] = useState<string[]>([]);

  return (
    <div>
      <h2>Dodaj certyfikaty</h2>
      <input
        placeholder="Nazwa certyfikatu"
        onKeyPress={(e) => {
          if (e.key === "Enter") {
            setCerts([...certs, e.currentTarget.value]);
            e.currentTarget.value = "";
          }
        }}
      />
      <div>
        {certs.map((c, i) => (
          <span key={i}>{c} ✕</span>
        ))}
      </div>
      <button onClick={onNext}>Dalej</button>
    </div>
  );
}
