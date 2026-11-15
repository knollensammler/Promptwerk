package de.mika.promptwerk;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.*;
import android.app.*;import android.content.*;import android.speech.RecognizerIntent;import java.util.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @LooperMode(LooperMode.Mode.PAUSED)
public class Revision32Tests {
 MainActivity a;
 @Before public void setup(){RuntimeEnvironment.getApplication().getSharedPreferences("library",0).edit().clear().commit();a=Robolectric.buildActivity(MainActivity.class).setup().visible().get();}
 @After public void close(){a.onDestroy();}
 Intent result(String value){Intent i=new Intent();i.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,new ArrayList<>(Arrays.asList(value)));return i;}
 @Test public void insertsAtSelectionAndPreservesUndo(){a.showDraft();a.editor.setText("Hallo Welt");a.editor.setSelection(6,10);a.speechInput.target=a.editor;a.speechInput.before="Hallo Welt";a.speechInput.start=6;a.speechInput.end=10;a.onActivityResult(41,Activity.RESULT_OK,result("Mika"));assertEquals("Hallo Mika",a.editor.getText().toString());assertNull(a.speechInput.target);a.undo();assertEquals("Hallo Welt",a.editor.getText().toString());}
 @Test public void cancellationDoesNotChangeText(){a.editor.setText("Bleibt");a.speechInput.target=a.editor;a.onActivityResult(41,Activity.RESULT_CANCELED,null);assertEquals("Bleibt",a.editor.getText().toString());assertNull(a.speechInput.target);}
 @Test public void interveningEditIsNotOverwritten(){a.editor.setText("Neu");a.speechInput.target=a.editor;a.speechInput.before="Alt";a.speechInput.start=0;a.speechInput.end=3;a.onActivityResult(41,Activity.RESULT_OK,result("Diktat"));assertEquals("Neu",a.editor.getText().toString());}
 @Test public void entryWindowDiktatIsIndependentFromDraft(){a.editor.setText("Entwurf");MainActivity.Item item=a.items.get(0);a.edit(item);EntryWindow w=a.entryWindow;w.setReading(false);w.text.setText("Text");a.speechInput.target=w.text;a.speechInput.before="Text";a.speechInput.start=0;a.speechInput.end=4;a.onActivityResult(41,Activity.RESULT_OK,result("Diktierter Eintrag"));assertEquals("Diktierter Eintrag",w.text.getText().toString());assertEquals("Entwurf",a.editor.getText().toString());assertNotEquals("Diktierter Eintrag",item.text);}
 @Test public void closedWindowDoesNotReceiveDiktat(){a.edit(a.items.get(0));EntryWindow w=a.entryWindow;String before=w.text.getText().toString();a.speechInput.target=w.text;a.speechInput.before=before;a.speechInput.start=0;a.speechInput.end=before.length();w.dialog.dismiss();a.onActivityResult(41,Activity.RESULT_OK,result("Verwerfen"));assertEquals(before,w.text.getText().toString());}
 @Test public void manualIsTaskBasedWithoutReleaseHistory(){a.showManual();AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertTrue(d.isShowing());assertEquals("Ausführliche Anleitung",Shadows.shadowOf(d).getTitle());String manual;try(java.io.InputStream in=a.getResources().openRawResource(R.raw.anleitung)){manual=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){throw new AssertionError(e);}assertTrue(manual.contains("6. Spracheingabe"));assertFalse(manual.contains("Neu in"));assertFalse(manual.contains("Seit 3."));}
}
