package de.mika.promptwerk;
import java.io.*;import java.util.*;import org.json.*;
public class PolicyTests {
 static int passed=0;interface Job{void run()throws Exception;}static void ok(boolean v){if(!v)throw new AssertionError();passed++;}static void reject(Job j)throws Exception{try{j.run();throw new AssertionError("Should reject");}catch(IOException expected){passed++;}}
 public static void main(String[] args)throws Exception{
 String key=String.join("",Collections.nCopies(64,"a"));ok(AttachmentPolicy.validKey(key));for(String s:new String[]{"../secret","/etc/passwd",key+"/x","",key.toUpperCase(),"%2e%2e"})ok(!AttachmentPolicy.validKey(s));
 for(String m:new String[]{"image/jpeg","image/png","image/heic","application/pdf","text/plain"})ok(AttachmentPolicy.allowed(m));for(String m:new String[]{"text/html","application/zip","application/javascript","image/svg+xml"})ok(!AttachmentPolicy.allowed(m));
 AttachmentPolicy.check(10,10,AttachmentPolicy.TOTAL_LIMIT);passed++;reject(()->AttachmentPolicy.check(11,0,0));reject(()->AttachmentPolicy.check(0,11,0));reject(()->AttachmentPolicy.check(0,0,AttachmentPolicy.TOTAL_LIMIT+1));
 byte[] raw=new byte[40001];new Random(17).nextBytes(raw);ByteArrayOutputStream out=new ByteArrayOutputStream();ok(AttachmentPolicy.copy(new ByteArrayInputStream(raw),out,raw.length)==raw.length);ok(Arrays.equals(raw,out.toByteArray()));reject(()->AttachmentPolicy.copy(new ByteArrayInputStream(raw),new ByteArrayOutputStream(),40000));
 ArrayList<MainActivity.Att> files=new ArrayList<>();for(int i=0;i<10;i++)files.add(new MainActivity.Att(key,"Bild"+i+".png","image/png",123));JSONArray a=MainActivity.attJson(files);ok(MainActivity.readAtt(a).size()==10);files.add(new MainActivity.Att(key,"elf.png","image/png",1));reject(()->MainActivity.readAtt(MainActivity.attJson(files)));JSONObject corrupted=a.getJSONObject(0);corrupted.put("key","../secret");reject(()->MainActivity.readAtt(a));corrupted.put("key",key);corrupted.put("size",-1);reject(()->MainActivity.readAtt(a));corrupted.put("size",AttachmentPolicy.FILE_LIMIT+1);reject(()->MainActivity.readAtt(a));
 MainActivity.Item item=new MainActivity.Item("Alt","Text","prompt");JSONObject legacy=item.json();legacy.remove("files");ok(MainActivity.readAtt(legacy.optJSONArray("files")).isEmpty());System.out.println(passed+" attachment/metadata/bounds checks passed");
 }
}
