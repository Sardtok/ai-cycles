# Pakker og klassen for boten din #

AI-Cycles har en pakkestruktur som følger:
  * no.uio.ifi.sonen.aicycles - Klasser som er generelle for prosjektet.
  * no.uio.ifi.sonen.aicycles.net - Klasser som håndterer nettverkskommunikasjon.
  * no.uio.ifi.sonen.aicycles.server - Klasser som er spesifikke for tjeneren og simulatoren.

Så hvor hører boten din hjemme? Den er egentlig ikke en del av AI-Cycles-prosjektet, men et eget prosjekt som bruker klasser i prosjektet. Så da lager vi heller en ny pakke. Du kan for eksempel bruke no.uio.folk.brukernavn.aicycles (altså domenet for studenter og ansatte ved uio, brukernavnet ditt og til slutt prosjektnavnet). Hvis du ikke vil lage noen pakke, vil det fungere å bruke standardpakken også (altså ikke sette noen pakke). Se http://programmering.wiki.ifi.uio.no/Pakker_i_Java for mer informasjon om å opprette pakker.

Lag en klasse for boten din, f.eks. Sigmunha (det trenger ikke være brukernavnet ditt). Importer no.uio.ifi.sonen.aicycles.BotBase og utvid denne:

```
package no.uio.folk.sigmunha.aicycles;

import no.uio.ifi.sonen.aicycles.BotBase;

/**
 * My bot!
 */
public class Sigmunha extends BotBase {

	/**
	 * Does nothing specific, but calls BotBase's constructor
	 * which will connect to the server and identify us.
	 */
	public Sigmunha(String server) {
		super(server);
	}

	/**
	 * Gets the name of this bot.
	 * Used to identify the bot when connecting to the server.
	 *
	 * @return This bot's name.
	 */
	@Override
	public String getName() {
		return "sigmunha";
	}
}
```

Hvis man oppretter et objekt av denne klassen, vil den koble til tjeneren. Hvis man kaller på metoden _start_, vil den også begynne å lytte til pakker fra tjeneren.

# AwesomeBot som utgangspunkt #

Kopier koden i AwesomeBot og endre navnet på klassen, pakken og konstruktøren à la det vist ovenfor. Den metoden som er mest interessant for oss, er **run**:

```
    /**
     * If there has been any updates,
     * it will randomly choose a new direction unless it will lead to a crash.
     */
    public void run() {
        int lastUpdate = updates - 1;
        while (cycles[id - 1].isAlive()) {
            synchronized (this) {
                while (updates <= lastUpdate) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                    }
                }
                lastUpdate = updates;
            }
            
            if (!cycles[id - 1].isAlive()) {
                break;
            }

            double chance = random.nextDouble();
            Cycle c = cycles[id-1];
            Direction dir = c.getDirection();
            int x = c.getX();
            int y = c.getY();
            boolean forward = false, left = false, right = false;
            switch (dir) {
                case N:
                    forward = map[x][y - 1] != 0;
                    left = map[x - 1][y] != 0;
                    right = map[x + 1][y] != 0;
                    break;
                case E:
                    forward = map[x + 1][y ] != 0;
                    left = map[x][y - 1] != 0;
                    right = map[x][y + 1] != 0;
                    break;
                case W:
                    forward = map[x - 1][y ] != 0;
                    left = map[x][y + 1] != 0;
                    right = map[x][y - 1] != 0;
                    break;
                case S:
                    forward = map[x][y + 1] != 0;
                    left = map[x + 1][y] != 0;
                    right = map[x - 1][y] != 0;
                    break;
            }
            
            if (chance <= 0.3 && !left) {
                turnLeft();
            } else if (chance >= 0.7 && !right) {
                turnRight();
            } else if (forward) {
                if (!right) {
                    turnRight();
                } else {
                    turnLeft();
                }
            }
        }
    }
```

Metoden består av en løkke som går helt til spilleren er død: `while (cycles[id - 1].isAlive())`. Inne i denne brukes en løkke i en synkronisert blokk til å synkronisere AIen med serveren. Denne er ikke helt nødvendig, men gjør det lettere å holde følge med serveren (slik at man ikke prøver å tenke ut to planer før den første er gjennomført).

Etter den synkroniserte blokka, sjekker vi at spilleren fortsatt lever. Det kan hende at vi fikk en oppdatering fra serveren som sa at spilleren er død, og da vil vi avrbyte løkka. En annen mulighet er å flytte den synkroniserte blokka til slutt og fjerne denne testen, men da vil vi utføre den vanlige tenkingen før første tur, og det kan være en fordel å ha en spesiell logikk for å avgjøre startretning.

Etter denne if-setningen, kommer logikken som brukes av boten for å beregne hvilken rute den skal flytte seg til neste gang. Denne koden kan du bytte ut (fra og med opprettingen av **chance**). Merk at switchen inneholder kode for å sjekke om man kolliderer neste runde om man fortsetter rett fram, svinger til venstre eller til høyre, så denne kan være nyttig avhengig av hvordan AIen din velger hva den skal gjøre.

# Sette startretning #

For å endre startretningen må vi sette inn kode før løkka i **run**. Det er lurt å lage en funksjon som kalles før løkka for å ta dette valget. Under følger kode som passer for å endre AwesomeBot til å velge en tilfeldig startretning.

```
private void chooseStartDirection() {
	Direction d = dirs[random.nextInt(dirs.length)];

	setDirection(d);
}

public void run() {
	chooseStartDirection();
	// Endringen under gjør at innholdet i while-løkka ikke blir kjørt
	// før det skjer noen bevegelser
	int lastUpdate = updates;
	...
}
```

# Tilfeldige tall #

Hvis man skal bruke tilfeldige tall, må man bruke frøet (seeden) som sendes av serveren. Merk at koden til AwesomeBot bruker **random.next()** for å få et tilfeldig flyttall mellom 0.0 og 1.0. Alle metodene som finnes i **java.util.Random** er tilgjengelig, så  vi kan bruke for eksempel **nextInt** som i eksemplet i **chooseStartDirection** ovenfor.