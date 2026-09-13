package com.jsirgalaxybase.terminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestDefinitionBatchRetirementRequest;

/** Bounded terminal intent for administrator batch retirement. */
public final class TerminalQuestBatchRetirePayload {
    private static final int MAGIC=0x47524231,MAX_ENCODED=16384,MAX_TARGETS=128;
    private final QuestDefinitionBatchRetirementRequest request;
    private final String query,lifecycle; private final int page;
    public TerminalQuestBatchRetirePayload(QuestDefinitionBatchRetirementRequest request,String query,String lifecycle,int page){
        if(request==null||request.getTargets().size()<2||request.getTargets().size()>MAX_TARGETS)throw new IllegalArgumentException("2 to 128 targets are required");
        this.request=request;this.query=bounded(query,96);this.lifecycle=bounded(lifecycle,16);this.page=Math.max(0,Math.min(100000,page));
    }
    public QuestDefinitionBatchRetirementRequest getRequest(){return request;} public String getQuery(){return query;} public String getLifecycle(){return lifecycle;} public int getPage(){return page;}
    public String encode(){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(MAGIC);out.writeUTF(query);out.writeUTF(lifecycle);out.writeInt(page);out.writeInt(request.getTargets().size());for(QuestDefinitionBatchRetirementRequest.Target target:request.getTargets()){out.writeUTF(target.getQuestId().toString());out.writeInt(target.getVersion());}out.flush();String value=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());if(value.length()>MAX_ENCODED)throw new IllegalArgumentException("batch retire payload is too large");return value;}catch(IOException impossible){throw new IllegalStateException(impossible);}}
    public static TerminalQuestBatchRetirePayload decode(String value){try{if(value==null||value.isEmpty()||value.length()>MAX_ENCODED)throw new IllegalArgumentException("invalid batch retire payload size");DataInputStream in=new DataInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(value)));if(in.readInt()!=MAGIC)throw new IllegalArgumentException("unsupported batch retire payload");String query=bounded(in.readUTF(),96),lifecycle=bounded(in.readUTF(),16);int page=in.readInt(),count=in.readInt();if(count<2||count>MAX_TARGETS)throw new IllegalArgumentException("invalid batch retire target count");List<QuestDefinitionBatchRetirementRequest.Target> targets=new ArrayList<QuestDefinitionBatchRetirementRequest.Target>(count);for(int i=0;i<count;i++)targets.add(new QuestDefinitionBatchRetirementRequest.Target(UUID.fromString(bounded(in.readUTF(),36)),in.readInt()));if(in.available()!=0)throw new IllegalArgumentException("trailing batch retire payload data");return new TerminalQuestBatchRetirePayload(new QuestDefinitionBatchRetirementRequest(targets),query,lifecycle,page);}catch(IOException|RuntimeException invalid){throw new IllegalArgumentException("invalid batch retire payload",invalid);}}
    private static String bounded(String value,int max){String safe=value==null?"":value;if(safe.length()>max)throw new IllegalArgumentException("text exceeds limit");return safe;}
}
