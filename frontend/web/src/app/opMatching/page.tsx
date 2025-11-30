'use client'

import { useState } from 'react';

interface Operator {
    id: number;
    name: string;
    surname: string;
    username: string;
    certificates: Array<string>;
    services: Array<string>;
}

interface MatchResult {
  operatorId: number;
  decision: 'accept' | 'reject' | 'pending';
}

export default function OperatorMatchingPage() {
    const exampleOperator: Operator = {
        id: 1,
        name: "Jan",
        surname: "Kowalski",
        username: "jkowalski",
        certificates: ["A2"],
        services: ["ortofotomapa, fotogrametria"]
    };

    const [currentOperator, setCurrentOperator] = useState<Operator>(exampleOperator);
    const [matchResult, setMatchResult] = useState<MatchResult | null>(null);
    const [showResult, setShowResult] = useState(false);

    const accept = () => {
        const res: MatchResult = {
            operatorId: currentOperator.id,
            decision: 'accept',
        };
        setMatchResult(res);
        setShowResult(true);
    }

    const reject = () => {
        const res: MatchResult = {
            operatorId: currentOperator.id,
            decision: 'reject',
        }
        setMatchResult(res);
        setShowResult(true);
    }

    const handleNextOperator = () => {
        setShowResult(false);
        setMatchResult(null);
        console.log('Fetching next operator...');
    };

    const handleCheckUserAccount = () => {
        window.location.href = `/user_profile/`;
    };

return (
    <div>
      <h1>Zaakceptuj / odrzuć chętnych operatorów</h1>

      {!showResult ? (
        <div>
          <div>
            <h2>{currentOperator.name} {currentOperator.surname} ({currentOperator.username})</h2>
          </div>

          <div>
            <h3>Specjalizuje się w: </h3>
            <p>{currentOperator.services.join(', ')}</p>
          </div>

          <div>
            <h3>Certyfikaty: </h3>
            <p>{currentOperator.certificates.join(', ')}</p>
          </div>

          <div>
            <button onClick={handleCheckUserAccount} style={{ border: '1px solid blue', margin: '4px' }}>Szczegóły operatora</button>
          </div>

          <div>
            <button onClick={reject} style={{ border: '1px solid red', margin: '4px' }}>Odrzuć prośbę</button>
            <button onClick={accept} style={{ border: '1px solid green', margin: '4px' }}>Akceptuj prośbę</button>
          </div>
        </div>
      ) : (
        <div>
          {matchResult?.decision === 'accept' ? (
            <div>
              <h2>Zaakceptowano operatora! </h2>
              <p>Zostałeś sparowany z {currentOperator.name}!</p>
              <p>Operator zostanie wkrótce powiadomiony o akceptacji.</p>
            </div>
          ) : (
            <div>
              <h2>Odrzucono operatora.</h2>
              <p>Odrzucono prośbę od {currentOperator.name}.</p>
            </div>
          )}

          <button onClick={handleNextOperator} style={{ border: '1px solid orange', }}>Następny operator</button>
        </div>
      )}

      {matchResult && (
          <p style={ {color: 'red'} }>
            debug: id: {matchResult.operatorId} / decision: {matchResult.decision}
          </p>
      )}
    </div>
  );
}