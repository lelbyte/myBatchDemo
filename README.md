# Customer Invoice Batch Processing

## Überblick
Dieses Projekt entstand im Rahmen meiner Einarbeitung in Spring Batch.
Die Umsetzung sowie die technischen Details erläutere ich gerne näher im Vorstellungsgespräch :-)

Dieses Projekt ist eine Spring-Batch-Anwendung zur Verarbeitung von Bestelldaten.

Dieses Projekt ist eine Spring-Batch-Anwendung zur Verarbeitung von Bestelldaten.

Die Anwendung liest verschiedene Dateiformate wie .json und .csv ein, verarbeitet die Daten und generiert daraus 
.xml- sowie .csv-Dateien. Zusätzlich wird eine H2-Datenbank genutzt, um Daten zu speichern, auszulesen und 
Datenbankoperationen wie Schemaänderungen mithilfe von Tasklets durchzuführen.

Die Anwendung demonstriert folgende Konzepte im Bereich Batch-Processing:
- Dateibasierte Datenverarbeitung
- Chunk-orientierte Verarbeitung
- Partitionierung
- Tasklets
- Readers/Processors/Writers inkl. Multi-Threading Verarbeitung
- Listeners
- DTO-Mapping
- Datenbankmigrationen

## Technologien
- Java
- Spring Batch
- Spring Boot (nur um Projekt zu starten)
- Maven
- YAML-Konfiguration
- Flyway Datenbankmigration
- Docker
- H2 Datenbank

## Projektstruktur
```text
src/main/java/com/example/myBatchDemo
│
├── APIs              # Fake REST-Endpunkte
├── Configs           # Spring-Batch-Konfigurationen (aktuell nur Wrapper für Multi-Threading Verarbeitung)
├── DTOs              # Data Transfer Objects
├── Listeners         # Job- und Step-Listener (eher fürs Logging von Meta-Daten verwendet)
├── Mappers           # Mapping-Logik
├── Partitioners      # Parallelisierung / Partitionierung
├── Processors        # Datenverarbeitung
├── Readers           # Daten einlesen
├── Services          # Business-Logik
├── Tasklets          # Tasklet-Schritte
├── Writers           # Daten schreiben
│
├── AmazonOrdersJobConfiguration.java
└── MyBatchDemoApplication.java
```

### Ressourcenstruktur
```text
src/main/resources
│
├── db.migration      # Datenbank-Migrationsskripte
├── input             # Eingabedateien
└── application.yaml  # Anwendungskonfiguration
```
### Batch-Verarbeitungsablauf

Herzstück der Anwendung ist der Batch-Job AmazonOrdersJobConfiguration.java.


Der Batch-Job verarbeitet Amazon-Bestelldateien in folgenden Schritten:
*(BPMN-Diagramm kommt noch)*


### Configuration

Die Anwendungskonfiguration befindet sich unter: src/main/resources/application.yaml

### Eingabedateien

Die Eingabedateien befinden sich unter:
src/main/resources/input

### Ausgabedateien

Die generierten Dateien werden im folgenden Verzeichnis gespeichert:
/output

## Anwendung starten

### Empfehlung: Anwendung mit Docker starten
Ein Dockerfile ist bereits im Projekt beigefügt. Folgender Befehl startet die Anwendung: 

```bash 
docker build --no-cache -t my-batch-demo .
docker run --rm \
  -p 8080:8080 \
  -v "$(pwd)/data:/app/data" \
  -v "$(pwd)/output:/app/output" \
  -e SPRING_PROFILES_ACTIVE=docker \
  my-batch-demo
```

Verwendet wird eine embedded H2-Datenbank. Sie kann lokal durch folgenden Link aufgerufen werden:
http://localhost:8080/h2-console 

Im Browser bitte folgende Daten eingeben:
- JDBC URL: jdbc:h2:file:./data/testdb
- username: dataprocessing
- password: password

### Anwendung mit MyBatchDemoApplication.java starten

Einfach die Klasse MyBatchDemoApplication.java starten.
Bitte Projekt vorher auf Java SDK 21 setzen.

### Bereinigung nach jedem Batch-Durchlauf
Batch und Flyway arbeiten mit temporären Daten und Zwischenergebnissen.

Nach jedem Durchlauf sollten deshalb die Datenordner gelöscht werden, da die veralteteten Daten inkonsistentes Verhalten 
verursachen könnten. 

Mit diesem Befehl werden die alten Daten gelöscht und das Projekt wird neugebaut:

```bash 
./mvnw clean package
rm -rf data && mkdir data
```