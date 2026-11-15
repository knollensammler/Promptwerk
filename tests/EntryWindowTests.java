package de.mika.promptwerk;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.*;
import android.app.*;import android.os.*;import android.view.*;import android.widget.*;
import java.io.*;import java.util.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,packageName="de.mika.promptwerk")
@LooperMode(LooperMode.Mode.PAUSED)
public class EntryWindowTests {
 public static class TestActivity extends MainActivity {boolean failSaving;@Override boolean persist(){return !failSaving&&super.persist();}}
 TestActivity a;
 @Before public void start(){RuntimeEnvironment.getApplication().getSharedPreferences("library",0).edit().clear().commit();a=Robolectric.buildActivity(TestActivity.class).setup().get();}
 @After public void stop(){a.onDestroy();idle();}
 void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 Button find(View v,String text){if(v instanceof Button&&((Button)v).getText().toString().equals(text))return (Button)v;if(v instanceof ViewGroup)for(int j=0;j<((ViewGroup)v).getChildCount();j++){Button b=find(((ViewGroup)v).getChildAt(j),text);if(b!=null)return b;}return null;}
 MainActivity.Item item(){return a.items.stream().filter(i->i.type.equals("prompt")).findFirst().get();}
 @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void tappingCollapsedListOpensLargeReaderAndNormalViewKeepsDraft()throws Exception{
  a.editor.setText("Unabhängiger Entwurf");a.editPane.setVisibility(View.GONE);a.render();a.rows.getChildAt(0).performClick();idle();EntryWindow e=a.entryWindow;assertNotNull(e);assertTrue(e.reading);assertNull(e.text.getKeyListener());assertEquals(item().text,e.text.getText().toString());assertEquals("Unabhängiger Entwurf",a.editor.getText().toString());
  View decor=e.dialog.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));decor.layout(0,0,360,800);assertTrue("Text field should fill most available height",e.text.getHeight()>350);
  android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(bitmap));try(OutputStream out=new FileOutputStream("tests/Promptwerk-2.3-Eintrag.png")){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  assertEquals(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,e.dialog.getWindow().getAttributes().softInputMode&WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST);
  find(decor,"Normalansicht").performClick();idle();assertNull(a.entryWindow);assertEquals(View.GONE,a.editPane.getVisibility());assertEquals("Unabhängiger Entwurf",a.prefs.getString("draft",""));
  find(a.rows,"Einfügen").performClick();assertTrue(a.editor.getText().toString().contains(item().text));assertNull(a.entryWindow);
 }
 @Test public void savingUpdatesExactlyTheArchivedEntryAndPreservesDraftAndIdentity(){
  MainActivity.Item item=item();item.archived=true;item.star=true;a.persist();a.archivedView=true;a.render();int count=a.items.size();String id=item.id;
  a.editor.setText("Entwurf bleibt");a.edit(item);idle();EntryWindow e=a.entryWindow;e.mode.performClick();assertFalse(e.reading);assertNotNull(e.text.getKeyListener());
  e.title.setText("Überarbeiteter Titel");e.text.setText("Der überarbeitete gespeicherte Text.");find(e.dialog.getWindow().getDecorView(),"Speichern").performClick();idle();assertNull(a.entryWindow);assertEquals(count,a.items.size());assertEquals(id,item.id);assertTrue(item.archived);assertTrue(item.star);assertEquals("Der überarbeitete gespeicherte Text.",item.text);assertEquals("Entwurf bleibt",a.editor.getText().toString());
  MainActivity reopened=Robolectric.buildActivity(TestActivity.class).setup().get();MainActivity.Item stored=reopened.items.stream().filter(i->i.id.equals(id)).findFirst().get();assertEquals(item.text,stored.text);assertTrue(stored.archived);assertTrue(stored.star);reopened.onDestroy();
 }
 @Test public void normalAndAndroidBackAskBeforeDiscardingChanges(){
  MainActivity.Item item=item();String original=item.text;a.edit(item);EntryWindow e=a.entryWindow;e.mode.performClick();e.text.setText("Noch nicht gespeichert");e.dialog.cancel();idle();AlertDialog prompt=ShadowAlertDialog.getLatestAlertDialog();assertNotSame(e.dialog,prompt);assertTrue(e.dialog.isShowing());prompt.getButton(-3).performClick();idle();assertSame(e,a.entryWindow);assertEquals(original,item.text);
  find(e.dialog.getWindow().getDecorView(),"Normalansicht").performClick();idle();ShadowAlertDialog.getLatestAlertDialog().getButton(-2).performClick();idle();assertNull(a.entryWindow);assertEquals(original,item.text);
  a.edit(item);a.entryWindow.text.setText("Jetzt speichern");a.entryWindow.dialog.cancel();idle();ShadowAlertDialog.getLatestAlertDialog().getButton(-1).performClick();idle();assertNull(a.entryWindow);assertEquals("Jetzt speichern",item.text);
 }
 @Test public void recreationKeepsUnsavedEditsAndNewAttachmentAndSaveFailureRollsBack()throws Exception{
  MainActivity.Item original=item();String originalText=original.text;a.edit(original);EntryWindow e=a.entryWindow;e.mode.performClick();e.text.setText("Ungespeicherte Arbeit");
  File temp=File.createTempFile("new-file-",".txt",a.getCacheDir());try(OutputStream out=new FileOutputStream(temp)){out.write("Anhang".getBytes("UTF-8"));}MainActivity.Att attachment=new MainActivity.Att(a.digest(temp),"Neu.txt","text/plain",temp.length());a.file(attachment).getParentFile().mkdirs();assertTrue(temp.renameTo(a.file(attachment)));e.files.add(attachment);a.pickTarget=e.files;a.afterPick=()->e.fileButtonLabel();
  Bundle state=new Bundle();a.onSaveInstanceState(state);a.onDestroy();idle();TestActivity reopened=Robolectric.buildActivity(TestActivity.class).create(state).start().resume().visible().get();assertNotNull(reopened.entryWindow);assertSame(reopened.entryWindow.files,reopened.pickTarget);assertNotNull(reopened.afterPick);assertFalse(reopened.entryWindow.reading);assertEquals("Ungespeicherte Arbeit",reopened.entryWindow.text.getText().toString());assertTrue(reopened.file(attachment).isFile());
  MainActivity.Item stored=reopened.entryWindow.item;assertEquals(originalText,stored.text);reopened.failSaving=true;reopened.entryWindow.save();assertEquals(originalText,stored.text);assertNotNull(reopened.entryWindow);reopened.failSaving=false;reopened.entryWindow.save();idle();assertEquals("Ungespeicherte Arbeit",stored.text);assertEquals(1,stored.files.size());reopened.onDestroy();
 }
 @Test public void editButtonFocusesTextShowsKeyboardAndAcceptsRealInputConnection(){
  a.edit(item());EntryWindow e=a.entryWindow;String original=e.text.getText().toString();e.mode.performClick();idle();
  assertTrue("Edit button must focus the text field",e.text.isFocused());
  assertTrue("Touch focus must be restored",e.text.isFocusableInTouchMode());
  android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
  assertTrue("Edit button must request the IME",Shadows.shadowOf(imm).isSoftInputVisible());
  assertTrue("Multiline input type must survive read mode",(e.text.getInputType()&android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)!=0);
  for(int n=0;n<3;n++){
   e.text.setSelection(e.text.length());android.view.inputmethod.InputConnection connection=e.text.onCreateInputConnection(new android.view.inputmethod.EditorInfo());assertNotNull(connection);assertTrue(connection.commitText(" Eingabe",1));
   e.mode.performClick();idle();assertTrue(e.reading);assertFalse(Shadows.shadowOf(imm).isSoftInputVisible());
   e.mode.performClick();idle();assertFalse(e.reading);assertTrue(e.text.isFocused());assertTrue(Shadows.shadowOf(imm).isSoftInputVisible());
  }
  assertEquals(original+" Eingabe Eingabe Eingabe",e.text.getText().toString());e.save();idle();assertEquals(original+" Eingabe Eingabe Eingabe",item().text);
 }
 @Test public void draftWriteButtonAlsoRestoresFocusAndKeyboard(){
  a.editor.setText("Mein Entwurf");a.expandEditor();idle();a.readingButton.performClick();idle();assertTrue(a.reading);a.readingButton.performClick();idle();
  assertTrue("Write button must focus the draft",a.editor.isFocused());assertTrue(a.editor.isFocusableInTouchMode());
  android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
  assertTrue(Shadows.shadowOf(imm).isSoftInputVisible());assertTrue((a.editor.getInputType()&android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE)!=0);
  a.editor.setSelection(a.editor.length());android.view.inputmethod.InputConnection c=a.editor.onCreateInputConnection(new android.view.inputmethod.EditorInfo());assertNotNull(c);assertTrue(c.commitText(" ergänzt",1));assertEquals("Mein Entwurf ergänzt",a.prefs.getString("draft",""));
 }

}
