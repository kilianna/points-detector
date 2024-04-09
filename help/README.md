# Background Remover Help - PL

## Instalacja i informacje wstępne

Aby móc używać zestawu narzędzi do przetwarzania obrazów z mikroskopu w celu wyszukania śladów po promieniowaniu należy pobrać plik BackgroundRemover.jar i umieścić go w katalogu Plugins (podkatalog katalogu ImageJ lub Figi.app). Po ponownym uruchomieniu programu powinien być on widoczny w menu rozwijanym w ImageJ: **Plugins -> FNTD IFJ**.

W skład pakietu **FNTD IFJ** wchodzą następujące narzędzia:
1. _Background Remover_ - główna część paczki, która służy do analizy obrazu i odseparowania sygnału of szumu.
2. _Slices Correction_ - plugin służący wprowadzaniu do stosu obrazów poprawek na wypalanie i głębokość.
3. _Slice Multiply_ - plugin pozwalający na wprowadzenie dodatkowych poprawek do stosu obrazów.
4. _Reset color balance_ - plugin przesuwający zakres wyświetlania (display range) tak, aby 0 odpowiadało kolorowi czarnemu.
5. _Stack to CSV_ - plugin umożliwiający przekonwertowanie stosu obrazów na odpowiadający im zestaw plików CSV.
6. _Points generator_ - wtyczka generująca losowe punkty na obrazie - według parametrów zadanych przez użytkownika.


W pierwszej kolejności należy wczytać plik w postaci pojedynczego obrazka albo stacku. Obraz (lub stos) powinien być 16-bitowy w odcieniach szarości. Plugin uruchamia się domyślnie dla aktywnego okna. 

Plugin umieszcza adnotacje odnośnie wykonanych na pliku operacji. Można je prześledzić wchodząc w **Image -> Show Info...**.

![Reset](img/adnotacje.JPG)

## Reset color balance

Dla wybranego obrazka (lub stosu) zakres wyświetlania (Display range) zostaje przesunięty tak, aby wartość piksela równa 0 odpowiadała kolorowi czarnemu. Po wykonaniu wyświetlone zostaje okno z informacją z o przesunięciu.

![Reset](img/log_reset.jpg)

W podanym powyżej przykładzie Display range został przesunięty z zakresu 1746.0 - 15951.0, gdzie 1746.0 odpowiadało najciemniejszemu pikslelowi na obrazie (kolorowi czarnemu), a 15951.0 odpowiadało pikselowi najjasniejszemu (kolorowi białemu) do zakresu 0 - 14205.0. Wartość każdego piksela w każdym z obrazów w stosie została obniżona o 1746.0 i aktualnie piksele w kolorze czarnym mają wartość 0, natomiast piksele w kolorze białym mają wartość 14205.0.

## Slices Correction

Część pluginu umożliwiająca wprowadzenie poprawek na wypalanie oraz głębokość. 

Współczynnik korekcyjny na wypalanie dla konkretnego obrazu w stosie obliczany jest według wyznaczonego empirycznie wzoru:

$$
  time\\\_corr = 0.765*e^{(\frac{-t}{443.85})}+0.235
$$

$$
  t = t_{0}+ (n-1)*step
$$

gdzie:<br>
$t_{0}$ - czas początkowy [s];<br>
$n$ - numer obrazu;<br>
$step$ - czas odczytu pojedynczego obrazu [s].

Poszczególne wartości pikseli na obrazie są następnie dzielone przez obliczony według powyższego wzoru współczynnik korekcyjny.

Analogicznie wprowadzane są poprawki na głębokość. Tutaj także posługujemy się wzorem wyznaczonym empirycznie:

$$
depth\\\_corr = e^{-0.015 * d}
$$

$$
  d = d_{0}+ (n-1)*step
$$

gdzie:<br>
$d_{0}$ - głębokość startowa [&mu;m];<br>
$n$ - numer obrazu;<br>
$step$ - krok o jaki zwiększana jest głębokość na jakich dokonywany jest odczyt [&mu;m].

Podobnie jak wcześniej - wartości poszczególnych pikseli na obrazie dzielone są przez wartość współczynnika korekcyjnego na głębokość wyznaczonego dla danego obrazu.

![Slices correction](img/Slices_corr.JPG)

Jako wartości wejściowe należy podać:<br>
**First depth** - głębokość na jakiej dokonywany jest odczyt (0 oznacza powierzchnię) [&mu;m];<br>
**Slice thick** - krok o jaki zwiększa się głębokość [&mu;m];<br>
**Time 0** - czas początkowy (w przypadku, gdy dany kryształ był już uprzednio odczytywany) [s];<br>
**Time** - czas odczytu każdego z obrazów [s].

Program wprowadzi obie poprawki jednocześnie na oryginalnym obrazie.

![out of range error](img/Values_out_of_range.PNG)

Jeżeli w skutek zastosowanych przeliczeń wartości wynikowe pikseli przekroczą zakres przewidziany dla obrazu 16-bitowego (tj. 65535) wyświetlony zostanie odpowiedni komunikat. Piksele, których wartość po przeliczeniu przekroczyła wartość maksymalną zostają domyślnie ustawione na 65535 (bez względu na ich rzeczywistą wyliczoną wartość). W tym przypadku zależności wartości pomiędzy poszczególnymi pikselami nie zostaja zachowane. 

## Slice Multiply

![Slice dev](img/slices_div.JPG)

Część pluginu umożliwiajaca przemnożenie wartości pikseli na poszczególnych obrazach przez podane przez użytkownika wartości. W polu tekstowym należy podać kolejno odpowiednie mnożniki dla poszczególnych obrazów (oddzialając je przecinkiem). W przypadku, gdy dany obraz w stosie ma pozostać bez zmian należy jako odpowiadający mu mnożnik podać wartość 1. Liczba podanych wartości musi się zgadzać z liczbą obrazów w stosie, w przeciwnym razie program wyrzuci informację o niezgodności (poda liczbę współczynników wprowadzonych przez użytkownika oraz liczbę oczekiwanych współczynników).

![Slice dev](img/log_corr.JPG)

Jeżeli w skutek zastosowanych przeliczeń wartości wynikowe pikseli przekroczą zakres przewidziany dla obrazu 16-bitowego (tj. 65535) wyświetlony zostanie odpowiedni komunikat. Piksele, których wartość po przeliczeniu przekroczyła wartość maksymalną zostają domyślnie ustawione na 65535 (bez względu na ich rzeczywistą wyliczoną wartość). W tym przypadku zależności wartości pomiędzy poszczególnymi pikselami nie zostaja zachowane. 

![Display range too high error](img/Display_range_too_high.PNG)

## Stack to CSV
Moduł umożliwiający przekonwertowanie, a następnie wyeksportowanie do pliku stosu obrazów. Uruchomienie tego modułu spowoduje otwarcie okna, w którym należy wybrać nazwę oraz docelową lokalizację eksportowanych plików. Po zaakceptowaniu plugin utowrzy pliki w formacie CSV w lokalizacji podanej przez użytkownika. Nazwy poszczególnych pliów składają się z nazwy podanej przez użytkownika oraz numeru obrazu w stosie (np. nazwa_3.CSV odpowiada trzeciemu z kolei obrazowi w stosie). 

## Background Remover
Główna część pluginu, której zadaniem jest umożliwienie oddzielenia sygnału od szumu w analizowanych obrazach.

![MENU](img/MainWindow.PNG)

Program może działać w dwóch trybach: automatycznym i manualnym. W trybie automatycznym podajemy w polu Parameters ustalone wcześniej parametry. Dodatkowo można spersonalizować sposób wyświetlania wyniku: wyznaczonych punktów i sygnału uznanego za tło. Po kliknięciu OK program automatycznie wyszukuje punkty i otwiera nowe okno z obrazem wynikowym. 

Dostępne opcje wyświetlenia punktów:
- **White** - punkty wyświetlane na biało;
- **Black**- punkty wyświetlane na czarno;
- **Orginal** - punkty wyświetlane takie same jak na obrazie oryginalnym;
- **Degree of matching** - jasność pikseli odpowiada różnicy między wyliczoną przez program "jasnością punktu", a "jasnością tła" (im jaśniejszy punkt, tym bardziej "wystaje on ponad tło");
- **Net signal (average)** - wartość pikseli odpowiada podstawowej wartości pikseli pomniejszonej o tło wyliczone jako średnia arytmetyczna pikseli wokół danego punktu;
- **Net signal (mode)** - wartość pikseli odpowiada podstawowej wartości pikseli pomniejszonej o tło wyliczone jako wartość modalna pikseli wokół danego punktu;
- **Net signal (median)** - wartość pikseli odpowiada podstawowej wartości pikseli pomniejszonej o tło wyliczone jako mediana pikseli wokół danego punktu;


Dostępne opcje wyświetlenia tła:
- **White** - tło wyświetlane na biało;
- **Black** - tło wyświetlane na czarno;
- **Orginal** - tło wyświetlane takie same jak na obrazie oryginalnym;
- **Degree of matching** - jasność pikseli odpowiada różnicy między wyliczoną przez program "jasnością punktu", a "jasnością tła";

Dodatkowe opcje

### dupaScope

- plugin wykonuje operacje nie tylko na wybranym obrazie, ale na wszystkich obrazach w stosie w aktywnym oknie;
- **Keep orginal slices** - jako wynik plugin tworzy stos obrazów, gdzie wynikowe obrazy przeplatane są odpowiadającymi im oryginalnymi obrazami (funkcja przydatna przy porównywaniu obrazów wynikowych i oryginalnych);
- **Manual mode** - przejście do trybu manualnego.

## Tryb manualny

W trybie manualnym parametry możemy ustalać na bieżąco. Po wybraniu opcji Manual mode i kliknięciu OK przechodzimy do trybu manualnego ustalania parametrów. Otwiera się seria okien. W pierwszej kolejności w Menu ustalamy wielkość okna skanującego (Scaning window size). 

TIP: Im gęściej rozmieszczone punkty tym rozmiar okna powinien być mniejszy.

Następnie podajemy przewidywany rozmiar punktów.

TIP: Można przybliżyć obraz, aby orientacyjnie ocenić wielkość punktów.

![MANUAL](img/Manual_mode_windows.jpg)

Krok kolejny to zaznaczenie w oknie Noise obszarów, które zawierają wyłącznie szum/tło. W tym celu można użyć dowolnego narządzia wyboru z ImageJ. Aby dodać kolejne obszary należy trzymać wciśnięty przycisk SHIFT. 

TIP: Ważne jest aby zaznaczyć obszary tła o jak najbardziej zróżnicowanej jasności.

Wybrane obszary zostają zaznaczone na wykresie w oknie Plot. Na poniższym wykresie poszczególne grupy punktów odpowiadają obszarom oznaczonym na zdjęciu. Warto zwrócić uwagę na różne poziomy jasności zaznaczonych obszarów

![NOISE](img/Noise_selection.jpg)

W kolejnym etapie w oknie Points za pomoca narzędzia wyboru punktów z ImageJ zaznaczamy punkty. Oznaczone punkty pojawiają się na wykresie w oknie Plot.

![POINTS](img/Points_selection.jpg)

Aby zaznaczać kolejne punkty należy trzymać wciśnięty przycisk SHIFT. 

TIP: Ważne jest oznaczenie szczególnie tych punktów, które w najmniejszym stopniu odróżniają się od otaczającego je tła. W tym celu warto powiększyć sobie wybrane obszary obrazu.

![NOISE](img/Points_selection2.jpg)

Po zaznaczeniu obszarów szumu i punktów możemy dopasować ręcznie prostą odcinającą szum od punktów. (Można albo po prostu przesunąć istniejącą już na wykresie prostą, albo narysować właśną za pomocą narzędzia rysowania prostej z ImageJ.) Parametry prostej pokazują się na bieżąco w oknie Menu, natomiast obraz wynikowy można podejrzeć w oknie Preview. Przesuwając pasek na dole okna można podejrzeć porównanie obrazu wejściowego i wynikowego.

![NOISE](img/Preview.jpg)
