# Padel termini – kako dobiti APK

1. Skini i instaliraj **Android Studio** (besplatno): https://developer.android.com/studio
   Kod prvog pokretanja ostavi sve na „Standard” i pusti ga da skine što treba.
2. Otpakiraj ovaj zip, npr. u `Dokumenti\PadelTermini`.
3. U Android Studiju: **File → Open**, odaberi mapu `PadelTermini` (onu u kojoj je ova datoteka) → **Trust Project**.
4. Pričekaj da dolje nestane „Gradle sync” traka (prvi put par minuta, skida alate).
   Ako ponudi „Upgrade” ili „AGP Upgrade Assistant”, slobodno klikni da – ili ignoriraj, radi i ovako.
5. Gornji izbornik: **Build → Generate App Bundles or APKs → Generate APKs**
   (u starijim verzijama: **Build → Build Bundle(s) / APK(s) → Build APK(s)**).
6. Kad gotovo, dolje desno klikni **locate**. Datoteka je
   `app\build\outputs\apk\debug\app-debug.apk`.
7. Prebaci `app-debug.apk` na mobitel (kabel, Google Drive, mail sebi…) i otvori je na mobitelu.
   Mobitel će pitati smije li instalirati aplikacije iz tog izvora – dozvoli.
8. Kod prvog otvaranja aplikacije dozvoli **obavijesti**.

## Podsjetnici
- Dan prije termina (u isto vrijeme) i 1 sat prije termina.
- Termin bez vremena: samo podsjetnik dan prije u 9:00.
- Žuti termini („Dao nekom”) nemaju podsjetnike.
- Crveni („Otkazati”) imaju podsjetnik koji kaže da trebaš otkazati.
- Na dnu liste je gumb „Pošalji probnu obavijest” da vidiš rade li obavijesti.
- Ako obavijesti kasne, u postavkama mobitela za aplikaciju „Padel termini” isključi
  ograničenje baterije (Baterija → Bez ograničenja). Neki Xiaomi/Huawei/Samsung mobiteli to traže.

## Napomena
Termini se spremaju na mobitelu. Ako deinstaliraš aplikaciju, brišu se.
Za novu verziju aplikacije samo ponovi korak 5–7 na istom laptopu; termini ostaju.

## Filter i izvoz
- Gumb **Filter**: biraš više boja, više centara i raspon datuma odjednom.
- Gumb **Izvoz**: svi trenutno prikazani termini kao tekst, jedan red po terminu,
  npr. `11.11. srijeda 19:30 h - PAZ - milky/zaja`. Kopiraj ili Podijeli (Bilješke, WhatsApp…).
