package com.jsirgalaxybase.terminal;

import com.jsirgalaxybase.quest.core.QuestChapterAlignment;
import com.jsirgalaxybase.quest.core.QuestChapterAlignmentRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Bounded Base protocol for a server-derived selected-entry chapter alignment. */
public final class TerminalQuestChapterAlignmentPayload {
    private static final int MAGIC=0x47434131,MAX_ENCODED=8192;
    private final QuestChapterAlignmentRequest request; private final String query,lifecycle; private final int page;
    public TerminalQuestChapterAlignmentPayload(QuestChapterAlignmentRequest request,String query,String lifecycle,int page){if(request==null)throw new IllegalArgumentException("request is required");this.request=request;this.query=bounded(query,96);this.lifecycle=bounded(lifecycle,16);this.page=Math.max(0,Math.min(100000,page));}
    public QuestChapterAlignmentRequest getRequest(){return request;}public String getQuery(){return query;}public String getLifecycle(){return lifecycle;}public int getPage(){return page;}
    public String encode(){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(MAGIC);text(out,request.getChapterId().toString(),36);out.writeInt(request.getVersion());text(out,request.getExpectedHash(),128);text(out,query,96);text(out,lifecycle,16);out.writeInt(page);out.writeByte(request.getAlignment().ordinal());out.writeInt(request.getQuestIds().size());for(UUID id:request.getQuestIds())text(out,id.toString(),36);out.flush();String value=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());if(value.length()>MAX_ENCODED)throw new IllegalArgumentException("alignment payload is too large");return value;}catch(IOException impossible){throw new IllegalStateException(impossible);}}
    public static TerminalQuestChapterAlignmentPayload decode(String value){if(value==null||value.isEmpty()||value.length()>MAX_ENCODED)throw new IllegalArgumentException("invalid alignment payload size");try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(value)));if(in.readInt()!=MAGIC)throw new IllegalArgumentException("unsupported alignment payload");UUID chapter=UUID.fromString(text(in,36));int version=in.readInt();String hash=text(in,128),query=text(in,96),lifecycle=text(in,16);int page=in.readInt(),ordinal=in.readUnsignedByte();QuestChapterAlignment[] modes=QuestChapterAlignment.values();if(ordinal>=modes.length)throw new IllegalArgumentException("unknown alignment mode");int count=in.readInt();if(count<2||count>128)throw new IllegalArgumentException("invalid alignment selection size");List<UUID> ids=new ArrayList<UUID>(count);for(int i=0;i<count;i++)ids.add(UUID.fromString(text(in,36)));if(in.available()!=0)throw new IllegalArgumentException("trailing alignment payload data");return new TerminalQuestChapterAlignmentPayload(new QuestChapterAlignmentRequest(chapter,version,hash,ids,modes[ordinal]),query,lifecycle,page);}catch(IOException|RuntimeException invalid){throw new IllegalArgumentException("invalid chapter alignment payload",invalid);}}
    private static void text(DataOutputStream out,String value,int max)throws IOException{out.writeUTF(bounded(value,max));}private static String text(DataInputStream in,int max)throws IOException{return bounded(in.readUTF(),max);}private static String bounded(String value,int max){String result=value==null?"":value;if(result.length()>max)throw new IllegalArgumentException("text exceeds "+max+" characters");return result;}
}
