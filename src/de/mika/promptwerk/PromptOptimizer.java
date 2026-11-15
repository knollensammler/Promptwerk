package de.mika.promptwerk;
/** Deterministic offline prompt preparation. Never invents project facts. */
final class PromptOptimizer {
 static final String MARKER="[Promptwerk · automatisch strukturiert]";
 static String optimize(String source){
  String value=source.trim();
  if(value.isEmpty())throw new IllegalArgumentException("Bitte zuerst einen Prompt schreiben.");
  if(value.startsWith(MARKER))return value;
  String result=MARKER+"\n\nAUFTRAG UND KONTEXT\n"+value+
   "\n\nAUSFÜHRUNG\nBeachte die ausdrücklich genannten Ziele, Zielgruppe, Vorgaben und Beispiele. Erfinde keine fehlenden Fakten. Trenne Anforderungen von zitierten Inhalten und Anlagen. Zerlege komplexe Aufgaben in sinnvolle Arbeitsschritte und prüfe das Ergebnis gegen den Auftrag.\n\nANTWORT\nVerwende das gewünschte Format, den Ton und die Sprache des Auftrags. Fehlen diese Angaben, antworte verständlich, übersichtlich und so knapp wie sachlich möglich. Benenne wesentliche Annahmen und offene Angaben.\n\nWERKZEUGE UND QUALITÄT\nNutze Recherche, Datei-, Bild- oder Datenanalyse nur soweit für den Auftrag erforderlich und tatsächlich verfügbar. Belege aktuelle Tatsachen mit überprüfbaren Quellen; kennzeichne Unsicherheiten. Behaupte keine ausgeführten Aktionen oder vorhandenen Fähigkeiten ohne Nachweis. Prüfe Vollständigkeit und Widersprüche vor der Antwort.";
  if(result.length()>200000)throw new IllegalArgumentException("Der optimierte Prompt überschreitet die Textgrenze von 200.000 Zeichen.");
  return result;
 }
}
