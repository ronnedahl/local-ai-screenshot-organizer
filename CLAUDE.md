# CLAUDE.md — Skärmdumps-organiseraren (lokalt AI-portfolioprojekt)

> Den här filen är instruktioner till Claude Code. Läs hela innan du börjar koda.
> Bygg **bara fas 1** tills jag uttryckligen säger till. Fas 2 och 3 är medvetet utanför scope nu.

---

## 1. Vad det här är

En Android-app som hittar mina skärmdumpar, läser texten i dem **lokalt på telefonen**, föreslår en kategori, och låter mig söka/filtrera bland dem. Inga bilder lämnar enheten. Ingen molntjänst. Ingen prenumeration.

Det här är ett **lär- och portfolioprojekt**. Syftet är att jag ska lära mig hantera lokal AI på mobilen. Marknaden är redan mättad (Google Photos, PixelShot, Shots Studio m.fl.) — det spelar ingen roll. Koden ska vara ren, idiomatisk och något jag kan visa upp för rekryterare.

**Utvecklingsfilosofi (viktigt):** Jag vill förstå *varför* för varje steg, inte bara få kod som funkar. Förklara designval kort i kod-kommentarer och i commit-meddelanden. Bygg inkrementellt. Hellre en liten fungerande vertikal skiva än mycket halvfärdigt.

---

## 2. Hårda begränsningar (läs detta noga)

- **Testenhet: Samsung Galaxy A33** (mid-range, Exynos 1280, ~6 GB RAM, ingen kraftfull NPU).
- **Gemini Nano / ML Kit GenAI / AICore är FÖRBJUDET.** Det kräver flaggskepp med 12 GB+ RAM och Nano v3. A33:an stödjer det inte. Föreslå det inte ens som fallback.
- **Inga molnanrop för AI.** Ingen Firebase AI, ingen Gemini-API, inga externa inferens-endpoints. All inferens sker on-device.
- **Lättvikts-AI bara:** ML Kit traditionella API:er (fas 1) och LiteRT med liten kvantiserad modell (fas 2). Inget annat.
- **Inga on-device LLM:er** (MediaPipe LLM Inference / Gemma). För segt på den här hårdvaran — bygg det inte.

---

## 3. Teknikstack

- **Språk:** Kotlin (senaste stabila), Android Studio.
- **UI:** Jetpack Compose + Material 3.
- **Arkitektur:** MVVM — `ViewModel` + `StateFlow`/`UiState`, repository-mönster. UI-lagret är dumt och observerar state.
- **Persistens:** Room.
- **Bildåtkomst:** MediaStore (scoped storage).
- **Bildladdning i Compose:** Coil.
- **Asynkront:** Kotlin Coroutines + Flow.
- **DI:** Hilt rekommenderas (bra att visa upp). Manuell DI är OK om du vill hålla fas 1 lätt — välj och var konsekvent.
- **Bakgrundsjobb:** WorkManager för skanning/OCR-batch (se fas 1).
- **SDK:** `minSdk = 26`, `compileSdk`/`targetSdk` = senaste stabila (35 / Android 15 vid skrivande stund — använd den nyaste som Android Studio erbjuder).

Pinna inte gamla versioner av beroenden. Hämta senaste stabila artefakt-versioner när du sätter upp `build.gradle.kts`.

Relevanta artefakter (kolla senaste version):
- `com.google.mlkit:text-recognition` (Latin, on-device OCR)
- `com.google.mlkit:image-labeling` (on-device bildetiketter, fas 1 valfritt / fas 2)
- Room, Coil, WorkManager, Hilt via deras vanliga koordinater.

---

## 4. Datamodell (Room)

En entitet räcker för fas 1.

```
@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey val mediaStoreId: Long,   // stabil koppling till MediaStore
    val uri: String,                       // content:// URI som sträng
    val dateAdded: Long,                   // epoch-sekunder från MediaStore
    val ocrText: String?,                  // null tills OCR körts
    val category: String,                  // se Category nedan, default "OVRIGT"
    val status: String,                    // "ACTIVE" | "ARCHIVED" | "TRASH"
    val processedAt: Long?                 // när OCR/kategorisering kördes, null = ej bearbetad
)
```

Kategorier som en enum (svenska etiketter i UI, engelska konstanter i kod):

```
enum class Category { KVITTO, PRODUKT, KOD, TRANING, RESA, JOBB, OVRIGT }
```

`status` styr swipe-flödet senare (spara/arkivera/radera) utan att faktiskt radera filen på disk i fas 1 — vi flaggar bara i databasen.

---

## 5. Faser

### FAS 1 — Kärnan (bygg detta nu)

Mål: en app som skannar skärmdumpar, kör OCR lokalt, kategoriserar med enkla regler, sparar i Room, och visar en filtrerbar/sökbar lista.

**1.1 Behörigheter**
- Android 13+ (API 33+): begär `READ_MEDIA_IMAGES`.
- API < 33: begär `READ_EXTERNAL_STORAGE`.
- Hantera Android 14:s partiella mediaåtkomst (`READ_MEDIA_VISUAL_USER_SELECTED`) snyggt — om användaren bara ger delvis åtkomst, jobba med det den gett, krascha inte.
- Visa en kort förklaringsskärm *innan* systemdialogen (varför appen behöver bildåtkomst). Integritet är hela poängen med appen — var tydlig.

**1.2 Hitta skärmdumpar (MediaStore)**
- Fråga `MediaStore.Images` filtrerat till skärmdumpar (bucket/`RELATIVE_PATH` som innehåller "Screenshots").
- Hämta `_ID`, `DATE_ADDED`, och bygg content-URI.
- Synka mot Room: nya skärmdumpar läggs till med `processedAt = null`.

**1.3 OCR (ML Kit Text Recognition, on-device)**
- För varje obearbetad rad: ladda bitmap från URI, kör `TextRecognition`-klienten, spara resultatet i `ocrText`.
- Kör som en **WorkManager-batch** så det överlever att appen stängs och inte blockerar UI. Det här är ett bra lärmoment — förklara i kommentar varför WorkManager och inte bara en coroutine i ViewModel.
- Hantera fel per bild (korrupt bild, ingen text) utan att fälla hela jobbet.

**1.4 Kategorisering (regelbaserad — medvetet enkel i fas 1)**
- En ren funktion `categorize(ocrText: String?): Category`.
- Nyckelordsmatchning, svenska, t.ex. kvitto → "kvitto"/"summa"/"moms"/"totalt", resa → "boarding"/"avgång"/"hotell", kod → "function"/"const"/"error"/"stack", osv.
- **Det här är skört med flit.** Jag vet om det. Lägg matchningsreglerna i *en* fil (`Categorizer.kt`) som är lätt att byta ut senare. Skriv enhetstester för den.
- Användaren ska kunna ändra kategori manuellt på en bild → uppdaterar Room.

**1.5 UI (Compose, Material 3)**
- Lista/rutnät över skärmdumpar (Coil för thumbnails).
- Filter-chips per kategori överst.
- Sökfält som matchar mot `ocrText` (live filtrering via Flow).
- Detaljvy: stor bild + extraherad text + kategori-väljare.
- Tom-state och laddnings-state.

**1.6 Definition of done för fas 1**
- App startar, begär behörighet snyggt, listar mina riktiga skärmdumpar.
- OCR körs i bakgrunden och fyller på text progressivt (UI uppdateras live).
- Varje skärmdump får en föreslagen kategori; jag kan ändra den.
- Jag kan filtrera på kategori och fritextsöka i OCR-texten.
- Enhetstester för `Categorizer`.
- README med screenshots och en rad om varför allt körs lokalt.

---

### FAS 2 — LiteRT (bygg INTE förrän jag säger till)

Mål: bilder utan text (eller med lite text) klassificeras med en egen kvantiserad modell.
- LiteRT (f.d. TensorFlow Lite). En liten kvantiserad bildklassificerare i MobileNet-klass i `assets/`.
- CPU/GPU-delegate. Mät latens på A33:an.
- Detta är huvudlärmomentet: ladda `.tflite`-modell, kvantisering, delegater. Förklaras separat när vi kommer dit.

---

### FAS 3 — Röststyrning (bygg INTE förrän jag säger till)

Mål: snabbkommandon ovanpå ett system som redan funkar. Jag har gjort detta i Kylskåpskollen, så det är inte science fiction.
- Android `SpeechRecognizer`, on-device-igenkänning där det stöds, svensk locale.
- Enkla kommandon: "visa kvitton", "nästa", "spara", "radera", "flytta till produkt".
- Kom ihåg `destroy()` på recognizern.

---

## 6. Kodkonventioner

- Idiomatisk Kotlin. Inga onödiga abstraktioner — det här är ett lärprojekt, inte ett ramverk.
- En tydlig paketstruktur: `data/` (Room, MediaStore, repository), `domain/` (modeller, Categorizer), `ui/` (Compose-skärmar + ViewModels), `work/` (WorkManager).
- Inga hårdkodade strängar i UI:t — använd `strings.xml` (jag vill ha svenska som standardspråk).
- Commit ofta, små commits, beskrivande meddelanden som förklarar *varför*.
- Inga TODO-stubbar som låtsas vara klara. Om något inte är gjort, säg det.

## 7. Utanför scope (säg ifrån om du frestas)

- Gemini Nano / ML Kit GenAI / AICore / Firebase AI — nej.
- Molnanrop, inloggning, konton, analytics, annonser — nej.
- On-device LLM via MediaPipe — nej (för segt på A33).
- Faktisk radering av filer på disk i fas 1 — nej, bara `status`-flagga i Room.

---

## 8. Kom igång (för Claude Code)

1. Sätt upp ett tomt Android-projekt (Compose, Material 3, minSdk 26).
2. Lägg in beroenden enligt avsnitt 3 (senaste stabila versioner).
3. Bygg fas 1 i ordningen 1.1 → 1.6, en delsteg i taget, och stäm av med mig efter 1.2 (MediaStore-synk fungerar) och 1.3 (OCR fyller databasen) innan du fortsätter.
4. Stanna efter fas 1. Fråga innan du rör fas 2.