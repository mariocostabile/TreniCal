## Backlog & Roadmap

### Moduli
- **treni-cal-proto** — .proto e classi gRPC generate
- **treni-cal-server** — servizi gRPC (bootstrap, api.grpc, application, domain, infra/db, infra/adapters, realtime)
- **treni-cal-client** — client (CLI/GUI minima) con `net/GrpcClientService`
- **treni-cal-shading** — packaging fat-jar

### Requisiti Funzionali (FR)
- **FR-1 Ricerca stazioni** – input stringa; output elenco stazioni.
- **FR-2 Ricerca treni** – origine/destinazione/data (+preferenze).
- **FR-3 Acquisto biglietto(i)** – stesso treno/stesso utente.
- **FR-4 Modifica biglietto** – cambio orario/classe con differenza/penale.
- **FR-5 Pagamento simulato** – no gateway reali.
- **FR-6 Prenotazioni (opz.)** – prenota con scadenza, conferma/decaduta.
- **FR-7 Promozioni** – regole su tratta/periodo/tipo, targeting Fedeltà.
- **FR-8 Monitoraggio real-time** – subscribe a stato treno (ritardi/binari).
- **FR-9 Auto-notifiche su viaggi attivi** – push a chi ha biglietto/prenotazione.
- **FR-10 Notifiche promozioni Fedeltà** – invii periodici (opt-in).
- **FR-11 Client GUI minima** – flussi base.
- **FR-12 GUI admin server** – gestione promo/overview.
- **FR-13 Persistenza (opz.)** – DB/file (es. SQLite).

### Requisiti Non Funzionali (NFR)
- **NFR-1 Scalabilità** – concorrenti elevati (target test >1000 req concorrenti).
- **NFR-2 Performance** – ricerca/acquisto <~200ms in locale.
- **NFR-3 Affidabilità** – riavvio sicuro moduli.
- **NFR-4 Usabilità** – CLI/GUI lineare per casi principali.
- **NFR-5 Modularità** – separazione in 4 moduli Maven.
- **NFR-6 Integrazione** – microservizio esterno (**Python**) via gRPC.

### Milestones
- **M1 – Core MVP (FR-1,2,3,5,8)**  
  Server+Client con ricerche, acquisto singolo, subscribe RT (mock), packaging runnable.
- **M2 – Biglietteria Pro (FR-3 esteso, FR-4, FR-9, FR-13)**  
  Multi-acquisto, modifica con differenza/penale, persistenza base, auto-notifiche.
- **M3 – Promo/Loyalty (FR-7, FR-10, parti FR-12)**  
  CRUD promozioni + invii periodici + GUI admin minima.
- **M4 – Prenotazioni (FR-6)**  
  Prenota, scadenza, reminder, conferma/decaduta.

### Criteri di Accettazione (per M1)
- **FR-1/2**: dato origine/destinazione/data, il server risponde con ≥1 treno valido; tempo medio <~200ms in locale (10 chiamate ripetute).
- **FR-3**: acquisto ritorna `ticketId` univoco; input minimi (`userId`, `trainId`, `fareClass`).
- **FR-5**: pagamento fittizio (sempre esito OK o regola simulata) senza gateway esterni.
- **FR-8**: subscribe a un `trainId` riceve ≥2 aggiornamenti streaming (ritardo/binario), poi `onCompleted()`.

### Tracciabilità (Servizi ↔ Moduli)
- **treni-cal-proto**: definizioni `TreniCalService` + DTO (Station, Train, Ticket, …). 
- **treni-cal-server**: `api.grpc/*ServiceImpl` per FR-1/2/3/5/8 (M1), poi estensioni M2–M4. 
- **treni-cal-client**: `net/GrpcClientService` + GUI/CLI.
- **treni-cal-shading**: JAR eseguibile.

### Definizioni di Done (DoD)
- Compila con `mvn clean install` su main.
- Test minimi M1: unit su ricerche/acquisto; uno streaming test.
- README aggiornato con istruzioni run (server/client) + screenshot CLI/GUI.
- `.proto` versionati e rigenerabili con un comando.
