"use client";

import { useState } from "react";
import { CertificatesInputModule } from "./cert_input_module";

interface OperatorRegisterModuleProps {
  onClose: () => void;
}

export default function OperatorRegisterModule({
  onClose,
}: OperatorRegisterModuleProps) {
  const [step, setStep] = useState(1);

  const handleSubmit = async () => {
    onClose();
  };

  return (
    <div>
      {step === 1 && <CertificatesInputModule onNext={() => setStep(2)} />}
      {step === 2 && <div>Step 2</div>}
      {step === 3 && <div>Step 3</div>}
      {step === 4 && <button onClick={handleSubmit}>Zatwierdź</button>}
    </div>
  );
}
