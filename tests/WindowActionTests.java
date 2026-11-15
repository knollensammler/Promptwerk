package de.mika.promptwerk;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.*;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;
import java.io.*;import java.util.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,packageName="de.mika.promptwerk")
@LooperMode(LooperMode.Mode.PAUSED)
public class WindowActionTests {
 TokensparerTests.TestActivity a;
 @Before public void start(){RuntimeEnvironment.getApplication().getSharedPreferences("library",0).edit().clear().commit();a=Robolectric.buildActivity(TokensparerTests.TestActivity.class).setup().get();}
 @After public void stop(){a.onDestroy();idle();}
 void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 Button find(View v,String text){if(v instanceof Button&&((Button)v).getText().toString().equals(text))return (Button)v;if(v instanceof ViewGroup)for(int j=0;j<((ViewGroup)v).getChildCount();j++){Button b=find(((ViewGroup)v).getChildAt(j),text);if(b!=null)return b;}return null;}
 EditText firstEdit(View v){if(v instanceof EditText)return (EditText)v;if(v instanceof ViewGroup)for(int j=0;j<((ViewGroup)v).getChildCount();j++){EditText e=firstEdit(((ViewGroup)v).getChildAt(j));if(e!=null)return e;}return null;}
 AlertDialog dialog(){idle();return ShadowAlertDialog.getLatestAlertDialog();}
 void choose(int n){AlertDialog d=dialog();d.getListView().performItemClick(null,n,n);idle();}
 String copied(){return ((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText().toString();}
 MainActivity.Item item(){return a.items.stream().filter(i->i.type.equals("prompt")).findFirst().get();}
 MainActivity.Att attachment(String name,String mime)throws Exception{File temp=File.createTempFile("attach-",".tmp",a.getCacheDir());try(OutputStream out=new FileOutputStream(temp)){out.write(name.getBytes("UTF-8"));}MainActivity.Att f=new MainActivity.Att(a.digest(temp),name,mime,temp.length());a.file(f).getParentFile().mkdirs();if(!a.file(f).exists())assertTrue(temp.renameTo(a.file(f)));else temp.delete();return f;}
 @Test public void coldStartCollapsedAndDraftFullscreenActionsKeepTextAfterCopyShareAndSave(){
  assertEquals(View.GONE,a.editPane.getVisibility());assertEquals("Editor ▾",a.foldButton.getText().toString());a.editor.setText("Entwurf bleibt erhalten");
  TokensparerTests.TestActivity cold=Robolectric.buildActivity(TokensparerTests.TestActivity.class).setup().get();assertEquals(View.GONE,cold.editPane.getVisibility());assertEquals("Entwurf bleibt erhalten",cold.editor.getText().toString());cold.onDestroy();
  a.foldButton.performClick();assertEquals(View.VISIBLE,a.editPane.getVisibility());a.foldButton.performClick();assertEquals(View.GONE,a.editPane.getVisibility());
  a.expandEditor();idle();View decor=a.expandedDialog.getWindow().getDecorView();for(String name:new String[]{"Kopieren","Teilen","Speichern","Tokensparer","Anhänge","Leeren","Normalansicht"})assertNotNull(name,find(decor,name));
  find(decor,"Kopieren").performClick();assertEquals("Entwurf bleibt erhalten",copied());assertEquals("Entwurf bleibt erhalten",a.editor.getText().toString());
  find(decor,"Teilen").performClick();Intent chooser=Shadows.shadowOf(a).getNextStartedActivity();Intent send=chooser.getParcelableExtra(Intent.EXTRA_INTENT);assertEquals("Entwurf bleibt erhalten",send.getStringExtra(Intent.EXTRA_TEXT));assertEquals("Entwurf bleibt erhalten",a.editor.getText().toString());
  find(decor,"Speichern").performClick();AlertDialog save=dialog();firstEdit(save.getWindow().getDecorView()).setText("Gespeicherter Entwurf");save.getButton(-1).performClick();idle();assertEquals("Entwurf bleibt erhalten",a.prefs.getString("draft",""));
  find(decor,"Leeren").performClick();assertEquals("",a.editor.getText().toString());a.undo();assertEquals("Entwurf bleibt erhalten",a.editor.getText().toString());
 }
 @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void entryCopyAndShareUseUnsavedWindowTextAndAllAttachmentsWithoutClearing()throws Exception{
  a.editor.setText("Separater Arbeitsentwurf");MainActivity.Item item=item();String original=item.text;item.files.add(attachment("Bild.png","image/png"));item.files.add(attachment("Brief.pdf","application/pdf"));item.files.add(attachment("Notizen.txt","text/plain"));a.persist();a.edit(item);EntryWindow e=a.entryWindow;e.text.setText("Ungespeicherte Fassung");
  View decor=e.dialog.getWindow().getDecorView();find(decor,"Kopieren").performClick();assertEquals("Ungespeicherte Fassung",copied());assertEquals("Ungespeicherte Fassung",e.text.getText().toString());assertEquals(original,item.text);
  find(decor,"Teilen").performClick();Intent chooser=Shadows.shadowOf(a).getNextStartedActivity();assertEquals(Intent.ACTION_CHOOSER,chooser.getAction());Intent send=chooser.getParcelableExtra(Intent.EXTRA_INTENT);assertEquals(Intent.ACTION_SEND_MULTIPLE,send.getAction());assertEquals("Ungespeicherte Fassung",send.getStringExtra(Intent.EXTRA_TEXT));assertEquals(3,send.getParcelableArrayListExtra(Intent.EXTRA_STREAM).size());assertEquals(3,send.getClipData().getItemCount());assertTrue((send.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
  assertEquals(3,e.files.size());assertEquals("Ungespeicherte Fassung",e.text.getText().toString());assertEquals(original,item.text);assertEquals("Separater Arbeitsentwurf",a.editor.getText().toString());assertTrue(e.dialog.isShowing());
  decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));decor.layout(0,0,360,800);assertTrue(e.text.getHeight()>350);for(String label:new String[]{"Kopieren","Teilen","Speichern","Normalansicht","•••"})assertTrue(find(decor,label).getHeight()>=a.dp(48));
  android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(bitmap));try(OutputStream out=new FileOutputStream("tests/Promptwerk-2.5-Aktionen.png")){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  find(decor,"•••").performClick();choose(2);assertEquals("",e.text.getText().toString());assertEquals(3,e.files.size());assertEquals(original,item.text);e.undoText();assertEquals("Ungespeicherte Fassung",e.text.getText().toString());
 }
 @Test public void tokensparerTranslatesCurrentEntryAndAppliesBackToThatWindowOnly(){
  a.editor.setText("Arbeitsentwurf bleibt");MainActivity.Item item=item();String original=item.text;a.edit(item);EntryWindow e=a.entryWindow;e.text.setText("Current unsaved English text.");find(e.dialog.getWindow().getDecorView(),"•••").performClick();choose(0);choose(1);dialog().getButton(-1).performClick();idle();assertEquals("Current unsaved English text.",a.fake.input);assertFalse(a.fake.toEnglish);
  a.fake.callback.done("Aktueller deutscher Fenstertext.");idle();assertEquals("Current unsaved English text.",e.text.getText().toString());dialog().getButton(-1).performClick();idle();assertEquals("Aktueller deutscher Fenstertext.",e.text.getText().toString());assertEquals(original,item.text);assertEquals("Arbeitsentwurf bleibt",a.editor.getText().toString());e.undoText();assertEquals("Current unsaved English text.",e.text.getText().toString());
  int count=a.items.size();e.tokensparer();choose(0);dialog().getButton(-1).performClick();idle();assertTrue(a.fake.toEnglish);assertEquals("Current unsaved English text.",a.fake.input);a.fake.callback.done("English version saved separately.");idle();AlertDialog save=dialog();firstEdit(save.getWindow().getDecorView()).setText("Separate English copy");save.getButton(-1).performClick();idle();assertEquals(count+1,a.items.size());assertEquals("Current unsaved English text.",e.text.getText().toString());assertEquals(original,item.text);assertEquals("Arbeitsentwurf bleibt",a.editor.getText().toString());
 }
 @Test public void missingAttachmentShareFailurePreservesOpenEditsAndDraft(){
  a.editor.setText("Mein Entwurf");MainActivity.Item item=item();String original=item.text;a.edit(item);EntryWindow e=a.entryWindow;e.text.setText("Änderungen behalten");e.files.add(new MainActivity.Att(String.join("",Collections.nCopies(64,"f")),"Fehlt.pdf","application/pdf",7));find(e.dialog.getWindow().getDecorView(),"Teilen").performClick();idle();assertNull(Shadows.shadowOf(a).getNextStartedActivity());assertEquals("Änderungen behalten",e.text.getText().toString());assertEquals(1,e.files.size());assertEquals(original,item.text);assertEquals("Mein Entwurf",a.editor.getText().toString());assertTrue(e.dialog.isShowing());
 }
}
