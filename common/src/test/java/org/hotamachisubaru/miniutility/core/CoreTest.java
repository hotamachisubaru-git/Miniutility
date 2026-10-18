package org.hotamachisubaru.miniutility.core;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class CoreTest {
 @TempDir Path directory;
 @Test void profilesSurviveRestartAndReset() throws Exception {
  Path file=directory.resolve("profiles.properties"); UUID id=UUID.randomUUID();
  ProfileStore store=new ProfileStore(file);
  assertTrue(store.prefix(id)); assertTrue(store.protectedBlocks());
  store.setNickname(id,"&aすばる"); store.prefix(id,false); store.protectedBlocks(false);
  ProfileStore restarted=new ProfileStore(file);
  assertEquals("&aすばる",restarted.nickname(id,"original")); assertFalse(restarted.prefix(id)); assertFalse(restarted.protectedBlocks());
  restarted.setNickname(id,null);
  assertEquals("original",new ProfileStore(file).nickname(id,"original"));
 }
 @Test void failedWriteKeepsPreviousState() throws Exception {
  Path file=directory.resolve("profiles.properties"); UUID id=UUID.randomUUID();
  ProfileStore store=new ProfileStore(file); store.setNickname(id,"old");
  Files.delete(file); Files.createDirectory(file); Files.writeString(file.resolve("block"),"x");
  assertThrows(java.io.IOException.class,()->store.setNickname(id,"new"));
  assertEquals("old",store.nickname(id,"original"));
 }
 @Test void invalidReloadDoesNotReplaceLiveData() throws Exception {
  Path file=directory.resolve("profiles.properties"); UUID id=UUID.randomUUID();
  ProfileStore store=new ProfileStore(file); store.setNickname(id,"old");
  Files.writeString(file,"schema=999"); assertThrows(java.io.IOException.class,store::reload);
  assertEquals("old",store.nickname(id,"original"));
 }
 @Test void staleInputCannotFinishReplacementSession() {
  InputSessions inputs=new InputSessions(); UUID id=UUID.randomUUID();
  var old=inputs.begin(id,InputSessions.Mode.NICKNAME);
  var current=inputs.begin(id,InputSessions.Mode.NICKNAME);
  assertFalse(inputs.current(id,old)); inputs.complete(id,old); assertSame(current,inputs.get(id));
  inputs.complete(id,current); assertNull(inputs.get(id));
 }
 @Test void logoutDiscardsPendingInput() {
  InputSessions inputs=new InputSessions(); UUID id=UUID.randomUUID();
  inputs.begin(id,InputSessions.Mode.EXPERIENCE); inputs.forget(id); assertNull(inputs.get(id));
 }
 @Test void nicknameRulesRejectFormattingInjection() {
  assertTrue(NicknameRules.valid("&aすばる_123",true)); assertFalse(NicknameRules.valid("&aすばる",false));
  assertFalse(NicknameRules.valid("&a",true)); assertFalse(NicknameRules.valid("bad\nname",true));
  assertFalse(NicknameRules.valid("a".repeat(17),true)); assertFalse(NicknameRules.valid("&xname",true));
 }
 @Test void experienceCannotOverflowOrGoNegative() {
  assertEquals(-10,NicknameRules.experienceDelta(10,Integer.MIN_VALUE));
  assertEquals(3,NicknameRules.experienceDelta(Integer.MAX_VALUE-3,100));
  assertEquals(-5,NicknameRules.experienceDelta(10,-5));
 }
}
