package de.mika.promptwerk;

import com.google.mlkit.nl.translate.*;
import com.google.mlkit.common.model.DownloadConditions;
import java.util.ArrayList;

/** Only ML Kit's on-device translator receives the text; network is for model download. */
class LocalTranslation implements MainActivity.TranslationService {
    private Translator client;
    private boolean closed;
    public void translate(String text, boolean toEnglish, MainActivity.TranslationCallback callback) {
        TranslatorOptions options = new TranslatorOptions.Builder()
            .setSourceLanguage(toEnglish ? TranslateLanguage.GERMAN : TranslateLanguage.ENGLISH)
            .setTargetLanguage(toEnglish ? TranslateLanguage.ENGLISH : TranslateLanguage.GERMAN).build();
        client = Translation.getClient(options);
        ArrayList<String> chunks = chunks(text);
        // ML Kit checks already downloaded models locally. Missing models download only on Wi-Fi.
        client.downloadModelIfNeeded(new DownloadConditions.Builder().requireWifi().build())
            .addOnSuccessListener(v -> next(chunks, 0, new StringBuilder(), callback))
            .addOnFailureListener(e -> { if (!closed) callback.failed(e); });
    }
    private void next(ArrayList<String> chunks, int index, StringBuilder result, MainActivity.TranslationCallback callback) {
        if (closed) return;
        if (index == chunks.size()) { callback.done(result.toString()); return; }
        String chunk = chunks.get(index);
        if (chunk.trim().isEmpty()) { result.append(chunk); next(chunks, index + 1, result, callback); return; }
        client.translate(chunk).addOnSuccessListener(translated -> {
            if (closed) return;
            if (translated == null || translated.trim().isEmpty()) { callback.failed(new java.io.IOException("Leeres Übersetzungsergebnis")); return; }
            result.append(translated);
            next(chunks, index + 1, result, callback);
        }).addOnFailureListener(e -> { if (!closed) callback.failed(e); });
    }
    // Keep paragraph separators outside translation and never split a UTF-16 surrogate pair.
    static ArrayList<String> chunks(String text) {
        ArrayList<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + 4000, text.length());
            int newline = text.indexOf('\n', start);
            if (newline >= start && newline < end) end = newline;
            else if (end < text.length()) {
                int space = text.lastIndexOf(' ', end - 1);
                if (space > start) end = space;
                else if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
            }
            if (end > start) result.add(text.substring(start, end));
            int next = end;
            while (next < text.length() && Character.isWhitespace(text.charAt(next))) next++;
            if (next > end) result.add(text.substring(end, next));
            start = next;
        }
        return result;
    }
    public void close() { closed = true; if (client != null) client.close(); }
}
