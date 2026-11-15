package de.mika.promptwerk;
import org.json.*;import java.util.*;
/** Account-scoped bridge for project notes and parked drafts; uses normal revisioned records. */
final class DriveExtras {
 static JSONObject initial(JSONObject ideas,JSONArray parked)throws JSONException{
  return new JSONObject().put("ideas",new JSONObject(ideas.toString())).put("parked",new JSONArray(parked.toString())).put("ids",new JSONObject());
 }
 static boolean extra(JSONObject entry){return entry.optString("section").equals("idea")||entry.optString("section").equals("draft");}
 static JSONArray snapshot(JSONObject state,String owner)throws JSONException{
  JSONArray out=new JSONArray();JSONObject ideas=state.getJSONObject("ideas"),ids=state.getJSONObject("ids");Iterator<String> keys=ideas.keys();
  while(keys.hasNext()){String key=keys.next();String id=ids.optString(key);if(id.isEmpty()){id=UUID.randomUUID().toString();ids.put(key,id);}out.put(entry(id,key,ideas.getString(key),"idea",0,new JSONArray(),owner));}
  JSONArray parked=state.getJSONArray("parked");for(int n=0;n<parked.length();n++){JSONObject draft=parked.getJSONObject(n);String id=draft.optString("syncId");if(id.isEmpty()){id=UUID.randomUUID().toString();draft.put("syncId",id);}long time=draft.optLong("time");String title=draft.optString("title");if(title.isEmpty()){title="Geparkter Entwurf · "+new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.GERMANY).format(new Date(time));draft.put("title",title);}out.put(entry(id,title,draft.optString("text"),"draft",time,draft.optJSONArray("files"),owner));}return out;
 }
 static JSONObject entry(String id,String title,String text,String section,long time,JSONArray files,String owner)throws JSONException{
  JSONObject e=new JSONObject().put("id",id).put("title",title).put("text",text).put("type","prompt").put("project","").put("star",false).put("archived",false).put("section",section).put("extraTime",time).put("files",files==null?new JSONArray():new JSONArray(files.toString()));return DriveMerge.completeEntry(e,owner);
 }
 static void apply(JSONObject state,JSONObject row)throws JSONException{
  JSONObject e=row.getJSONObject("entry");String id=e.getString("id");
  if(e.optString("section").equals("idea")){
   JSONObject ideas=state.getJSONObject("ideas"),ids=state.getJSONObject("ids");String old=null;Iterator<String> it=ids.keys();while(it.hasNext()){String key=it.next();if(ids.getString(key).equals(id)){old=key;break;}}
   if(old!=null){ideas.remove(old);ids.remove(old);}if(row.optBoolean("deleted"))return;
   String key=e.getString("title");if(ideas.has(key))key+=" · "+id.substring(0,Math.min(6,id.length()));
   while(ideas.has(key))key+=" · Kopie";ideas.put(key,e.getString("text"));ids.put(key,id);
  }else if(e.optString("section").equals("draft")){
   JSONArray next=new JSONArray(),parked=state.getJSONArray("parked");for(int n=0;n<parked.length();n++)if(!parked.getJSONObject(n).optString("syncId").equals(id))next.put(parked.getJSONObject(n));
   if(!row.optBoolean("deleted"))next.put(new JSONObject().put("syncId",id).put("text",e.getString("text")).put("title",e.getString("title")).put("time",e.optLong("extraTime")).put("files",e.getJSONArray("files")));state.put("parked",next);
  }
 }
}
