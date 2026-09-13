package com.jsirgalaxybase.terminal.network;

import java.util.ArrayList;
import java.util.List;

import com.jsirgalaxybase.terminal.TerminalTrackedQuestSnapshot;
import com.jsirgalaxybase.terminal.client.TrackedQuestHudState;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class TrackedQuestSnapshotMessage implements IMessage {
    private TerminalTrackedQuestSnapshot snapshot=TerminalTrackedQuestSnapshot.EMPTY;
    public TrackedQuestSnapshotMessage(){}
    public TrackedQuestSnapshotMessage(TerminalTrackedQuestSnapshot snapshot){this.snapshot=snapshot==null?TerminalTrackedQuestSnapshot.EMPTY:snapshot;}
    @Override public void toBytes(ByteBuf buf){buf.writeLong(snapshot.getGeneratedAt());buf.writeInt(snapshot.getQuests().size());for(TerminalTrackedQuestSnapshot.Quest quest:snapshot.getQuests()){ByteBufUtils.writeUTF8String(buf,quest.getId());ByteBufUtils.writeUTF8String(buf,quest.getName());ByteBufUtils.writeUTF8String(buf,quest.getState());buf.writeInt(quest.getTasks().size());for(TerminalTrackedQuestSnapshot.Task task:quest.getTasks()){ByteBufUtils.writeUTF8String(buf,task.getLabel());buf.writeLong(task.getValue());buf.writeLong(task.getTarget());}}}
    @Override public void fromBytes(ByteBuf buf){long generatedAt=Math.max(0L,buf.readLong());int questCount=count(buf.readInt(),5);List<TerminalTrackedQuestSnapshot.Quest> quests=new ArrayList<TerminalTrackedQuestSnapshot.Quest>(questCount);for(int i=0;i<questCount;i++){String id=ByteBufUtils.readUTF8String(buf),name=ByteBufUtils.readUTF8String(buf),state=ByteBufUtils.readUTF8String(buf);int taskCount=count(buf.readInt(),3);List<TerminalTrackedQuestSnapshot.Task> tasks=new ArrayList<TerminalTrackedQuestSnapshot.Task>(taskCount);for(int task=0;task<taskCount;task++)tasks.add(new TerminalTrackedQuestSnapshot.Task(ByteBufUtils.readUTF8String(buf),buf.readLong(),buf.readLong()));quests.add(new TerminalTrackedQuestSnapshot.Quest(id,name,state,tasks));}snapshot=new TerminalTrackedQuestSnapshot(quests,generatedAt);}
    private static int count(int value,int max){if(value<0||value>max)throw new IllegalArgumentException("invalid tracked quest payload count");return value;}
    public static final class Handler implements IMessageHandler<TrackedQuestSnapshotMessage,IMessage>{@Override public IMessage onMessage(TrackedQuestSnapshotMessage message,MessageContext context){TrackedQuestHudState.update(message.snapshot);return null;}}
}
