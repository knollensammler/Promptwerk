package de.mika.promptwerk;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
final class UpdateInfo {
 final int code;final String version,url;
 UpdateInfo(int c,String v,String u){code=c;version=v;url=u;}
 static UpdateInfo parse(String text)throws Exception{
 JSONObject j=new JSONObject(text);int c=j.getInt("versionCode");String v=j.getString("versionName"),u=j.getString("apkUrl");URI uri=new URI(u);
 if(!"de.mika.promptwerk".equals(j.getString("packageName"))||c<1||v.length()>40||!v.matches("[0-9]+(?:\\.[0-9]+)*")||!"https".equals(uri.getScheme())||!"gsgev.org".equals(uri.getHost())||uri.getPort()!=-1||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!uri.getPath().matches("/apps/Promptwerk-[0-9]+(?:\\.[0-9]+)*\\.apk"))throw new IOException("Ungültige Versionsdatei");
 return new UpdateInfo(c,v,u);
 }
 static UpdateInfo fetch()throws Exception{
 HttpsURLConnection c=(HttpsURLConnection)new URL("https://gsgev.org/apps/promptwerk-update.json").openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
 try{if(c.getResponseCode()!=200)throw new IOException("Versionsdatei nicht erreichbar");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){if(out.size()+n>16384)throw new IOException("Versionsdatei zu groß");out.write(b,0,n);}return parse(out.toString("UTF-8"));}}finally{c.disconnect();}
 }
}
