package de.mika.promptwerk;
import org.json.*;import java.util.*;
/** Pure queue merge shared by foreground and background synchronization. */
final class DriveMerge {
 static JSONObject completeEntry(JSONObject source,String owner)throws JSONException{
  if(!owner.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Google-Kennung ungültig");
  JSONObject e=copy(source);e.put("cloud",true).put("cloudOwner",owner);
  JSONArray files=e.optJSONArray("files");if(files!=null)for(int n=0;n<files.length();n++)files.getJSONObject(n).put("sync",true);
  return e;
 }
 static JSONArray completeCollection(JSONArray items,String owner)throws JSONException{
  JSONArray out=new JSONArray();for(int n=0;n<items.length();n++){JSONObject e=items.getJSONObject(n);out.put(!e.optString("cloudOwner").isEmpty()&&!owner.equals(e.optString("cloudOwner"))?copy(e):completeEntry(e,owner));}return out;
 }
 static JSONObject copy(JSONObject o)throws JSONException{return new JSONObject(o.toString());}
 static String canonical(Object value)throws JSONException{if(value instanceof JSONObject){JSONObject o=(JSONObject)value;java.util.ArrayList<String> keys=new java.util.ArrayList<>();java.util.Iterator<String> it=o.keys();while(it.hasNext())keys.add(it.next());java.util.Collections.sort(keys);StringBuilder b=new StringBuilder("{");for(String k:keys)b.append(JSONObject.quote(k)).append(':').append(canonical(o.get(k))).append(',');return b.append('}').toString();}if(value instanceof JSONArray){JSONArray a=(JSONArray)value;StringBuilder b=new StringBuilder("[");for(int i=0;i<a.length();i++)b.append(canonical(a.get(i))).append(',');return b.append(']').toString();}return value==null||value==JSONObject.NULL?"null":value instanceof String?JSONObject.quote((String)value):value.toString();}
 static String content(JSONObject e)throws JSONException{JSONObject n=new JSONObject();for(String key:new String[]{"id","title","text","type","project","star","archived"})n.put(key,e.opt(key));JSONArray files=new JSONArray(),input=e.optJSONArray("files");if(input!=null)for(int i=0;i<input.length();i++){JSONObject f=input.getJSONObject(i),clean=new JSONObject();for(String key:new String[]{"key","name","mime","size"})clean.put(key,f.opt(key));clean.put("sync",f.optBoolean("sync"));files.put(clean);}n.put("files",files).put("section",e.optString("section","collection")).put("extraTime",e.optLong("extraTime",0));return canonical(n);}

 static void capture(JSONObject records,JSONArray items,String owner)throws JSONException{
  HashSet<String> seen=new HashSet<>();
  for(int i=0;i<items.length();i++){JSONObject e=items.getJSONObject(i);String id=e.getString("id");seen.add(id);JSONObject r=records.optJSONObject(id);
   if(!e.optBoolean("cloud")||!owner.equals(e.optString("cloudOwner"))){if(r!=null)r.put("excluded",true);continue;}
   if(r==null){r=new JSONObject().put("entry",copy(e)).put("base",0).put("dirty",true);records.put(id,r);}
   else if(!content(e).equals(content(r.getJSONObject("entry")))){r.put("entry",copy(e)).put("dirty",true);}
   r.put("seenLocal",true).put("excluded",false).put("deleted",false);
  }
  Iterator<String> ids=records.keys();while(ids.hasNext()){JSONObject r=records.getJSONObject(ids.next());if(r.optBoolean("seenLocal")&&!r.optBoolean("excluded")&&!seen.contains(r.getJSONObject("entry").getString("id")))r.put("deleted",true).put("dirty",true);}
 }
 static void pull(JSONObject records,JSONObject manifest,String owner)throws JSONException{
  JSONObject entries=manifest.getJSONObject("entries");Iterator<String> ids=entries.keys();while(ids.hasNext()){String id=ids.next();JSONObject old=records.optJSONObject(id);if(old!=null&&(old.optBoolean("dirty")||old.has("flight")))continue;JSONObject e=fromWire(entries.getJSONObject(id),owner);JSONObject r=new JSONObject().put("entry",e).put("base",e.optInt("syncVersion")).put("dirty",false).put("deleted",false).put("seenLocal",old!=null&&old.optBoolean("seenLocal")).put("excluded",old!=null&&old.optBoolean("excluded"));records.put(id,r);}
  JSONObject deleted=manifest.optJSONObject("deleted");if(deleted!=null){ids=deleted.keys();while(ids.hasNext()){String id=ids.next();JSONObject r=records.optJSONObject(id);if(r!=null&&!r.optBoolean("dirty"))r.put("deleted",true).put("remoteDeleted",true);}}
 }
 static JSONObject startFlight(JSONObject row)throws JSONException{JSONObject existing=row.optJSONObject("flight");if(existing!=null)return existing;JSONObject flight=new JSONObject().put("operation",UUID.randomUUID().toString()).put("entry",copy(row.getJSONObject("entry"))).put("deleted",row.optBoolean("deleted")).put("base",row.optInt("base"));row.put("flight",flight);return flight;}
 static String acknowledge(JSONObject records,String id,JSONObject result,String owner)throws JSONException{
  JSONObject latest=records.optJSONObject(id);if(latest==null)return "";JSONObject flight=latest.optJSONObject("flight");if(flight==null)return "";
  boolean edited=!content(latest.getJSONObject("entry")).equals(content(flight.getJSONObject("entry")))||latest.optBoolean("deleted")!=flight.optBoolean("deleted");
  JSONObject entries=result.getJSONObject("document").getJSONObject("entries");int base=result.getInt("committedVersion");
  if(result.optBoolean("deleteConflict")){
   if(!edited){JSONObject remote=entries.optJSONObject(id);if(remote==null)records.remove(id);else records.put(id,new JSONObject().put("entry",fromWire(remote,owner)).put("base",remote.optInt("syncVersion")).put("dirty",false).put("deleted",false).put("seenLocal",false).put("excluded",latest.optBoolean("excluded")));}
   else{latest.remove("flight");latest.put("base",base).put("deleted",false).put("dirty",true);}
   return "Löschung nicht übernommen: Eine neuere Fassung wurde erhalten. Bitte prüfen und bei Bedarf erneut löschen.";
  }
  if(result.optBoolean("conflict")){
   String next=result.getString("id");JSONObject e=copy(edited?latest.getJSONObject("entry"):flight.getJSONObject("entry"));e.put("id",next);JSONObject remote=entries.optJSONObject(next);e.put("title",remote==null?flight.getJSONObject("entry").getString("title")+" · Konfliktkopie":remote.getString("title"));
   records.remove(id);records.put(next,new JSONObject().put("entry",e).put("base",base).put("dirty",edited).put("deleted",edited&&latest.optBoolean("deleted")).put("seenLocal",false).put("excluded",latest.optBoolean("excluded")));
   return "Gleichzeitige Änderungen wurden als getrennte Konfliktkopie erhalten.";
  }
  if(flight.optBoolean("deleted")&&!edited){records.remove(id);return "";}
  latest.remove("flight");latest.put("base",base).put("dirty",edited);return "";
 }
 static JSONObject fromWire(JSONObject wire,String owner)throws JSONException{
  JSONObject e=copy(wire);e.put("cloud",true).put("cloudOwner",owner);JSONArray files=e.optJSONArray("files");JSONArray out=new JSONArray();if(files!=null)for(int i=0;i<files.length();i++){JSONObject f=copy(files.getJSONObject(i));String key=f.optString("key",f.optString("id"));if(!key.matches("[a-f0-9]{64}"))key=sha(("foreign-local:"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8));JSONObject nativeFile=new JSONObject().put("key",key).put("name",f.getString("name")).put("mime",f.getString("mime")).put("size",f.getLong("size")).put("sync",f.optString("storage").equals("drive"));out.put(nativeFile);}e.put("files",out);return e;
 }
 static JSONObject wire(JSONObject nativeEntry)throws JSONException{
  JSONObject e=copy(nativeEntry);JSONArray out=new JSONArray(),files=e.optJSONArray("files");if(files!=null)for(int i=0;i<files.length();i++){JSONObject f=copy(files.getJSONObject(i));f.put("id",f.getString("key")).put("storage",f.optBoolean("sync")?"drive":"local");out.put(f);}e.put("files",out);e.remove("cloud");e.remove("cloudOwner");return e;
 }
 static String sha(byte[] b){try{byte[] d=java.security.MessageDigest.getInstance("SHA-256").digest(b);StringBuilder out=new StringBuilder();for(byte n:d)out.append(String.format(java.util.Locale.ROOT,"%02x",n&255));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
