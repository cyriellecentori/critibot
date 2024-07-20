# Critibot
Le bot des Critiqueurs de la Fondation SCP francophone, utile pour gérer les différents écrits à critiquer.

Cette version du critbot a été entièrement reprogrammée en Rust ici : https://github.com/Fondation-SCP/fondabots. Cela a notamment l’avantage de séparer le code générique réutilisable du code spécifique lié au forum des critiques, permettant de réutiliser le concept du bot pour d’autres utilisations (fils O5, surveillance de pages, relectures de traduction…).

## Dépendances
* [Java Discord API 5.X](https://github.com/DV8FromTheWorld/JDA)
* [GSON](https://github.com/google/gson) — Bibliothèque JSON
* [JDOM](http://jdom.org) — Bibliothèque XML
* [Rome](https://github.com/rometools/rome) — Bibliothèque RSS
