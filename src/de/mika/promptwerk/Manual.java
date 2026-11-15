package de.mika.promptwerk;
import android.app.AlertDialog;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.*;
import java.nio.charset.StandardCharsets;
final class Manual {
 static void show(MainActivity a){
  try(InputStream in=a.getResources().openRawResource(de.mika.promptwerk.R.raw.anleitung)){
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);
   TextView text=a.label(new String(bytes.toByteArray(),StandardCharsets.UTF_8),16,a.INK);text.setTextIsSelectable(true);text.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(12));
   ScrollView scroll=new ScrollView(a);scroll.addView(text);
   new AlertDialog.Builder(a).setTitle("Ausführliche Anleitung").setView(scroll).setPositiveButton("Schließen",null).show();
  }catch(IOException e){a.toast("Anleitung konnte nicht geöffnet werden");}
 }
}
