package de.mika.promptwerk;
import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import org.json.*;
final class DriveHttp {
 static final class HttpError extends IOException{final int status;HttpError(int status,String text){super(text);this.status=status;}}
 static final String BASE=BuildConfig.DRIVE_BASE;
 static byte[] request(String path,String method,byte[] body,String token,String name)throws Exception{
  if(!path.matches("google-(sync|file|device)\\.php(?:\\?[a-zA-Z0-9_=&%.-]*)?"))throw new IOException("Ungültiges Syncziel");
  HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(60000);c.setRequestMethod(method);c.setRequestProperty("Accept","application/json");c.setRequestProperty("X-Promptwerk-Sync-Protocol","2");
  if(token!=null)c.setRequestProperty("X-Promptwerk-Device","Bearer "+token);
  if(name!=null)c.setRequestProperty("X-File-Name",URLEncoder.encode(name,"UTF-8").replace("+","%20"));
  if(body!=null){c.setRequestProperty("Content-Type",name==null?"application/json":"application/octet-stream");c.setDoOutput(true);c.setFixedLengthStreamingMode(body.length);try(OutputStream out=c.getOutputStream()){out.write(body);}}
  int status=c.getResponseCode();InputStream input=status>=200&&status<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream();if(input!=null)try(InputStream in=input){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){bytes.write(b,0,n);if(bytes.size()>25*1024*1024)throw new IOException("Syncantwort zu groß");}}
  c.disconnect();if(status<200||status>=300){String message="Sync nicht bestätigt ("+status+")";try{message=new JSONObject(bytes.toString("UTF-8")).optString("error",message);}catch(Exception ignored){}throw new HttpError(status,message);}return bytes.toByteArray();
 }
 static byte[] read(InputStream in,int max)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>max)throw new IOException("Datei zu groß");}return out.toByteArray();}
 static JSONObject json(String path,String method,JSONObject body,String token)throws Exception{return new JSONObject(new String(request(path,method,body==null?null:body.toString().getBytes(StandardCharsets.UTF_8),token,null),StandardCharsets.UTF_8));}
}
