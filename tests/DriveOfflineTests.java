package de.mika.promptwerk;

import android.content.Context;
import java.io.File;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

/** Actual disk queue reloads, not a physical Android process/device restart. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@LooperMode(LooperMode.Mode.PAUSED)
public class DriveOfflineTests {
    final String owner = "a".repeat(64), other = "b".repeat(64);
    Context context;
    @Before public void setup() {
        context = RuntimeEnvironment.getApplication();
        File[] files = DriveSync.directory(context).listFiles();
        if (files != null) for (File file : files) assertTrue(file.delete());
    }
    JSONObject entry(String id, String text) throws Exception {
        return new JSONObject().put("id",id).put("title","Offline-Test")
            .put("text",text).put("type","prompt").put("project","")
            .put("star",false).put("archived",false).put("files",new JSONArray())
            .put("cloud",true).put("cloudOwner",owner);
    }
    JSONObject manifest(JSONObject entries) throws Exception {
        return new JSONObject().put("entries",entries).put("deleted",new JSONObject());
    }
    JSONObject row(JSONObject state, String id) throws Exception {
        return state.getJSONObject("records").getJSONObject(id);
    }
    @Test public void offlineChangesAndAttachmentsSurviveDiskReload() throws Exception {
        JSONObject state = DriveSync.state(context,owner), e = entry("one","Offline-Fassung");
        String key = DriveMerge.sha("Anhang".getBytes("UTF-8"));
        e.getJSONArray("files").put(new JSONObject().put("key",key).put("name","test.txt")
            .put("mime","text/plain").put("size",6).put("sync",true));
        DriveMerge.capture(state.getJSONObject("records"),new JSONArray().put(e),owner);
        DriveSync.write(context,owner,state);
        state.getJSONObject("records").remove("one");
        JSONObject reloaded = row(DriveSync.state(context,owner),"one");
        assertTrue(reloaded.getBoolean("dirty"));
        assertEquals("Offline-Fassung",reloaded.getJSONObject("entry").getString("text"));
        assertEquals(key,reloaded.getJSONObject("entry").getJSONArray("files").getJSONObject(0).getString("key"));
    }
    @Test public void lostResponseRetryKeepsOperationAndOriginalPayloadAfterReload() throws Exception {
        JSONObject state = DriveSync.state(context,owner);
        DriveMerge.capture(state.getJSONObject("records"),new JSONArray().put(entry("one","Gesendet")),owner);
        JSONObject flight = DriveMerge.copy(DriveMerge.startFlight(row(state,"one")));
        DriveSync.write(context,owner,state);
        JSONObject loaded = DriveSync.state(context,owner);
        DriveMerge.capture(loaded.getJSONObject("records"),new JSONArray().put(entry("one","Später offline geändert")),owner);
        DriveSync.write(context,owner,loaded);
        JSONObject retry = DriveMerge.startFlight(row(DriveSync.state(context,owner),"one"));
        assertEquals(flight.getString("operation"),retry.getString("operation"));
        assertEquals("Gesendet",retry.getJSONObject("entry").getString("text"));
        assertEquals("Später offline geändert",row(DriveSync.state(context,owner),"one").getJSONObject("entry").getString("text"));
    }
    @Test public void remotePullNeverOverwritesPersistedOfflineChange() throws Exception {
        JSONObject state = DriveSync.state(context,owner);
        DriveMerge.capture(state.getJSONObject("records"),new JSONArray().put(entry("one","Lokal offline")),owner);
        DriveSync.write(context,owner,state);
        JSONObject loaded = DriveSync.state(context,owner);
        DriveMerge.pull(loaded.getJSONObject("records"),manifest(new JSONObject().put("one",entry("one","Webänderung").put("syncVersion",8))),owner);
        DriveSync.write(context,owner,loaded);
        assertEquals("Lokal offline",row(DriveSync.state(context,owner),"one").getJSONObject("entry").getString("text"));
    }
    @Test public void conflictCopyAndRemoteOriginalSurviveAcknowledgementAndReload() throws Exception {
        JSONObject state = DriveSync.state(context,owner);
        DriveMerge.capture(state.getJSONObject("records"),new JSONArray().put(entry("one","Lokal offline")),owner);
        DriveMerge.startFlight(row(state,"one"));
        DriveSync.write(context,owner,state);
        JSONObject loaded = DriveSync.state(context,owner);
        JSONObject remote = new JSONObject().put("one",entry("one","Webänderung").put("syncVersion",2))
            .put("conflict-one",entry("conflict-one","Lokal offline").put("title","Offline-Test · Konfliktkopie").put("syncVersion",3));
        JSONObject reply = new JSONObject().put("document",manifest(remote)).put("id","conflict-one")
            .put("committedVersion",3).put("conflict",true);
        DriveMerge.acknowledge(loaded.getJSONObject("records"),"one",reply,owner);
        DriveMerge.pull(loaded.getJSONObject("records"),reply.getJSONObject("document"),owner);
        DriveSync.write(context,owner,loaded);
        JSONObject reloaded = DriveSync.state(context,owner);
        assertEquals("Webänderung",row(reloaded,"one").getJSONObject("entry").getString("text"));
        assertEquals("Lokal offline",row(reloaded,"conflict-one").getJSONObject("entry").getString("text"));
        assertFalse(row(reloaded,"conflict-one").getBoolean("dirty"));
    }
    @Test public void accountQueuesRemainSeparateOnDisk() throws Exception {
        JSONObject state = DriveSync.state(context,owner);
        DriveMerge.capture(state.getJSONObject("records"),new JSONArray().put(entry("one","Nur A")),owner);
        DriveSync.write(context,owner,state);
        assertEquals(0,DriveSync.state(context,other).getJSONObject("records").length());
        assertEquals("Nur A",row(DriveSync.state(context,owner),"one").getJSONObject("entry").getString("text"));
    }
    @Test public void extrasPersistOfflineAndRemainAccountScoped() throws Exception {
        context.getSharedPreferences("library",0).edit().putString("ideas","{\"Lokal\":\"Original\"}").putBoolean("driveExtras:"+owner,true).commit();
        JSONObject extras=DriveExtras.initial(new JSONObject().put("Projekt","Notiz"),new JSONArray().put(new JSONObject().put("text","Geparkt").put("time",1234).put("files",new JSONArray())));
        DriveSync.saveExtras(context,owner,extras);JSONArray all=DriveSync.withExtras(context,new JSONArray(),owner);assertEquals(2,all.length());
        JSONObject state=DriveSync.state(context,owner);DriveMerge.capture(state.getJSONObject("records"),all,owner);DriveSync.write(context,owner,state);
        assertEquals(2,DriveSync.state(context,owner).getJSONObject("records").length());assertEquals(0,DriveSync.withExtras(context,new JSONArray(),other).length());assertEquals(0,DriveSync.extras(context,other).getJSONObject("ideas").length());assertTrue(context.getSharedPreferences("library",0).getString("ideas","").contains("Original"));
        assertEquals(all.toString(),DriveSync.withExtras(context,new JSONArray(),owner).toString());
    }
    @Test public void consciousConnectionAutomaticallyEnablesExtrasAndMovesLocalDataToOwnerCache() throws Exception {
        context.getSharedPreferences("library",0).edit().putString("ideas","{\"Neu\":\"Notiz\"}").putString("parkedDrafts","[{\"text\":\"Entwurf\",\"files\":[],\"time\":1}]").commit();
        DriveSync.ensureExtras(context,owner);assertTrue(context.getSharedPreferences("library",0).getBoolean("driveExtras:"+owner,false));assertEquals(2,DriveSync.withExtras(context,new JSONArray(),owner).length());assertEquals("{}",context.getSharedPreferences("library",0).getString("ideas",""));
        DriveSync.ensureExtras(context,other);assertEquals(0,DriveSync.withExtras(context,new JSONArray(),other).length());assertEquals("Notiz",DriveSync.extras(context,owner).getJSONObject("ideas").getString("Neu"));
    }
    @Test public void cleanupRetainsAttachmentsFromOtherAccountAndAnUnacknowledgedFlight() throws Exception {
        JSONObject state=DriveSync.state(context,other),e=entry("foreign-extra","Offline").put("cloudOwner",other).put("section","draft");
        e.getJSONArray("files").put(new JSONObject().put("key","old-file").put("name","old.txt").put("mime","text/plain").put("size",3).put("sync",true));
        DriveMerge.capture(state.getJSONObject("records"),new JSONArray().put(e),other);JSONObject r=state.getJSONObject("records").getJSONObject("foreign-extra");DriveMerge.startFlight(r);r.getJSONObject("entry").put("files",new JSONArray());DriveSync.write(context,other,state);
        assertTrue(DriveSync.referencedFiles(context).contains("old-file"));
    }
}
