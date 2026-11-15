package de.mika.promptwerk;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.util.ArrayList;

/** Edit an existing library entry independently of the working draft. */
final class EntryWindow {
    final MainActivity a;
    final MainActivity.Item item;
    final boolean fresh;
    final ArrayList<MainActivity.Att> files;
    final EditText title, text;
    final Spinner kind;
    final Spinner storage;
    final Button mode, fileButton;
    final Dialog dialog;
    final KeyListener titleKeys, textKeys;
    final int titleInputType, textInputType;
    boolean reading;
    boolean restoringText;
    String lastText;
    ArrayList<MainActivity.Att> lastFiles;

    EntryWindow(MainActivity activity, MainActivity.Item original) {
        a = activity; item = original; fresh=!a.items.contains(item); files = new ArrayList<>(item.files);
        LinearLayout body = a.col();
        body.setPadding(a.dp(12), a.dp(8), a.dp(12), a.dp(8));
        body.setBackgroundColor(a.BG);
        body.addView(a.label(item.type.equals("block")?"BAUSTEIN · eigener Editor":"PROMPT · eigener Editor", 18, a.INK));
        title = a.input("Titel");
        title.setSingleLine(true);
        title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(150)});
        title.setText(item.title);
        body.addView(title, new LinearLayout.LayoutParams(-1, a.dp(48)));
        kind = new Spinner(a);
        kind.setAdapter(new ArrayAdapter<String>(a, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Prompt", "Textbaustein"}));
        kind.setSelection(item.type.equals("block") ? 1 : 0);
        body.addView(kind, new LinearLayout.LayoutParams(-1, a.dp(48)));
        storage=new Spinner(a);storage.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,new String[]{"Nur auf diesem Gerät","Mit meinem Google Drive synchronisieren"}));storage.setSelection(!DriveSync.autoOwner(a).isEmpty()?1:item.cloud?1:0);storage.setEnabled(DriveSync.autoOwner(a).isEmpty());body.addView(storage,new LinearLayout.LayoutParams(-1,a.dp(48)));
        LinearLayout tools = a.line();
        mode = a.button("Bearbeiten", () -> {
            setReading(!reading);
            if (!reading) focusText();
        });
        a.equal(tools, mode);
        a.equal(tools, a.button("Normalansicht", () -> requestClose()));
        Button more = a.button("•••", () -> actionsMenu());
        more.setContentDescription("Weitere Aktionen für diesen Text");
        tools.addView(more, new LinearLayout.LayoutParams(a.dp(48), a.dp(48)));
        body.addView(tools);
        text = a.input("Text deines Eintrags");
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        text.setGravity(Gravity.TOP);
        text.setHorizontallyScrolling(false);
        text.setMinLines(1);
        text.setMaxLines(Integer.MAX_VALUE);
        text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200000)});
        text.setText(item.text);
        lastText = item.text; lastFiles = new ArrayList<>(files);
        text.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!restoringText) { lastText = s.toString(); lastFiles = new ArrayList<>(files); }
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable value) {}
        });
        body.addView(text, new LinearLayout.LayoutParams(-1, 0, 1));
        titleKeys = title.getKeyListener(); textKeys = text.getKeyListener();
        titleInputType = title.getInputType(); textInputType = text.getInputType();
        fileButton = a.button("📎 Anhänge · " + files.size(), () -> a.attachments(files,
            () -> fileButtonLabel()));
        body.addView(fileButton, new LinearLayout.LayoutParams(-1, a.dp(48)));
        LinearLayout actions = a.line();
        a.equal(actions, a.button("Kopieren", () -> a.copyText(title.getText().toString(), text.getText().toString())));
        a.equal(actions, a.button("Teilen", () -> a.shareContent(text.getText().toString(), new ArrayList<>(files))));
        Button save = a.button("Speichern", () -> save());
        save.setTextColor(a.BG); save.setBackground(a.shape(a.ACCENT));
        a.equal(actions, save); body.addView(actions);
        dialog = new Dialog(a, android.R.style.Theme_Material_NoActionBar) {
            @Override public void cancel() { requestClose(); }
        };
        dialog.setContentView(body);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((d, key, event) -> {
            if (key != KeyEvent.KEYCODE_BACK) return false;
            if (event.getAction() == KeyEvent.ACTION_UP) requestClose();
            return true;
        });
        dialog.setOnDismissListener(d -> {
            if (a.entryWindow == this) a.entryWindow = null;
        });
        dialog.show();
        Window window = dialog.getWindow();
        window.setLayout(-1, -1);
        window.setStatusBarColor(a.BG); window.setNavigationBarColor(a.BG);
        KeyboardLayout.install(window, body);
        setReading(!fresh);
        if(fresh) EditorMode.showKeyboard(title, dialog.getWindow());
    }

    void fileButtonLabel() { fileButton.setText("📎 Anhänge · " + files.size()); }

    void actionsMenu() {
        new AlertDialog.Builder(a).setTitle("Aktionen für diesen Text")
            .setItems(new String[]{"Tokensparer", "Anhänge verwalten", "Text leeren", "Letzte Textänderung rückgängig", "Automatisch optimieren", "Prompt ↔ Baustein", "Spracheingabe"}, (d, w) -> {
                if(w==6){setReading(false);a.speechInput.start(text);}
                if(w==4)a.optimize(text.getText().toString(),new ArrayList<>(files));
                if(w==5){kind.setSelection(type().equals("block")?0:1);a.toast("Typ geändert · Speichern übernimmt die Änderung");}
                if (w == 0) tokensparer();
                if (w == 1) a.attachments(files, () -> fileButtonLabel());
                if (w == 2) { if (text.length() > 0) text.setText(""); }
                if (w == 3) undoText();
            }).setNegativeButton("Schließen", null).show();
    }

    void undoText() {
        if (a.busy) return;
        String previous = lastText; ArrayList<MainActivity.Att> previousFiles = lastFiles;
        lastText = text.getText().toString(); lastFiles = new ArrayList<>(files);
        restoringText = true; text.setText(previous); text.setSelection(text.length());
        files.clear(); files.addAll(previousFiles); restoringText = false; fileButtonLabel();
    }

    void tokensparer() {
        new AlertDialog.Builder(a).setTitle("Tokensparer für diesen Text")
            .setItems(new String[]{"Deutsch → Englisch als neuen Eintrag speichern", "Englisch → Deutsch in diesem Fenster"}, (d, w) -> {
                String value = text.getText().toString();
                if (w == 0) a.translateText(value, true, new ArrayList<>(files));
                else a.translateText(value, false, new ArrayList<>(files), translated -> showTranslation(translated));
            }).setNegativeButton("Abbrechen", null).show();
    }

    void showTranslation(String translated) {
        if (!dialog.isShowing()) return;
        TextView preview = a.label(translated, 16, a.INK);
        preview.setPadding(a.dp(16), a.dp(12), a.dp(16), a.dp(12));
        preview.setTextIsSelectable(true);
        ScrollView scroll = new ScrollView(a); scroll.addView(preview);
        new AlertDialog.Builder(a).setTitle("Deutsche Fassung prüfen").setView(scroll)
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Text im Fenster ersetzen", (d, w) -> {
                if (!dialog.isShowing() || a.busy) return;
                text.setText(translated); text.setSelection(text.length());
                a.toast("Übersetzung übernommen · Speichern aktualisiert den Eintrag");
            }).show();
    }

    void focusText() { EditorMode.showKeyboard(text, dialog.getWindow()); }

    void setReading(boolean value) {
        reading = value;
        mode.setText(value ? "Bearbeiten" : "Lesen");
        EditorMode.apply(text, value, textKeys, textInputType);
        EditorMode.apply(title, value, titleKeys, titleInputType);
        kind.setEnabled(!value);storage.setEnabled(!value&&DriveSync.autoOwner(a).isEmpty());
        if (value) EditorMode.hideKeyboard(text, dialog.getWindow());
    }

    boolean changed() {
        try {
            return !title.getText().toString().equals(item.title) ||
                !text.getText().toString().equals(item.text) ||
                !type().equals(item.type) || (storage.getSelectedItemPosition()==1)!=item.cloud ||
                !MainActivity.attJson(files).toString().equals(MainActivity.attJson(item.files).toString());
        } catch (JSONException e) { return true; }
    }

    String type() { return kind.getSelectedItemPosition() == 1 ? "block" : "prompt"; }

    void requestClose() {
        if (a.busy) { a.toast("Bitte Dateiübertragung abwarten"); return; }
        if (!changed()) { dialog.dismiss(); return; }
        new AlertDialog.Builder(a).setTitle("Änderungen speichern?")
            .setMessage("Der gespeicherte Eintrag wurde geändert.")
            .setPositiveButton("Speichern", (d, w) -> save())
            .setNegativeButton("Verwerfen", (d, w) -> dialog.dismiss())
            .setNeutralButton("Weiter bearbeiten", null).show();
    }

    void save() {
        if (a.busy) { a.toast("Bitte Dateiübertragung abwarten"); return; }
        String nextTitle = title.getText().toString().trim(), nextText = text.getText().toString();
        if (nextTitle.isEmpty()) { setReading(false); EditorMode.showKeyboard(title, dialog.getWindow()); title.setError("Bitte einen Titel eingeben"); return; }
        if (nextText.trim().isEmpty() && files.isEmpty()) {
            setReading(false); focusText(); text.setError("Bitte Text eingeben oder Anhänge hinzufügen"); return;
        }
        if (!fresh && !a.items.contains(item)) { a.toast("Dieser Eintrag ist nicht mehr vorhanden"); return; }
        boolean cloud=storage.getSelectedItemPosition()==1;JSONObject auth=DriveSync.connection(a);if(cloud&&(auth==null||!auth.has("account")||auth.optBoolean("disconnectPending"))){a.toast("Bitte zuerst Mein Google Drive im Menü verbinden");return;}
        a.saveItem(fresh?null:item,nextTitle,nextText,type(),files,cloud,cloud?auth.optString("account"):item.cloudOwner,()->{dialog.dismiss();a.toast("Eintrag aktualisiert");});
    }

    String state() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("cloud",storage.getSelectedItemPosition()==1);o.put("id", item.id); o.put("fresh",fresh); o.put("title", title.getText().toString());
        o.put("text", text.getText().toString()); o.put("type", type());
        o.put("files", MainActivity.attJson(files)); o.put("reading", reading);
        o.put("start", text.getSelectionStart()); o.put("end", text.getSelectionEnd());
        return o.toString();
    }

    static void restore(MainActivity a, String state) throws Exception {
        JSONObject o = new JSONObject(state);
        ArrayList<MainActivity.Item> candidates=new ArrayList<>(a.items);
        if(o.optBoolean("fresh")){MainActivity.Item created=new MainActivity.Item("","",o.getString("type"));created.id=o.getString("id");candidates.add(created);}
        for (MainActivity.Item item : candidates) {
            if (!item.id.equals(o.getString("id"))) continue;
            a.entryWindow = new EntryWindow(a, item);
            EntryWindow e = a.entryWindow;
            e.storage.setSelection(!DriveSync.autoOwner(a).isEmpty()?1:o.optBoolean("cloud")?1:0);e.title.setText(o.getString("title")); e.text.setText(o.getString("text"));
            e.kind.setSelection(o.getString("type").equals("block") ? 1 : 0);
            e.files.clear(); e.files.addAll(MainActivity.readAtt(o.optJSONArray("files")));
            e.fileButtonLabel(); e.setReading(o.optBoolean("reading", true));
            e.text.setSelection(Math.max(0, Math.min(e.text.length(), o.optInt("start", 0))),
                Math.max(0, Math.min(e.text.length(), o.optInt("end", 0))));
            return;
        }
    }
}
