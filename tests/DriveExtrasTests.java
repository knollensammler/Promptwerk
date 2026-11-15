package de.mika.promptwerk;
import org.junit.Test;import static org.junit.Assert.*;import org.json.*;
import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @LooperMode(LooperMode.Mode.PAUSED)
public class DriveExtrasTests {
 final String owner="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
 JSONObject row(JSONObject e,boolean deleted)throws Exception{return new JSONObject().put("entry",e).put("deleted",deleted);}
 @Test public void snapshotIsStableAndKeepsOriginals()throws Exception{
  JSONObject ideas=new JSONObject().put("Projekt","Notiz");JSONArray parked=new JSONArray().put(new JSONObject().put("text","Entwurf").put("time",1000).put("files",new JSONArray().put(new JSONObject().put("key","a").put("sync",false))));
  JSONObject state=DriveExtras.initial(ideas,parked);String first=DriveExtras.snapshot(state,owner).toString();assertEquals(first,DriveExtras.snapshot(state,owner).toString());assertFalse(parked.getJSONObject(0).has("syncId"));assertFalse(parked.getJSONObject(0).getJSONArray("files").getJSONObject(0).getBoolean("sync"));
  JSONArray all=DriveExtras.snapshot(state,owner);assertEquals(2,all.length());assertEquals("idea",all.getJSONObject(0).getString("section"));assertTrue(all.getJSONObject(1).getJSONArray("files").getJSONObject(0).getBoolean("sync"));
 }
 @Test public void remoteChangesAndDeletionReachNotesWithoutDuplicates()throws Exception{
  JSONObject s=DriveExtras.initial(new JSONObject().put("Projekt","lokal"),new JSONArray());JSONObject e=DriveExtras.snapshot(s,owner).getJSONObject(0);e.put("title","Umbenannt").put("text","Web");DriveExtras.apply(s,row(e,false));DriveExtras.apply(s,row(e,false));assertEquals(1,s.getJSONObject("ideas").length());assertEquals("Web",s.getJSONObject("ideas").getString("Umbenannt"));DriveExtras.apply(s,row(e,true));assertEquals(0,s.getJSONObject("ideas").length());
 }
 @Test public void conflictsKeepBothNotesAndAccountBundlesStaySeparate()throws Exception{
  JSONObject a=DriveExtras.initial(new JSONObject().put("Projekt","A"),new JSONArray()),b=DriveExtras.initial(new JSONObject(),new JSONArray());JSONObject e=DriveExtras.snapshot(a,owner).getJSONObject(0);DriveExtras.apply(a,row(new JSONObject(e.toString()).put("id","conflict").put("text","B"),false));assertEquals(2,a.getJSONObject("ideas").length());assertEquals(0,b.getJSONObject("ideas").length());
 }
 @Test public void parkedEditsDeletionAndWireRoundTripPreserveKind()throws Exception{
  JSONObject s=DriveExtras.initial(new JSONObject(),new JSONArray().put(new JSONObject().put("text","vorher").put("time",1000).put("files",new JSONArray())));JSONObject e=DriveExtras.snapshot(s,owner).getJSONObject(0);JSONObject wire=DriveMerge.wire(e),round=DriveMerge.fromWire(wire,owner);assertEquals("draft",round.getString("section"));assertEquals(1000,round.getLong("extraTime"));round.put("text","nachher");DriveExtras.apply(s,row(round,false));assertEquals("nachher",s.getJSONArray("parked").getJSONObject(0).getString("text"));DriveExtras.apply(s,row(round,true));assertEquals(0,s.getJSONArray("parked").length());
 }
 @Test public void extrasUseSameOfflineConflictQueue()throws Exception{
  JSONObject s=DriveExtras.initial(new JSONObject().put("Projekt","A"),new JSONArray()),records=new JSONObject();JSONArray a=DriveExtras.snapshot(s,owner);DriveMerge.capture(records,a,owner);String id=a.getJSONObject(0).getString("id");JSONObject r=records.getJSONObject(id);DriveMerge.startFlight(r);s.getJSONObject("ideas").put("Projekt","Bearbeitung während Übertragung");DriveMerge.capture(records,DriveExtras.snapshot(s,owner),owner);JSONObject remote=new JSONObject(a.getJSONObject(0).toString()).put("id","copy").put("title","Projekt · Konfliktkopie").put("syncVersion",3);JSONObject result=new JSONObject().put("id","copy").put("conflict",true).put("committedVersion",3).put("document",new JSONObject().put("entries",new JSONObject().put("copy",remote)));DriveMerge.acknowledge(records,id,result,owner);assertTrue(records.getJSONObject("copy").getBoolean("dirty"));assertEquals("Bearbeitung während Übertragung",records.getJSONObject("copy").getJSONObject("entry").getString("text"));assertEquals("idea",records.getJSONObject("copy").getJSONObject("entry").getString("section"));
 }
}
