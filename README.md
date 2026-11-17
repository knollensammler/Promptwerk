# Promptwerk für Android

Eine lokale Werkstatt für Prompts, Textbausteine, Projektideen und Entwürfe.
Die Android-Version 3.4 wird als Open-Source-App zur freien Weiterentwicklung unter MIT bereitgestellt.

## Download

**Sync-Reparatur 3.4 (versionCode 20):** [Originalsignierte APK herunterladen](https://gsgev.org/apps/Promptwerk-3.4.apk).

Version 3.3 darf wegen eines Fehlers bei gleichzeitiger Synchronisation nicht mehr für Drive-Sync verwendet werden. Der Server weist das alte Syncprotokoll zum Schutz der Daten zurück. Bitte auf allen verbundenen Android-Geräten aktualisieren und die Webversion neu laden.

Die App über die vorhandene Installation aktualisieren.
Android ab 8.0 (API 26). Vor einem Update eine externe Sicherung anlegen und die bestehende App nicht deinstallieren.

SHA-256 der originalsignierten APK:
`d4cb76e8ccfba0473b6384cc07c4683c9cfe6e2e4d1cb9da7917124e9921070b`

Originalzertifikat SHA-256:
`8070096d1f615edc751a794e661929d2fc302e588dc067bcb2db61fba052038d`

## Entwickeln

Benötigt: JDK 21, Android SDK 35 mit Build Tools 35.0.0 und Gradle 8.13.
`local.properties` mit dem eigenen `sdk.dir` anlegen oder `ANDROID_HOME` setzen.

```sh
gradle testProductionDebugUnitTest
gradle assembleProductionDebug
gradle lintProductionRelease
```

Der Debugbuild benötigt den privaten Originalsignierer nicht. Er kann wegen der anderen Signatur nicht über die Original-App installiert werden. Für getrennte Entwicklungsinstallationen `applicationId` in der eigenen Kopie ändern; eigene Daten zuvor sichern.

Die Releasekonfiguration verweist absichtlich auf einen nicht mitgelieferten `signing.p12` und die Umgebungsvariable `PROMPTWERK_STORE_PASSWORD`. Eigene Releases benötigen einen eigenen Signierer. Niemals private Schlüssel oder Kennwörter in Git veröffentlichen. Es wird kein Ersatz für den Originalsignierer erzeugt.

Die Produktvariante `production` verwendet die ursprünglichen GsG-Endpunkte. Der optionale persönliche Google-Drive-Sync benötigt diese Gegenstelle, eigenes Nutzer-OAuth und die bewusste Verbindung des persönlichen Google-Kontos. Die Google-OAuth-App ist seit dem 15.11.2026 für externe Nutzer im Produktionsstatus veröffentlicht; eine Aufnahme als Testnutzer ist nicht mehr erforderlich. Das öffentliche Repository enthält die Android-App, keinen öffentlich betriebenen Backenddienst und keine Garantie für dessen dauerhaften Zugang. Für unabhängige Forks eigene Backend-/OAuth-Infrastruktur einrichten und `DRIVE_BASE` ändern. Lokale Nutzung und Android-Sicherungen bleiben möglich.

## Prüfstand des Originals

101 Android-/Robolectric-Tests, Releasebuild und Lint wurden vor Veröffentlichung geprüft. Ein Build-/Unit-Testnachweis ersetzt keinen physischen Android-Gerätetest. Installation, Energiespar- und Hintergrundverhalten können je Gerät abweichen.

## Mitwirken

Fehlerberichte bitte mit Android-Version, erwarteter und tatsächlicher Wirkung sowie reproduzierbaren Schritten beschreiben. Keine privaten Prompts, Sicherungen, Schlüssel oder Tokens in Issues hochladen. Weiterentwicklungen sind willkommen; eigene Distributionen bitte klar als solche kennzeichnen.

Siehe [LICENSE](LICENSE) und [NOTICE.md](NOTICE.md).

## Sync-Reparatur 3.4

- Fehlende Einträge in einem lokalen Zwischenstand lösen keine Drive-Löschungen mehr aus. Nur eine ausdrückliche Löschbestätigung stellt einen Löschauftrag in die Warteschlange.
- Hintergrundabrufe und Änderungen im geöffneten Editor behalten die tatsächlich angezeigte Ausgangsrevision. Gleichzeitige Änderungen bleiben als Konfliktkopien erhalten.
- Die Migration alter Warteschlangen nimmt unbeabsichtigte Löschvormerkungen zurück. Bestehende lokale Inhalte und Anhänge bleiben erhalten.
- Die geöffnete App gleicht alle 30 Sekunden ab; Android kann Aufgaben im Hintergrund weiterhin verzögern.
- „Prompt übergeben“ sendet Text gezielt über Android an die ChatGPT-App und hält ihn zusätzlich in der Zwischenablage bereit. Der ursprüngliche Entwurf bleibt wiederherstellbar. Ob die installierte ChatGPT-Version den Text direkt in ihrem Eingabefeld übernimmt, muss auf einem echten Gerät geprüft werden; ein erzwungenes Einfügen oder automatisches Absenden wird nicht versprochen.

Die Webversion 1.3 nutzt dasselbe geschützte Protokoll. Lokale Originale bleiben bei der Übernahme in Drive erhalten. Vor dem ersten Schreiben mit dem neuen Protokoll legt der Server eine separate Sicherung des vorhandenen Manifests im persönlichen Drive an. Neue Löschmarken bewahren den gelöschten Text für eine mögliche Wiederherstellung. Vorhandene Daten werden durch das Update nicht automatisch aus älteren Drive-Versionen zurückgeschrieben.
