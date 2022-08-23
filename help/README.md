# Points Detector Help - PL

![ICON](img/icon.png)

## Instalacja

Aby móc używać pluginu Points detector należy pobrać plik Points_Detector.jar i umieścić go w katalogu Plugins. Po ponownym uruchomieniu programu powinien być on widoczny w menu rozwijanym w Imagej -> Plugins.

## Uruchomienie pluginu

W pierwszej kolejności należy wczytać plik w postaci pojedynczego obrazka albo stacku. Plugin uruchamia się domyślnie dla aktywnego okna.

![MENU](img/Menu.jpg)

Program może działać w dwóch trybach: automatycznym i manualnym. W trybie automatycznym podajemy w polu Parameters ustalone wcześniej parametry. Po kliknięciu OK program automatycznie wyszukuje punkty i otwiera nowe okno z obrazem wynikowym. W trybie manualnym parametry należy ustalić na bieżąco.

## Tryb manualny

Po wybraniu opcji Manual mode i kliknięciu OK przechodzimy do trybu manualnego ustalania parametrów. W tym celu otwiera się seria okien.

![MANUAL](img/Manual_mode_windows.jpg)

Krok kolejny to zaznaczenie w oknie Noise obszarów, które zawierają wyłącznie szum/tło. W tym celu można użyć dowolnego narządzia wyboru z ImageJ. Aby dodać kolejne obszary należy trzymać wciśnięty przycisk SHIFT. 

TIP: Ważne jest aby zaznaczyć obszary tła o jak najbardziej zróżnicowanej jasności.

Wybrane obszary zostają zaznaczone na wykresie w oknie Plot. Na poniższym wykresie poszczególne grupy punktów odpowiadają obszarom oznaczonym na zdjęciu. Warto zwrócić uwagę na różne poziomy jasności zaznaczonych obszarów

![NOISE](img/Noise selection.jpg)
