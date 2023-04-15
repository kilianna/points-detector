# Points Detector Help - PL

## Instalacja

Aby móc używać zestawu narzędzi do przetwarzania obrazów z mikroskopu w celu wyszukania śladów po promieniowaniu należy pobrać plik Points_Detector.jar i umieścić go w katalogu Plugins (podkatalog katalogu ImageJ lub Figi.app). Po ponownym uruchomieniu programu powinien być on widoczny w menu rozwijanym w ImageJ: **Plugins -> FNDT IFJ**.

W skład paczki FNDT IFJ wchodzą następujące narzędzia:
1. _Reset color balance_ - plugin przesuwający zakres wyświetlania (display range) tak, aby 0 odpowiadało kolorowi czarnemu.
2. _Slices Correction_ - plugin służący wprowadzaniu do stosu obrazów poprawek na wypalanie i głębokość.
3. _Slice Dev_ - plugin pozwalający na wprowadzenie dodatkowych poprawek do stosu obrazów.
4. _Points detector_ - główna część paczki, która służy do analizy obrazu i odseparowania sygnału of szumu.

## Uruchomienie pluginu

W pierwszej kolejności należy wczytać plik w postaci pojedynczego obrazka albo stacku. Plugin uruchamia się domyślnie dla aktywnego okna. 

![MENU](img/Menu.jpg)

Program może działać w dwóch trybach: automatycznym i manualnym. W trybie automatycznym podajemy w polu Parameters ustalone wcześniej parametry. Po kliknięciu OK program automatycznie wyszukuje punkty i otwiera nowe okno z obrazem wynikowym. W trybie manualnym parametry należy ustalić na bieżąco.

## Tryb manualny

Po wybraniu opcji Manual mode i kliknięciu OK przechodzimy do trybu manualnego ustalania parametrów. Otwiera się seria okien. W pierwszej kolejności w Menu ustalamy wielkość okna skanującego (Scaning window size). 

TIP: Im gęściej rozmieszczone punkty tym rozmiar okna powinien być mniejszy.

Następnie podajemy przewidywany rozmiar punktów.

TIP: Można przybliżyć obraz, aby orientacyjnie ocenić wielkość punktów.

![MANUAL](img/Manual_mode_windows.jpg)

Krok kolejny to zaznaczenie w oknie Noise obszarów, które zawierają wyłącznie szum/tło. W tym celu można użyć dowolnego narządzia wyboru z ImageJ. Aby dodać kolejne obszary należy trzymać wciśnięty przycisk SHIFT. 

TIP: Ważne jest aby zaznaczyć obszary tła o jak najbardziej zróżnicowanej jasności.

Wybrane obszary zostają zaznaczone na wykresie w oknie Plot. Na poniższym wykresie poszczególne grupy punktów odpowiadają obszarom oznaczonym na zdjęciu. Warto zwrócić uwagę na różne poziomy jasności zaznaczonych obszarów

![NOISE](img/Noise_selection.jpg)

W kolejnym etapie w oknie Points za pomoca narzędzia wyboru punktów z ImageJ zaznaczamy punkty. Oznaczone punkty pojawiają się na wykresie w oknie Plot.

![NOISE](img/Points_selection.jpg)

Aby zaznaczać kolejne punkty należy trzymać wciśnięty przycisk SHIFT. 

TIP: Ważne jest oznaczenie szczególnie tych punktów, które w najmniejszym stopniu odróżniają się od otaczającego je tła. W tym celu warto powiększyć sobie wybrane obszary obrazu.

![NOISE](img/Points_selection2.jpg)

Po zaznaczeniu obszarów szumu i punktów możemy dopasować ręcznie prostą odcinającą szum od punktów. (Można albo po prostu przesunąć istniejącą już na wykresie prostą, albo narysować właśną za pomocą narzędzia rysowania prostej z ImageJ.) Parametry prostej pokazują się na bieżąco w oknie Menu, natomiast obraz wynikowy można podejrzeć w oknie Preview. Przesuwając pasek na dole okna można podejrzeć porównanie obrazu wejściowego i wynikowego.

![NOISE](img/Preview.jpg)

## Opcje

All slices - zaznaczenie tej opcji powoduje wykrywanie punktów osobno na każdym jednym obrazie w stacku.

Cut background - powoduje oznaczenie po przeskanowaniu obrazu wszystkich punktów określonych jako tło na czarno.

Cut background - powoduje oznaczenie po przeskanowaniu obrazu wszystkich zidentyfikowanych punktów na biało.

Surowy wynik jest w odcieniach szarości. Użycie powyższych opcji działa analogicznie jak funkcja Treshold.
