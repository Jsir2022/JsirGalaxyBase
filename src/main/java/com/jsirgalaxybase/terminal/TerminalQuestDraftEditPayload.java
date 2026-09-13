package com.jsirgalaxybase.terminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jsirgalaxybase.quest.core.QuestDraftEditOperation;
import com.jsirgalaxybase.quest.core.QuestDraftEditRequest;
import com.jsirgalaxybase.quest.core.QuestLogic;
import com.jsirgalaxybase.quest.core.RewardDefinition;
import com.jsirgalaxybase.quest.core.TaskDefinition;

/** Bounded Base-owned transport for one atomic quest draft edit batch. */
public final class TerminalQuestDraftEditPayload {
    private static final int MAGIC=0x47514531,MAX_ENCODED=196608,MAX_OPERATIONS=128,MAX_PARAMETERS=64;
    private final QuestDraftEditRequest request;private final String query,lifecycle;private final int page;
    public TerminalQuestDraftEditPayload(QuestDraftEditRequest request,String query,String lifecycle,int page){
        if(request==null)throw new IllegalArgumentException("request is required");this.request=request;
        this.query=bounded(query,96);this.lifecycle=bounded(lifecycle,16);this.page=Math.max(0,Math.min(100000,page));
    }
    public QuestDraftEditRequest getRequest(){return request;}public String getQuery(){return query;}public String getLifecycle(){return lifecycle;}public int getPage(){return page;}
    public String encode(){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(MAGIC);
            text(out,request.getQuestId().toString(),36);out.writeInt(request.getVersion());text(out,request.getExpectedContentHash(),128);
            text(out,query,96);text(out,lifecycle,16);out.writeInt(page);out.writeInt(request.getOperations().size());
            for(QuestDraftEditOperation operation:request.getOperations())writeOperation(out,operation);out.flush();
            String encoded=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
            if(encoded.length()>MAX_ENCODED)throw new IllegalArgumentException("edit payload is too large");return encoded;
        }catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    public static TerminalQuestDraftEditPayload decode(String value){
        if(value==null||value.isEmpty()||value.length()>MAX_ENCODED)throw new IllegalArgumentException("invalid edit payload size");
        try{byte[] raw=Base64.getUrlDecoder().decode(value);DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));
            if(in.readInt()!=MAGIC)throw new IllegalArgumentException("unsupported edit payload");UUID id=UUID.fromString(text(in,36));int version=in.readInt();String hash=text(in,128);
            String query=text(in,96),lifecycle=text(in,16);int page=in.readInt();int count=in.readInt();if(count<1||count>MAX_OPERATIONS)throw new IllegalArgumentException("invalid edit operation count");
            List<QuestDraftEditOperation> operations=new ArrayList<QuestDraftEditOperation>(count);for(int i=0;i<count;i++)operations.add(readOperation(in));
            if(in.available()!=0)throw new IllegalArgumentException("trailing edit payload data");return new TerminalQuestDraftEditPayload(new QuestDraftEditRequest(id,version,hash,operations),query,lifecycle,page);
        }catch(IOException|RuntimeException invalid){throw new IllegalArgumentException("invalid quest edit payload",invalid);}
    }
    private static void writeOperation(DataOutputStream out,QuestDraftEditOperation operation)throws IOException{
        out.writeByte(operation.getKind().ordinal());switch(operation.getKind()){
            case SET_NAME:text(out,operation.getText(),256);break;case SET_DESCRIPTION:text(out,operation.getText(),4096);break;
            case SET_PREREQUISITE_LOGIC:case SET_TASK_LOGIC:text(out,operation.getText(),8);break;
            case SET_OPTION:text(out,operation.getKey(),64);text(out,operation.getText(),256);break;
            case ADD_PREREQUISITE:case REMOVE_PREREQUISITE:text(out,operation.getQuestId().toString(),36);break;
            case ADD_TASK:writeTask(out,operation.getTask());break;case REPLACE_TASK:text(out,operation.getKey(),64);writeTask(out,operation.getTask());break;
            case REMOVE_TASK:text(out,operation.getKey(),64);break;case MOVE_TASK:text(out,operation.getKey(),64);out.writeInt(operation.getIndex());break;
            case ADD_REWARD:writeReward(out,operation.getReward());break;case REPLACE_REWARD:text(out,operation.getKey(),64);writeReward(out,operation.getReward());break;
            case REMOVE_REWARD:text(out,operation.getKey(),64);break;case MOVE_REWARD:text(out,operation.getKey(),64);out.writeInt(operation.getIndex());break;
            default:throw new IllegalArgumentException("unsupported operation");
        }
    }
    private static QuestDraftEditOperation readOperation(DataInputStream in)throws IOException{int ordinal=in.readUnsignedByte();QuestDraftEditOperation.Kind[] kinds=QuestDraftEditOperation.Kind.values();if(ordinal>=kinds.length)throw new IllegalArgumentException("unknown edit operation");switch(kinds[ordinal]){
        case SET_NAME:return QuestDraftEditOperation.name(text(in,256));case SET_DESCRIPTION:return QuestDraftEditOperation.description(text(in,4096));
        case SET_PREREQUISITE_LOGIC:return QuestDraftEditOperation.prerequisiteLogic(QuestLogic.valueOf(text(in,8)));case SET_TASK_LOGIC:return QuestDraftEditOperation.taskLogic(QuestLogic.valueOf(text(in,8)));
        case SET_OPTION:return QuestDraftEditOperation.option(text(in,64),text(in,256));
        case ADD_PREREQUISITE:return QuestDraftEditOperation.addPrerequisite(UUID.fromString(text(in,36)));case REMOVE_PREREQUISITE:return QuestDraftEditOperation.removePrerequisite(UUID.fromString(text(in,36)));
        case ADD_TASK:return QuestDraftEditOperation.addTask(readTask(in));case REPLACE_TASK:return QuestDraftEditOperation.replaceTask(text(in,64),readTask(in));case REMOVE_TASK:return QuestDraftEditOperation.removeTask(text(in,64));case MOVE_TASK:return QuestDraftEditOperation.moveTask(text(in,64),in.readInt());
        case ADD_REWARD:return QuestDraftEditOperation.addReward(readReward(in));case REPLACE_REWARD:return QuestDraftEditOperation.replaceReward(text(in,64),readReward(in));case REMOVE_REWARD:return QuestDraftEditOperation.removeReward(text(in,64));case MOVE_REWARD:return QuestDraftEditOperation.moveReward(text(in,64),in.readInt());default:throw new IllegalArgumentException("unsupported operation");}}
    private static void writeTask(DataOutputStream out,TaskDefinition task)throws IOException{text(out,task.getKey(),64);text(out,task.getTypeId(),128);out.writeBoolean(task.isOptional());writeParameters(out,task.getParameters());}
    private static TaskDefinition readTask(DataInputStream in)throws IOException{return new TaskDefinition(text(in,64),text(in,128),in.readBoolean(),readParameters(in));}
    private static void writeReward(DataOutputStream out,RewardDefinition reward)throws IOException{text(out,reward.getKey(),64);text(out,reward.getTypeId(),128);writeParameters(out,reward.getParameters());}
    private static RewardDefinition readReward(DataInputStream in)throws IOException{return new RewardDefinition(text(in,64),text(in,128),readParameters(in));}
    private static void writeParameters(DataOutputStream out,Map<String,String> values)throws IOException{if(values.size()>MAX_PARAMETERS)throw new IllegalArgumentException("too many parameters");out.writeInt(values.size());for(Map.Entry<String,String> entry:values.entrySet()){text(out,entry.getKey(),128);text(out,entry.getValue(),2048);}}
    private static Map<String,String> readParameters(DataInputStream in)throws IOException{int count=in.readInt();if(count<0||count>MAX_PARAMETERS)throw new IllegalArgumentException("invalid parameter count");Map<String,String> result=new LinkedHashMap<String,String>();for(int i=0;i<count;i++){String key=text(in,128);if(result.put(key,text(in,2048))!=null)throw new IllegalArgumentException("duplicate parameter");}return result;}
    private static void text(DataOutputStream out,String value,int max)throws IOException{String safe=bounded(value,max);out.writeUTF(safe);}
    private static String text(DataInputStream in,int max)throws IOException{return bounded(in.readUTF(),max);}
    private static String bounded(String value,int max){String safe=value==null?"":value;if(safe.length()>max)throw new IllegalArgumentException("text exceeds "+max+" characters");return safe;}
}
