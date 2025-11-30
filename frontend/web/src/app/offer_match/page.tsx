'use client'

import { useState, useEffect } from 'react';

interface Offer {
    id: number;
    clientName: string;
    clientSurname: string;
    clientUsername: string;
    shortDesc: string;
    location: string;
    service: string;
    endDate: Date;
}

interface MatchResult {
  offerId: number;
  decision: 'accept' | 'reject' | 'pending';
}

export default function OfferMatchingPage() {
    const exampleOffer: Offer = {
        id: 1,
        clientName: "Andrzej",
        clientSurname: "Jeziorski",
        clientUsername: "ajezior",
        location: "api google maps",
        shortDesc: "Chcę zdjęć mojej działki",
        service: "ortofotomapa",
        endDate: new Date(2025, 10, 24, 23, 59)
    };

    const [currentOffer, setCurrentOffer] = useState<Offer>(exampleOffer);
    const [matchResult, setMatchResult] = useState<MatchResult | null>(null);
    const [showResult, setShowResult] = useState(false);
    const [formattedDate, setFormattedDate] = useState<string>('');

    useEffect(() => {
        setFormattedDate(currentOffer.endDate.toLocaleString());
    }, [currentOffer]);

    const accept = () => {
        const res: MatchResult = {
            offerId: currentOffer.id,
            decision: 'accept',
        };
        setMatchResult(res);
        setShowResult(true);
    }

    const reject = () => {
        const res: MatchResult = {
            offerId: currentOffer.id,
            decision: 'reject',
        }
        setMatchResult(res);
        setShowResult(true);
    }

    const handleNextOffer = () => {
        setShowResult(false);
        setMatchResult(null);
        console.log('Fetching next Offer...');
    };

    const handleCheckUserAccount = () => {
        window.location.href = `/user_profile/`;
    };

return (
    <div>
      <h1>Zgłoś chęć / brak chęci spełnienia zlecenia</h1>

      {!showResult ? (
        <div>
          <div>
            <h2>Zlecenie od {currentOffer.clientName} {currentOffer.clientSurname} ({currentOffer.clientUsername})</h2>
          </div>

          <div>
            <h3>Skrócony opis zlecenia: {currentOffer.shortDesc}</h3>
            <p>{currentOffer.service}</p>
          </div>

          <div>
            <h3>Spodziewa się wykonania zlecenia do: {formattedDate}</h3>
          </div>

          <div>
            <button onClick={handleCheckUserAccount} style={{ border: '1px solid blue', margin: '4px' }}>Szczegóły zlecającego</button>
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
              <h2>Zgłosiłeś się jako chętny! </h2>
              <p>Zlecający zostanie wkrótce powiadomiony o twoim zgłoszeniu.</p>
              <p>Zlecający musi zdecydować, czy chcę z tobą współpracować. Jeżeli zostaniesz wybrany do współpracy, powiadomimy cię o tym!</p>
            </div>
          ) : (
            <div>
              <h2>Odrzucono zlecenie.</h2>
              <p>Czy chcesz zobaczyć inne, dostępne zlecenia?</p>
            </div>
          )}

          <button onClick={handleNextOffer} style={{ border: '1px solid orange', }}>Następne zlecenie</button>
        </div>
      )}

      {matchResult && (
          <p style={ {color: 'red'} }>
            debug: id: {matchResult.offerId} / decision: {matchResult.decision}
          </p>
      )}
    </div>
  );
}