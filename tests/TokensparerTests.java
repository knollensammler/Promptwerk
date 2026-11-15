package de.mika.promptwerk;

import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.*;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;
import java.util.*;import java.io.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,packageName="de.mika.promptwerk")
@LooperMode(LooperMode.Mode.PAUSED)
public class TokensparerTests {
 public static class FakeTranslation implements MainActivity.TranslationService {
  MainActivity.TranslationCallback callback;String input;boolean toEnglish,closed;
  public void translate(String text,boolean english,MainActivity.TranslationCallback c){input=text;toEnglish=english;callback=c;}
  public void close(){closed=true;}
 }
 public static class TestActivity extends MainActivity {
  FakeTranslation fake;
  @Override TranslationService newTranslationService(){fake=new FakeTranslation();return fake;}
 }
 TestActivity a;
 @Before public void setUp(){RuntimeEnvironment.getApplication().getSharedPreferences("library",0).edit().clear().commit();a=Robolectric.buildActivity(TestActivity.class).setup().get();}
 @After public void tearDown(){a.onDestroy();}
 void idle(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
 AlertDialog dialog(){idle();return ShadowAlertDialog.getLatestAlertDialog();}
 EditText firstEdit(View v){if(v instanceof EditText)return (EditText)v;if(v instanceof ViewGroup)for(int j=0;j<((ViewGroup)v).getChildCount();j++){EditText e=firstEdit(((ViewGroup)v).getChildAt(j));if(e!=null)return e;}return null;}
 void choose(AlertDialog d,int n){d.getListView().performItemClick(null,n,n);idle();}
 @Test public void englishSavePreservesCurrentGermanDraftAndSourceAttachments()throws Exception{
  a.editor.setText("Bitte sachlich schreiben.");MainActivity.Att file=new MainActivity.Att(String.join("",Collections.nCopies(64,"a")),"Notizen.txt","text/plain",3);a.draftFiles.add(file);a.draftChanged();int count=a.items.size();
  a.tokensparer();choose(dialog(),0);dialog().getButton(-1).performClick();idle();
  assertEquals("Bitte sachlich schreiben.",a.fake.input);assertTrue(a.fake.toEnglish);
  a.editor.setText("Weitergearbeiteter deutscher Entwurf");a.fake.callback.done("Please write objectively.");idle();assertTrue(a.fake.closed);
  assertEquals(count,a.items.size());AlertDialog save=dialog();firstEdit(save.getWindow().getDecorView()).setText("English prompt");save.getButton(-1).performClick();
  MainActivity.Item stored=a.items.get(a.items.size()-1);assertEquals("Please write objectively.",stored.text);assertEquals(1,stored.files.size());assertEquals("Notizen.txt",stored.files.get(0).name);
  assertEquals("Weitergearbeiteter deutscher Entwurf",a.editor.getText().toString());assertEquals(1,a.draftFiles.size());
  MainActivity reopened=Robolectric.buildActivity(TestActivity.class).setup().get();assertEquals("Weitergearbeiteter deutscher Entwurf",reopened.editor.getText().toString());assertEquals("Please write objectively.",reopened.items.get(reopened.items.size()-1).text);reopened.onDestroy();
 }
 @Test public void storedEnglishBackToGermanNeedsConfirmationAndUndoRestoresTextAndFiles(){
  MainActivity.Item stored=new MainActivity.Item("Saved English","Write a concise letter.","prompt");stored.archived=true;stored.files.add(new MainActivity.Att(String.join("",Collections.nCopies(64,"b")),"Anlage.pdf","application/pdf",2));a.items.add(stored);a.persist();a.editor.setText("Aktueller Entwurf");
  a.tokensparer();choose(dialog(),1);choose(dialog(),a.items.size()-1);dialog().getButton(-1).performClick();idle();assertFalse(a.fake.toEnglish);assertEquals(stored.text,a.fake.input);a.fake.callback.done("Schreibe einen kurzen Brief.");idle();
  assertEquals("Aktueller Entwurf",a.editor.getText().toString());dialog().getButton(-2).performClick();idle();assertEquals("Aktueller Entwurf",a.editor.getText().toString());
  a.startTranslation(stored.text,false,new ArrayList<>(stored.files));a.fake.callback.done("Schreibe einen kurzen Brief.");idle();dialog().getButton(-1).performClick();idle();assertEquals("Schreibe einen kurzen Brief.",a.editor.getText().toString());assertEquals(1,a.draftFiles.size());assertEquals(stored.text,a.items.get(a.items.size()-1).text);
  a.undo();assertEquals("Aktueller Entwurf",a.editor.getText().toString());assertEquals(0,a.draftFiles.size());a.undo();assertEquals("Schreibe einen kurzen Brief.",a.editor.getText().toString());assertEquals(1,a.draftFiles.size());
 }
 @Test public void cancellationFailureEmptyResultAndLateCallbackDoNotLoseData(){
  a.editor.setText("Mein Original");String before=a.prefs.getString("items","");
  a.startTranslation("Mein Original",true,new ArrayList<>());FakeTranslation cancelled=a.fake;a.translationProgress.getButton(-2).performClick();idle();assertTrue(cancelled.closed);cancelled.callback.done("Late result");idle();assertNull(a.translationService);assertEquals("Mein Original",a.editor.getText().toString());
  a.startTranslation("Mein Original",true,new ArrayList<>());a.fake.callback.failed(new IOException("offline"));idle();assertEquals("Mein Original",a.editor.getText().toString());dialog().dismiss();
  a.startTranslation("Mein Original",true,new ArrayList<>());a.fake.callback.done("");idle();assertEquals("Mein Original",a.editor.getText().toString());assertEquals(before,a.prefs.getString("items",""));
  dialog().dismiss();a.translateText("",true,new ArrayList<>());assertNull(a.translationService);
 }
 @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void fullScreenUsesSameEditorAndKeepsSelectionAutosaveUndoAndReadingMode()throws Exception{
  a.editor.setText("Dies ist mein deutscher Entwurf.\n\nHier kann ich in Ruhe weiterlesen und schreiben.");a.editor.setSelection(5,12);EditText original=a.editor;
  a.expandEditor();idle();assertSame(original,a.editor);assertEquals(5,a.editor.getSelectionStart());assertEquals(12,a.editor.getSelectionEnd());
  assertEquals(0,a.expandedDialog.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN);assertEquals(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,a.expandedDialog.getWindow().getAttributes().softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST);
  View decor=a.expandedDialog.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));decor.layout(0,0,360,800);assertTrue("Editor should occupy most of screen",a.editor.getHeight()>450);
  android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(360,800,android.graphics.Bitmap.Config.ARGB_8888);decor.draw(new android.graphics.Canvas(image));new File("tests").mkdirs();try(OutputStream out=new FileOutputStream("tests/Promptwerk-2.2-Vollbild.png")){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
  a.readingButton.performClick();assertTrue(a.reading);assertNull(a.editor.getKeyListener());assertFalse(a.editor.getShowSoftInputOnFocus());assertEquals(5,a.editor.getSelectionStart());a.readingButton.performClick();assertFalse(a.reading);assertNotNull(a.editor.getKeyListener());
  a.editor.setText("Im Vollbild bearbeitet");a.expandedDialog.dismiss();idle();assertNull(a.expandedDialog);assertSame(a.editorBox,a.editor.getParent());assertEquals("Im Vollbild bearbeitet",a.prefs.getString("draft",""));assertEquals(a.dp(a.compact?120:152),a.editorBox.getLayoutParams().height);
  a.undo();assertTrue(a.editor.getText().toString().startsWith("Dies ist mein"));a.expandEditor();a.setReading(true);Bundle state=new Bundle();a.onSaveInstanceState(state);a.onDestroy();TestActivity recreated=Robolectric.buildActivity(TestActivity.class).create(state).start().resume().visible().get();assertNotNull("Full-screen state restored",recreated.expandedDialog);assertTrue(recreated.reading);assertTrue(recreated.editor.getText().toString().startsWith("Dies ist mein"));recreated.expandedDialog.cancel();idle();assertNotNull("Write mode restored after Android back",recreated.editor.getKeyListener());recreated.onDestroy();
 }
 @Test public void attachmentPolicyRegression()throws Exception{PolicyTests.passed=0;PolicyTests.main(new String[0]);assertEquals(29,PolicyTests.passed);}
 @Test public void chunkingPreservesParagraphsAndUnicodeExactly(){
  String text="\n  "+String.join("",Collections.nCopies(5000,"😀"))+"\n\n"+String.join("",Collections.nCopies(700,"Wort "))+"\n";
  ArrayList<String> chunks=LocalTranslation.chunks(text);assertEquals(text,String.join("",chunks));for(String chunk:chunks){if(!chunk.trim().isEmpty())assertTrue(chunk.length()<=4000);assertFalse(Character.isLowSurrogate(chunk.charAt(0)));assertFalse(Character.isHighSurrogate(chunk.charAt(chunk.length()-1)));}
 }
}
