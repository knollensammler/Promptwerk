package de.mika.promptwerk;
import android.app.AlertDialog;
import android.content.*;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import java.util.ArrayList;
/** Uses the installed Android recognition activity; never starts recording itself. */
final class SpeechInput {
 static final int REQUEST=41;
 final MainActivity a;
 EditText target;
 String before;
 int start,end;
 SpeechInput(MainActivity activity){a=activity;}
 void start(EditText input){
  if(a.busy){a.toast("Bitte Dateiübertragung abwarten");return;}
  if(target!=null){a.toast("Spracheingabe läuft bereits");return;}
  new AlertDialog.Builder(a).setTitle("Spracheingabe")
   .setMessage("Der auf deinem Gerät eingerichtete Spracherkennungsdienst verarbeitet deine Aufnahme. Je nach Dienst kann Audio zur Erkennung übertragen werden. Offline-Erkennung ist nur verfügbar, wenn der Dienst sie unterstützt und passende Sprachdaten vorhanden sind. Promptwerk speichert keine Audioaufnahme. Das Diktat wird an der Cursorposition eingefügt und kann vor dem Speichern geprüft werden.")
   .setNegativeButton("Abbrechen",null).setPositiveButton("Diktieren",(d,w)->launch(input)).show();
 }
 boolean active(EditText input){return input!=null&&!a.isFinishing()&&!a.isDestroyed()&&(input==a.editor||(a.entryWindow!=null&&a.entryWindow.text==input&&a.entryWindow.dialog.isShowing()));}
 void launch(EditText input){
  if(!active(input)){a.toast("Textfenster bitte erneut öffnen");return;}
  target=input;before=input.getText().toString();start=Math.max(0,input.getSelectionStart());end=Math.max(start,input.getSelectionEnd());
  Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
  i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"de-DE");
  i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Prompt oder Baustein diktieren");
  i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
  try{a.startActivityForResult(i,REQUEST);}catch(ActivityNotFoundException e){target=null;a.toast("Kein Spracherkennungsdienst vorhanden. Du kannst das Mikrofon deiner Tastatur verwenden.");}catch(SecurityException e){target=null;a.toast("Spracherkennungsdienst nicht freigegeben. Bitte die Geräteeinstellungen prüfen.");}
 }
 void result(int result,Intent data){
  EditText input=target;target=null;
  if(result!=android.app.Activity.RESULT_OK||data==null)return;
  ArrayList<String> values=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
  if(!active(input)){a.toast("Diktat nicht eingefügt: Textfenster wurde geschlossen. Bitte erneut diktieren.");return;}
  if(values==null||values.isEmpty()||values.get(0).trim().isEmpty()){a.toast("Kein Text erkannt");return;}
  if(!input.getText().toString().equals(before)){a.toast("Text wurde zwischenzeitlich geändert. Bitte erneut diktieren.");return;}
  input.getText().replace(Math.min(start,input.length()),Math.min(end,input.length()),values.get(0));
  input.setSelection(Math.min(start+values.get(0).length(),input.length()));
  a.toast("Diktat eingefügt · bitte prüfen und speichern");
 }
}
