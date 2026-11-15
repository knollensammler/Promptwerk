# Promptwerk für Android

Eine lokale Werkstatt für Prompts, Textbausteine, Projektideen und Entwürfe.
Die Android-Version 3.3 wird als Open-Source-App zur freien Weiterentwicklung unter MIT bereitgestellt.

## Download

Die Original-APK 3.3 (versionCode 19) wird im GitHub-Release dieses Repositorys bereitgestellt.
Android ab 8.0 (API 26). Vor einem Update eine externe Sicherung anlegen und die bestehende App nicht deinstallieren.

SHA-256 der originalsignierten APK:
`00b97e3642d3dae6f59271aebb2b5a45d1e38ae25f3e735e7559e92e914913e9`

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

Die Produktvariante `production` verwendet die ursprünglichen GsG-Endpunkte. Der optionale persönliche Google-Drive-Sync benötigt diese Gegenstelle, eigenes Nutzer-OAuth und ein für den aktuellen Google-Testbetrieb zugelassenes Konto. Das öffentliche Repository enthält die Android-App, keinen öffentlich betriebenen Backenddienst und keine Garantie für dessen dauerhaften Zugang. Für unabhängige Forks eigene Backend-/OAuth-Infrastruktur einrichten und `DRIVE_BASE` ändern. Lokale Nutzung und Android-Sicherungen bleiben möglich.

## Prüfstand des Originals

91 Android-/Robolectric-Tests, Releasebuild und Lint wurden vor Veröffentlichung geprüft. Ein Build-/Unit-Testnachweis ersetzt keinen physischen Android-Gerätetest. Installation, Energiespar- und Hintergrundverhalten können je Gerät abweichen.

## Mitwirken

Fehlerberichte bitte mit Android-Version, erwarteter und tatsächlicher Wirkung sowie reproduzierbaren Schritten beschreiben. Keine privaten Prompts, Sicherungen, Schlüssel oder Tokens in Issues hochladen. Weiterentwicklungen sind willkommen; eigene Distributionen bitte klar als solche kennzeichnen.

Siehe [LICENSE](LICENSE) und [NOTICE.md](NOTICE.md).
