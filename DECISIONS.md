# Moje rozhodnutia a poznámky k riešeniu (DECISIONS.md)

Pri implementácii tohto systému som narazil na niekoľko "špinavých" dát v JSON súboroch. Tu je prehľad toho, ako som sa s nimi vysporiadal a prečo som zvolil daný prístup.

## 1. Problémy v dátach, ktoré som musel vyriešiť

### Chýbajúca cena (pricePerDay)
Všimol som si, že auto **C-500 (Kia Ceed SW)** nemá v dátach vôbec pole `pricePerDay` a **Tesla Model 3** ho má nastavené na `0`.
- **Moje rozhodnutie:** Pre tieto autá vraciam `estimatedPrice: null`. Auto v zozname nechávam, aby zákazník videl, že existuje, ale systém mu jednoducho nevypočíta cenu. Do logov som pridal varovanie (warning), aby to operátor mohol skontrolovať.

### Neplatné dátumy rezervácií (to ≤ from)
Rezervácia **R-020** má návrat skôr ako vyzdvihnutie. To je zjavná chyba pri vstupe dát.
- **Moje rozhodnutie:** Toto považujem za **DATA conflict**. Takúto rezerváciu neviem férovo vyhodnotiť na časovej osi, takže neblokuje auto (isAvailable: true), ale započítavam ju do počtu konfliktov, aby systém signalizoval, že niečo nie je v poriadku.

### Chýbajúce dátumy alebo statusy
Niektoré rezervácie nemajú buď dátum "od-do" (**R-022**) alebo im úplne chýba `status` (**R-021**). 
- **Moje rozhodnutie:** Ak chýba status, považujem to za **SOFT conflict**. Systém takéto auto v zozname zobrazí s výstrahou, ale nezakáže jeho rezerváciu. Ak totiž nevieme naisto, či je rezervácia potvrdená, nechceme zbytočne odmietnuť nového zákazníka.

## 2. Klasifikácia konfliktov

Rozdelil som to takto, aby v tom mala logika jasno:

| Typ | Podmienka | Blokuje auto? |
|------|-----------|-------------|
| **HARD** | Existuje prekryv + CONFIRMED/PICKED_UP | ✅ Áno |
| **SOFT** | Prekryv + UNKNOWN alebo chýbajúci status | ❌ Nie (iba varovanie) |
| **DATA** | Neplatné alebo chýbajúce dátumy | ❌ Nie |

## 3. Čo by som v produkcii urobil inak

Keďže toto je "test task", použil som JSON súbory, ale v reálnom svete by som to staval inak:
1. **Databáza namiesto JSONov:** Určite Postgres s poriadnymi check-constraints na dátumy.
2. **Validácia hneď pri vstupe:** Nedovolil by som, aby sa do systému dostala rezervácia s `to < from`. To by malo končiť chybou 400 už pri uložení.
3. **Caching:** Výpočet dostupnosti na 30 dní pre 1000 áut vie byť drahý. Použil by som Redis na kešovanie výsledkov, ktoré by sa invalidovali len pri zmene rezervácie.
4. **Monitoring:** Sledoval by som počet `DATA` konfliktov. Ak ich pribúda, znamená to, že nám niekto sype do systému odpad.
