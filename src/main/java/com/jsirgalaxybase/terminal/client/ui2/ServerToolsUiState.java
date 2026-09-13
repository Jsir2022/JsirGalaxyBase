package com.jsirgalaxybase.terminal.client.ui2;

import com.jsirgalaxybase.terminal.TerminalActionType;
import com.jsirgalaxybase.terminal.TerminalServerToolsActionPayload;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalServerToolsSectionModel;

/** Local navigation and drafts for ServerTools. Transfer truth remains on the dedicated server. */
public final class ServerToolsUiState {
    public enum Tab { WARP, HOME, TPA }
    public enum Field { NONE, HOME, TPA_PLAYER, TPA_SERVER }

    private final Tab tab;
    private final Field field;
    private final String selectedWarp, selectedHome, homeDraft, playerDraft, serverDraft;
    private final boolean helpOpen;
    private final Confirmation confirmation;

    public ServerToolsUiState(Tab tab, Field field, String selectedWarp, String selectedHome,
        String homeDraft, String playerDraft, String serverDraft, boolean helpOpen,
        Confirmation confirmation) {
        this.tab=tab==null?Tab.WARP:tab;this.field=field==null?Field.NONE:field;
        this.selectedWarp=name(selectedWarp);this.selectedHome=name(selectedHome);
        this.homeDraft=name(homeDraft);this.playerDraft=name(playerDraft);this.serverDraft=name(serverDraft);
        this.helpOpen=helpOpen;this.confirmation=confirmation;
    }

    public static ServerToolsUiState initial(TerminalServerToolsSectionModel model){
        TerminalServerToolsSectionModel value=model==null?TerminalServerToolsSectionModel.placeholder():model;
        String home=value.getSelectedHomeName().isEmpty()?"home":value.getSelectedHomeName();
        return new ServerToolsUiState(Tab.WARP,Field.NONE,value.getSelectedWarpName(),value.getSelectedHomeName(),
            home,"",value.getCurrentServerId(),false,null);
    }
    public Tab getTab(){return tab;}public Field getField(){return field;}public String getSelectedWarp(){return selectedWarp;}
    public String getSelectedHome(){return selectedHome;}public String getHomeDraft(){return homeDraft;}
    public String getPlayerDraft(){return playerDraft;}public String getServerDraft(){return serverDraft;}
    public boolean isHelpOpen(){return helpOpen;}public Confirmation getConfirmation(){return confirmation;}
    public boolean hasConfirmation(){return confirmation!=null;}
    public ServerToolsUiState withTab(Tab value){return copy(value,field,selectedWarp,selectedHome,homeDraft,playerDraft,serverDraft,false,null);}
    public ServerToolsUiState withField(Field value){return copy(tab,value,selectedWarp,selectedHome,homeDraft,playerDraft,serverDraft,helpOpen,confirmation);}
    public ServerToolsUiState withValue(Field target,String value){return copy(tab,target,selectedWarp,selectedHome,target==Field.HOME?value:homeDraft,target==Field.TPA_PLAYER?value:playerDraft,target==Field.TPA_SERVER?value:serverDraft,helpOpen,confirmation);}
    public ServerToolsUiState withWarp(String value){return copy(tab,field,value,selectedHome,homeDraft,playerDraft,serverDraft,helpOpen,confirmation);}
    public ServerToolsUiState withHome(String value){return copy(tab,field,selectedWarp,value,value,playerDraft,serverDraft,helpOpen,confirmation);}
    public ServerToolsUiState withHelp(boolean value){return copy(tab,field,selectedWarp,selectedHome,homeDraft,playerDraft,serverDraft,value,null);}
    public ServerToolsUiState withConfirmation(Confirmation value){return copy(tab,field,selectedWarp,selectedHome,homeDraft,playerDraft,serverDraft,false,value);}
    private ServerToolsUiState copy(Tab t,Field f,String w,String h,String hd,String p,String s,boolean help,Confirmation c){return new ServerToolsUiState(t,f,w,h,hd,p,s,help,c);}
    private static String name(String value){String source=value==null?"":value.trim();StringBuilder out=new StringBuilder();for(int i=0;i<source.length()&&out.length()<64;i++){char c=source.charAt(i);if((c>='A'&&c<='Z')||(c>='a'&&c<='z')||(c>='0'&&c<='9')||c=='_'||c=='-')out.append(c);}return out.toString();}

    public static final class Confirmation {
        private final TerminalActionType action;private final TerminalServerToolsActionPayload payload;
        private final String title,detail,confirmLabel;
        public Confirmation(TerminalActionType action,TerminalServerToolsActionPayload payload,String title,String detail,String confirmLabel){this.action=action;this.payload=payload;this.title=title;this.detail=detail;this.confirmLabel=confirmLabel;}
        public TerminalActionType getAction(){return action;}public TerminalServerToolsActionPayload getPayload(){return payload;}
        public String getTitle(){return title;}public String getDetail(){return detail;}public String getConfirmLabel(){return confirmLabel;}
    }
}
