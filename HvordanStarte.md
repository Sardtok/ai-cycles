# How To #

## Eclipse ##
Først må du laste ned koden og legge den inn som et Java-prosjekt. For å lage lage et nytt Java-prosjekt i Eclipse må du lage "Run Configurations" for serveren og hver spiller. Dette kan enklest gjøres ved å gå til Server.java og trykke "Run". Så går du til hver av Bot-klassene og trykker på "Run" der også.

Hver spiller må også ha et navn som passer med det som blir sendt med inn i linje 48 i Server.java.

Så kjører du serveren opp ved å trykke Run (Ctrl+F11), etter det så gjør du det samme med hver av spillerne. Når begge har logget seg på vil spillet starte med engang.

## NetBeans ##
For å laste ned prosjektet, velger du: _Team → Mercurial → Clone Other_. I veiviseren som dukker opp fyller du inn adressen som ligger på siden _Source_. I det siste vinduet i veiviseren velger du hvor på filsystemet du vil ha prosjektet.

Når prosjektet har blitt lagt til, kan du kjøre tjeneren ved å trykke F6 (Run main project) hvis du har satt det som hovedprosjektet ditt. Hvis du har et annet hovedprosjekt, kan du høyreklikke på prosjektet i lista og velge _Run_ eller åpne eller markere Server.java og trykke Shift+F6 (Run file).

For å starte _AwesomeBot_ kan du høyreklikke på prosjektet og velge _Custom → Run AwesomeBot_. Eller markerer eller åpner AwesomeBot og trykker Shift+F6. Hvis du skal endre adressen AwesomeBot kobler til, kan du høyreklikke på prosjektet og velge _Properties → Actions → Run AwesomeBot_ og legge til adressen til slutt i _exec.args_.

## Maven ##
Om du ikke bruker Eclipse, er det mulig å bygge prosjektet med Maven. Gå til prosjektets rotmappe og kjør kommandoen: `mvn package` Da blir prosjektet kompilert og pakket i et JAR-arkiv i mappen _target_.

Hvis du ikke vil bruke et JAR-arkiv, kan du kompilere tjeneren med: `mvn compile`.

For å kjøre tjeneren bruker du en av følgende kommandoer:
```
java -jar target/AICycles-1.0-SNAPSHOT.jar
java -cp target/classes/ no.uio.ifi.sonen.aicycles.server.Server
```

For å starte AwesomeBot (joe):
```
java -cp target/AICycles-1.0-SNAPSHOT.jar no.uio.ifi.sonen.aicycles.AwesomeBot adresse
java -cp target/classes/ no.uio.ifi.sonen.aicycles.AwesomeBot adresse
```

La _adresse_ stå tom om du kjører tjeneren på samme maskin.

## Manuelt ##
For å kompilere tjeneren helt manuelt kan du gå til rotmappen til prosjektet og kjøre følgende kommandoer (du trenger bare opprette mappen _server_ første gangen):

```
mkdir server
javac -sourcepath src/main/java src/main/java/no/uio/ifi/sonen/aicycles/server/Server.java -d server
jar -cvfe Server.jar no.uio.ifi.sonen.aicycles.server.Server -C server .
```

Nå kan du kjøre tjeneren med:
```
java -jar Server.jar
```

For å kompilere AwesomeBot kjører du følgende:

```
mkdir awesome
javac -sourcepath src/main/java src/main/java/no/uio/ifi/sonen/aicycles/AwesomeBot.java -d awesome
jar -cvfe AwesomeBot.jar no.uio.ifi.sonen.aicycles.AwesomeBot -C awesome .
```

Nå kan du kjøre AwesomeBot med en av følgende:
```
java -jar AwesomeBot.jar
java -jar AwesomeBot.jar adresse
```

# Tips #
  * For å hindre at server skal lukke seg etter spill kan du kommentere ut linje 49 i filen Server.java.
  * Du kan endre navn på AIene som skal være med å spille på linje 47.
  * Du kan endre størrelsen på kartet på samme linje, men størrelsen bør ikke overgå 720 i noen av retningene (ellers blir syklene usynlige).