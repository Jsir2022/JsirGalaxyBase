package com.jsirgalaxybase.ui2.terminal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Local, disposable form state derived from the server-provided type descriptor. */
final class QuestElementEditState {
    final boolean task;final String originalKey;String typeId,key;boolean optional;final Map<String,String> parameters;
    QuestManagementVisualModel.ElementType descriptor;final List<QuestManagementVisualModel.ElementType> types;int page,focus=-1,index,total;boolean confirmDelete;
    QuestElementEditState(boolean task,QuestManagementVisualModel.Element value,List<QuestManagementVisualModel.ElementType> types,int index,int total){
        this.task=task;this.types=types;this.index=index;this.total=total;originalKey=value.getKey();key=value.getKey();typeId=value.getTypeId();optional=value.isOptional();parameters=new LinkedHashMap<String,String>(value.getParameters());descriptor=find(typeId);
    }
    QuestElementEditState(boolean task,List<QuestManagementVisualModel.ElementType> types,String suggestedKey,int total){
        if(types==null||types.isEmpty())throw new IllegalArgumentException("element type catalog is empty");this.task=task;this.types=types;this.index=total;this.total=total+1;this.originalKey="";this.key=suggestedKey;this.optional=false;this.parameters=new LinkedHashMap<String,String>();select(types.get(0));
    }
    void cycleType(){if(types.isEmpty())return;int current=types.indexOf(descriptor);select(types.get((current+1)%types.size()));}
    private void select(QuestManagementVisualModel.ElementType value){descriptor=value;typeId=value.getId();parameters.clear();for(QuestManagementVisualModel.Field field:value.getFields()){if("ENUM".equals(field.getType())&&!field.getOptions().isEmpty())parameters.put(field.getKey(),field.getOptions().get(0));else if(field.isRequired()&&(field.getMinimum()!=null||"INTEGER".equals(field.getType())||"LONG".equals(field.getType())))parameters.put(field.getKey(),String.valueOf(field.getMinimum()==null?1:field.getMinimum()));}page=0;}
    private QuestManagementVisualModel.ElementType find(String id){for(QuestManagementVisualModel.ElementType type:types)if(type.getId().equals(id))return type;return null;}
    boolean complete(){if(key.trim().isEmpty()||descriptor==null)return false;for(QuestManagementVisualModel.Field field:descriptor.getFields())if(field.isRequired()&&value(field.getKey()).trim().isEmpty())return false;return true;}
    int pages(){return descriptor==null?1:Math.max(1,(descriptor.getFields().size()+3)/4);}
    String value(String field){String value=parameters.get(field);return value==null?"":value;}
    void value(String field,String value){if(value==null||value.isEmpty())parameters.remove(field);else parameters.put(field,value);}
}
