<!--
SPDX-FileCopyrightText: 2021 Markus Murto

SPDX-License-Identifier: CC0-1.0
-->

# KivaKeli

[![REUSE status](https://api.reuse.software/badge/github.com/murtoM/KivaKeli)](https://api.reuse.software/info/github.com/murtoM/KivaKeli)

A messy hackjob of java project done as a exercise for Turku University of Applied Sciences Java introductory course.

The class is supposed to print weather information.

## Compiling
```
# running under src-directory
$ javac -cp lib/json-simple.jar KivaKeli.java
```

## Running
```
Käyttö: KivaKeli [komentorivioptiot...]

-h		tulostetaan tämä apuviesti
-l <sijainti>	aseta sijainti, esimerkiksi kaupungin nimi
-u <symboli>	aseta haluttu lämpötilan symboli, esimerkisi C tai F
```

## Output
```
$ java -cp lib/json-simple.jar: KivaKeli
Sää laitteen sijainnissa

Lämpötila: 281.7 K
Keli: hajanaisia pilviä

$ java -cp lib/json-simple.jar: KivaKeli -u c
Sää laitteen sijainnissa

Lämpötila: 8.55 °C
Keli: hajanaisia pilviä

$ java -cp lib/json-simple.jar: KivaKeli -u c -l "Turku"
Sää Turku

Lämpötila: 8.52 °C
Keli: hajanaisia pilviä
```

## License

The main programme code is free software and licensed under the Apache 2.0 license. The documentation is licensed under CC0. This git repository is REUSE compliant and the specific license can be found in the header of each file (except the files under doc/).
