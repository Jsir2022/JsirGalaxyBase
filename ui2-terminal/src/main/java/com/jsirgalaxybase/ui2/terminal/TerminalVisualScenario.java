package com.jsirgalaxybase.ui2.terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.jsirgalaxybase.ui2.core.UiDocument;
import com.jsirgalaxybase.ui2.theme.GlassTerminalTheme;
import com.jsirgalaxybase.ui2.theme.HackerGreenTheme;
import com.jsirgalaxybase.ui2.theme.HighContrastTheme;
import com.jsirgalaxybase.ui2.theme.UiTheme;

/** Deterministic production-page scenario consumed by Java2D visual verification. */
public final class TerminalVisualScenario {
    public interface DocumentFactory { UiDocument create(); }

    private final String id;
    private final UiTheme theme;
    private final DocumentFactory factory;

    public TerminalVisualScenario(String id, UiTheme theme, DocumentFactory factory) {
        if (id == null || id.trim().isEmpty() || theme == null || factory == null) {
            throw new IllegalArgumentException("scenario id, theme and factory are required");
        }
        this.id = id;
        this.theme = theme;
        this.factory = factory;
    }

    public String getId() { return id; }
    public UiTheme getTheme() { return theme; }
    public UiDocument createDocument() { return factory.create(); }

    public static List<TerminalVisualScenario> v1Defaults() {
        List<TerminalVisualScenario> result = new ArrayList<TerminalVisualScenario>();
        addTheme(result, "glass", GlassTerminalTheme.create());
        addTheme(result, "hacker", HackerGreenTheme.create());
        addTheme(result, "contrast", HighContrastTheme.create());
        return Collections.unmodifiableList(result);
    }

    public static List<TerminalVisualScenario> defaults() {
        List<TerminalVisualScenario> result = new ArrayList<TerminalVisualScenario>(v1Defaults());
        addAsset(result, "glass", GlassTerminalTheme.create());
        addAsset(result, "hacker", HackerGreenTheme.create());
        addAsset(result, "contrast", HighContrastTheme.create());
        addMarket(result, "glass", GlassTerminalTheme.create());
        addMarket(result, "hacker", HackerGreenTheme.create());
        addMarket(result, "contrast", HighContrastTheme.create());
        addQuest(result, "glass", GlassTerminalTheme.create());
        addQuest(result, "hacker", HackerGreenTheme.create());
        addQuest(result, "contrast", HighContrastTheme.create());
        addQuestManagement(result, "glass", GlassTerminalTheme.create());
        addQuestManagement(result, "hacker", HackerGreenTheme.create());
        addQuestManagement(result, "contrast", HighContrastTheme.create());
        addQuestChapterManagement(result, "glass", GlassTerminalTheme.create());
        addQuestChapterManagement(result, "hacker", HackerGreenTheme.create());
        addQuestChapterManagement(result, "contrast", HighContrastTheme.create());
        return Collections.unmodifiableList(result);
    }

    private static void addQuestChapterManagement(List<TerminalVisualScenario> result,final String themeId,final UiTheme theme){
        final QuestChapterManagementVisualModel.Chapter draft=new QuestChapterManagementVisualModel.Chapter(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",2,"银河工业与公共建设","DRAFT","chapter-hash",3);
        final QuestChapterManagementVisualModel.Chapter published=new QuestChapterManagementVisualModel.Chapter(
            "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",1,"跨服职业认证","PUBLISHED","published-hash",2);
        final List<QuestChapterManagementVisualModel.Chapter> rows=Arrays.asList(draft,published);
        result.add(new TerminalVisualScenario("terminal-quest-chapters-list-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestChapterManagementVisualDocument(
            new QuestChapterManagementVisualModel(shellModel(),"","","章节由 PostgreSQL 跨服真源管理。",0,20,2,rows,null),TerminalActionPort.NONE,QuestChapterManagementActionPort.NONE,TerminalWindowProfile.STANDARD,new Runnable(){public void run(){}});}}));
        result.add(new TerminalVisualScenario("terminal-quest-chapters-empty-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestChapterManagementVisualDocument(
            new QuestChapterManagementVisualModel(shellModel(),"不存在","DRAFT","没有符合条件的章节",0,20,0,Collections.<QuestChapterManagementVisualModel.Chapter>emptyList(),null),TerminalActionPort.NONE,QuestChapterManagementActionPort.NONE,TerminalWindowProfile.STANDARD,new Runnable(){public void run(){}});}}));
        result.add(new TerminalVisualScenario("terminal-quest-chapters-create-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestChapterManagementVisualDocument document=new QuestChapterManagementVisualDocument(
            new QuestChapterManagementVisualModel(shellModel(),"","DRAFT","",0,20,2,rows,null),TerminalActionPort.NONE,QuestChapterManagementActionPort.NONE,TerminalWindowProfile.STANDARD,new Runnable(){public void run(){}});document.beginCreateForPreview();return document;}}));
        final QuestChapterManagementVisualModel.Detail detail=new QuestChapterManagementVisualModel.Detail(draft,
            "允许负坐标和较大跨度的章节画布；长中文必须留在详情侧栏内部。","minecraft:book","",256,"NORMAL",
            Arrays.asList(new QuestChapterManagementVisualModel.Entry("quest-steel","钢材公共补给",-48,24,24,24),
                new QuestChapterManagementVisualModel.Entry("quest-boiler","蒸汽锅炉认证",32,-16,32,24),
                new QuestChapterManagementVisualModel.Entry("quest-grid","跨服电网建设",96,56,40,32)),
            Arrays.asList(new QuestChapterManagementVisualModel.QuestCandidate("quest-ae","仓储网络入门"),
                new QuestChapterManagementVisualModel.QuestCandidate("quest-space","太空阶段准备")));
        result.add(new TerminalVisualScenario("terminal-quest-chapters-detail-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestChapterManagementVisualDocument(
            new QuestChapterManagementVisualModel(shellModel(),"","DRAFT","",0,20,2,rows,detail),TerminalActionPort.NONE,QuestChapterManagementActionPort.NONE,TerminalWindowProfile.STANDARD,new Runnable(){public void run(){}});}}));
        result.add(new TerminalVisualScenario("terminal-quest-chapters-editor-text-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestChapterManagementVisualDocument document=new QuestChapterManagementVisualDocument(
            new QuestChapterManagementVisualModel(shellModel(),"","DRAFT","",0,20,2,rows,detail),TerminalActionPort.NONE,QuestChapterManagementActionPort.NONE,TerminalWindowProfile.STANDARD,new Runnable(){public void run(){}});document.beginEditForPreview(0);return document;}}));
        result.add(new TerminalVisualScenario("terminal-quest-chapters-editor-resources-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestChapterManagementVisualDocument document=new QuestChapterManagementVisualDocument(
            new QuestChapterManagementVisualModel(shellModel(),"","DRAFT","",0,20,2,rows,detail),TerminalActionPort.NONE,QuestChapterManagementActionPort.NONE,TerminalWindowProfile.STANDARD,new Runnable(){public void run(){}});document.beginEditForPreview(1);return document;}}));
    }

    private static void addQuestManagement(List<TerminalVisualScenario> result,final String themeId,final UiTheme theme){
        final List<QuestManagementVisualModel.Definition> rows=Arrays.asList(
            new QuestManagementVisualModel.Definition("11111111-1111-1111-1111-111111111111",3,"银河公共钢材长期补给任务","DRAFT","draft-hash",0,5,3),
            new QuestManagementVisualModel.Definition("22222222-2222-2222-2222-222222222222",2,"蒸汽时代毕业认证","PUBLISHED","published-hash",1720000000000L,8,4),
            new QuestManagementVisualModel.Definition("33333333-3333-3333-3333-333333333333",1,"旧版新手引导","RETIRED","retired-hash",1710000000000L,3,1));
        result.add(new TerminalVisualScenario("terminal-quest-management-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"","","任务定义由 PostgreSQL 跨服真源管理。",0,20,3,rows),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);}}));
        result.add(new TerminalVisualScenario("terminal-quest-management-empty-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"不存在的任务","DRAFT","没有符合条件的任务定义",0,20,0,Collections.<QuestManagementVisualModel.Definition>emptyList()),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);}}));
        final java.util.Map<String,String> impactOptions=new java.util.LinkedHashMap<String,String>();impactOptions.put("behavior.visibility","NORMAL");impactOptions.put("impact.direct","2");impactOptions.put("impact.transitive","1");impactOptions.put("impact.placements","1");impactOptions.put("impact.truncated","false");impactOptions.put("impact.safeToRetire","false");impactOptions.put("impact.summary","后续认证、章节 工业起步");
        final QuestManagementVisualModel.Detail detail=new QuestManagementVisualModel.Detail(rows.get(0),"跨服公共建设任务，用于验证定义详情在管理终端中的结构与长文本布局。","AND","AND",Collections.singletonList(rows.get(1).getId()),Arrays.asList(new QuestManagementVisualModel.Element("steel","bq_standard:retrieval",false,Collections.singletonMap("item.count","1")),new QuestManagementVisualModel.Element("visit","bq_standard:location",false,Collections.singletonMap("dimension","0"))),Collections.singletonList(new QuestManagementVisualModel.Element("reward","bq_standard:item",false,Collections.singletonMap("item.count","1"))),impactOptions);
        final List<QuestManagementVisualModel.ElementType> taskTypes=Arrays.asList(new QuestManagementVisualModel.ElementType("bq_standard:retrieval","物品提交","TASK",Arrays.asList(new QuestManagementVisualModel.Field("item","物品列表","ITEM_LIST",true,null,null,Collections.<String>emptyList()),new QuestManagementVisualModel.Field("consume","消耗物品","BOOLEAN",false,null,null,Collections.<String>emptyList()))),new QuestManagementVisualModel.ElementType("bq_standard:location","位置检测","TASK",Arrays.asList(new QuestManagementVisualModel.Field("dimension","维度","LONG",false,null,null,Collections.<String>emptyList()),new QuestManagementVisualModel.Field("range","范围","LONG",true,Long.valueOf(1),null,Collections.<String>emptyList()))));
        final List<QuestManagementVisualModel.ElementType> rewardTypes=Collections.singletonList(new QuestManagementVisualModel.ElementType("bq_standard:item","物品奖励","REWARD",Collections.singletonList(new QuestManagementVisualModel.Field("item","物品列表","ITEM_LIST",true,null,null,Collections.<String>emptyList()))));
        result.add(new TerminalVisualScenario("terminal-quest-management-detail-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"","DRAFT","",0,20,3,rows,detail,taskTypes,rewardTypes),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);}}));
        result.add(new TerminalVisualScenario("terminal-quest-management-editor-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestManagementVisualDocument document=new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"","DRAFT","",0,20,3,rows,detail,taskTypes,rewardTypes),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);document.beginEdit();return document;}}));
        result.add(new TerminalVisualScenario("terminal-quest-management-element-editor-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestManagementVisualDocument document=new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"","DRAFT","",0,20,3,rows,detail,taskTypes,rewardTypes),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);document.beginFirstElementEdit(true);return document;}}));
        result.add(new TerminalVisualScenario("terminal-quest-management-new-task-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestManagementVisualDocument document=new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"","DRAFT","",0,20,3,rows,detail,taskTypes,rewardTypes),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);document.beginNewElementForPreview(true);return document;}}));
        result.add(new TerminalVisualScenario("terminal-quest-management-prerequisites-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestManagementVisualDocument document=new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"","DRAFT","",0,20,3,rows,detail,taskTypes,rewardTypes),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);document.beginPrerequisiteEditForPreview();return document;}}));
        result.add(new TerminalVisualScenario("terminal-quest-management-create-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestManagementVisualDocument document=new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"","DRAFT","",0,20,3,rows),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);document.beginCreateForPreview();return document;}}));
        result.add(new TerminalVisualScenario("terminal-quest-management-behavior-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestManagementVisualDocument document=new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"","DRAFT","",0,20,3,rows,detail,taskTypes,rewardTypes),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);document.beginBehaviorEditForPreview(0);return document;}}));
        result.add(new TerminalVisualScenario("terminal-quest-management-behavior-resources-"+themeId,theme,new DocumentFactory(){public UiDocument create(){QuestManagementVisualDocument document=new QuestManagementVisualDocument(new QuestManagementVisualModel(shellModel(),"","DRAFT","",0,20,3,rows,detail,taskTypes,rewardTypes),TerminalActionPort.NONE,QuestManagementActionPort.NONE,TerminalWindowProfile.STANDARD);document.beginBehaviorEditForPreview(2);return document;}}));
    }

    private static void addQuest(List<TerminalVisualScenario> result, final String themeId, final UiTheme theme) {
        final List<QuestCenterVisualModel.Chapter> chapters=Arrays.asList(
            new QuestCenterVisualModel.Chapter("intro","新手与生存",4,12),
            new QuestCenterVisualModel.Chapter("steam","蒸汽时代",7,18),
            new QuestCenterVisualModel.Chapter("lv","LV 工业",2,24),
            new QuestCenterVisualModel.Chapter("public","银河公共建设",1,8));
        final List<QuestCenterVisualModel.QuestSummary> quests=questSummaries();
        final QuestCenterVisualModel.QuestDetail detail=questDetail(quests.get(1));
        result.add(new TerminalVisualScenario("terminal-quests-browse-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestCenterVisualDocument(
            new QuestCenterVisualModel(shellModel(),QuestCenterVisualModel.View.BROWSE,QuestCenterVisualModel.LoadState.READY,"intro","","all","",chapters,quests,null),TerminalActionPort.NONE,QuestCenterActionPort.NONE,TerminalWindowProfile.STANDARD);}}));
        result.add(new TerminalVisualScenario("terminal-quests-detail-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestCenterVisualDocument(
            new QuestCenterVisualModel(shellModel(),QuestCenterVisualModel.View.DETAIL,QuestCenterVisualModel.LoadState.READY,"intro","steel","active","",chapters,quests,detail),TerminalActionPort.NONE,QuestCenterActionPort.NONE,TerminalWindowProfile.STANDARD);}}));
        result.add(new TerminalVisualScenario("terminal-quests-empty-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestCenterVisualDocument(
            new QuestCenterVisualModel(shellModel(),QuestCenterVisualModel.View.BROWSE,QuestCenterVisualModel.LoadState.EMPTY,"public","","claimable","",chapters,Collections.<QuestCenterVisualModel.QuestSummary>emptyList(),null),TerminalActionPort.NONE,QuestCenterActionPort.NONE,TerminalWindowProfile.STANDARD);}}));
        result.add(new TerminalVisualScenario("terminal-quests-error-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new QuestCenterVisualDocument(
            new QuestCenterVisualModel(shellModel(),QuestCenterVisualModel.View.BROWSE,QuestCenterVisualModel.LoadState.ERROR,"intro","","all","跨服任务数据库暂时不可用，请稍后重试。",chapters,quests,null),TerminalActionPort.NONE,QuestCenterActionPort.NONE,TerminalWindowProfile.STANDARD);}}));
    }

    private static List<QuestCenterVisualModel.QuestSummary> questSummaries(){return Arrays.asList(
        questSummary("wood","取得第一批木材","收集原木并制作工作台",QuestCenterVisualModel.QuestState.COMPLETED,2,2,"minecraft:log"),
        questSummary("steel","跨服钢材补给与公共仓储建设","向银河公共工程交付钢锭；这是一段用于验证极长中英文不会越界的说明",QuestCenterVisualModel.QuestState.IN_PROGRESS,1,3,"minecraft:iron_ingot"),
        questSummary("boiler","第一台蒸汽锅炉","准备燃料、水和安全空间",QuestCenterVisualModel.QuestState.AVAILABLE,0,3,"minecraft:furnace"),
        questSummary("circuit","基础电路组装","进入 LV 工业前置",QuestCenterVisualModel.QuestState.LOCKED,0,4,"minecraft:redstone"),
        questSummary("reward","公共任务奖励","贡献结算已经完成",QuestCenterVisualModel.QuestState.CLAIMABLE,2,2,"minecraft:gold_ingot"));}
    private static QuestCenterVisualModel.QuestSummary questSummary(String id,String name,String subtitle,QuestCenterVisualModel.QuestState state,int done,int total,String registry){return new QuestCenterVisualModel.QuestSummary(id,"intro",name,subtitle,state,done,total,new VisualItem("quest-"+id,registry,0,name,1),"steel".equals(id));}
    private static QuestCenterVisualModel.QuestDetail questDetail(QuestCenterVisualModel.QuestSummary summary){return new QuestCenterVisualModel.QuestDetail(summary,
        "银河公共建设需要持续稳定的钢材供应。进度由服务器事实记录，可以在任意服务器继续。",
        "前置：取得第一批木材","不可重复",Arrays.asList(
            new QuestCenterVisualModel.TaskRow("steel","交付钢锭","物品提交",128,256),
            new QuestCenterVisualModel.TaskRow("visit","到达公共仓库","位置检测",1,1),
            new QuestCenterVisualModel.TaskRow("confirm","确认安全规范","手动确认",0,1)),Arrays.asList(
            new QuestCenterVisualModel.RewardRow("coins","贡献与货币","贡献 120 · 货币 640",null,false,false),
            new QuestCenterVisualModel.RewardRow("item","建设补给箱","任选一种工程物资",new VisualItem("reward","minecraft:chest",0,"建设补给箱",1),false,false,Arrays.asList(
                new QuestCenterVisualModel.ChoiceOption(new VisualItem("choice-steel","minecraft:iron_ingot",0,"钢锭",64),"钢锭"),
                new QuestCenterVisualModel.ChoiceOption(new VisualItem("choice-circuit","minecraft:redstone",0,"电路材料",32),"电路材料")),0)));
    }

    private static void addMarket(List<TerminalVisualScenario> result, final String themeId, final UiTheme theme) {
        final List<MarketVisualModel.Product> products = marketProducts();
        result.add(new TerminalVisualScenario("terminal-market-catalog-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new MarketVisualDocument(new MarketVisualModel(shellModel(),MarketVisualModel.View.CATALOG,products,"iron","24h","最新"),TerminalWindowProfile.STANDARD);}}));
        result.add(new TerminalVisualScenario("terminal-market-detail-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new MarketVisualDocument(new MarketVisualModel(shellModel(),MarketVisualModel.View.DETAIL,products,"iron","24h","最新"),TerminalWindowProfile.STANDARD);}}));
        result.add(new TerminalVisualScenario("terminal-market-single-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new MarketVisualDocument(new MarketVisualModel(shellModel(),MarketVisualModel.View.DETAIL,marketProducts(Collections.singletonList(new MarketVisualModel.Point(1,62,128))),"iron","1h","稀疏"),TerminalWindowProfile.STANDARD);}}));
        result.add(new TerminalVisualScenario("terminal-market-empty-"+themeId,theme,new DocumentFactory(){public UiDocument create(){return new MarketVisualDocument(new MarketVisualModel(shellModel(),MarketVisualModel.View.DETAIL,marketProducts(Collections.<MarketVisualModel.Point>emptyList()),"iron","7d","暂无成交"),TerminalWindowProfile.STANDARD);}}));
    }

    private static List<MarketVisualModel.Product> marketProducts(){return marketProducts(Arrays.asList(new MarketVisualModel.Point(1,62,40),new MarketVisualModel.Point(2,65,128),new MarketVisualModel.Point(3,61,70),new MarketVisualModel.Point(4,67,256),new MarketVisualModel.Point(5,69,92)));}
    private static List<MarketVisualModel.Product> marketProducts(List<MarketVisualModel.Point> points){List<MarketVisualModel.Product> result=new ArrayList<MarketVisualModel.Product>();String[] ids={"iron","steel","gold","copper","silver","aluminium","tin","lead"};String[] labels={"铁锭","钢锭","金锭","铜锭","银锭","铝锭","锡锭","铅锭"};for(int i=0;i<ids.length;i++)result.add(new MarketVisualModel.Product(new VisualItem(ids[i],"minecraft:iron_ingot",i,labels[i],1),69+i*7,i%2==0?3:-2,1280+i*64,points));return result;}

    private static void addAsset(List<TerminalVisualScenario> result, final String themeId, final UiTheme theme) {
        result.add(new TerminalVisualScenario("terminal-assets-" + themeId, theme, new DocumentFactory() {
            @Override public UiDocument create() {
                List<VisualItem> items = Arrays.asList(
                    new VisualItem("cell-0", "minecraft:iron_ingot", 0, "铁锭", 8192),
                    new VisualItem("cell-1", "minecraft:gold_ingot", 0, "金锭", 256),
                    new VisualItem("cell-2", "minecraft:redstone", 0, "红石", 65536));
                return new AssetCenterVisualDocument(new AssetCenterVisualModel(shellModel(),
                    AssetCenterVisualModel.Tab.STORAGE, true, 32768, 65536, 3, 63, 0, 2,
                    false, "", items, Collections.<AssetCenterVisualModel.Activity>emptyList(), false),
                    TerminalActionPort.NONE, AssetCenterActionPort.NONE, TerminalWindowProfile.STANDARD);
            }
        }));
    }

    private static void addTheme(List<TerminalVisualScenario> result, final String themeId,
        final UiTheme theme) {
        result.add(new TerminalVisualScenario("terminal-home-" + themeId, theme,
            new DocumentFactory() {
                @Override public UiDocument create() {
                    return new TerminalHomeVisualDocument(homeModel(), TerminalActionPort.NONE,
                        TerminalWindowProfile.STANDARD);
                }
            }));
        result.add(new TerminalVisualScenario("terminal-settings-" + themeId, theme,
            new DocumentFactory() {
                @Override public UiDocument create() {
                    return new TerminalSettingsVisualDocument(new PreviewSettingsPort(shellModel()),
                        TerminalActionPort.NONE);
                }
            }));
    }

    private static TerminalVisualModel shellModel() {
        return new TerminalVisualModel("银河终端", "home", Arrays.asList(
            new TerminalVisualModel.NavItem("home", "首页", true, true),
            new TerminalVisualModel.NavItem("career", "职业", true, false),
            new TerminalVisualModel.NavItem("public", "公共", true, false),
            new TerminalVisualModel.NavItem("market", "市场", true, false),
            new TerminalVisualModel.NavItem("land", "地产", true, false),
            new TerminalVisualModel.NavItem("travel", "传送", true, false),
            new TerminalVisualModel.NavItem("bank", "银行", true, false),
            new TerminalVisualModel.NavItem("notifications", "通知", true, false),
            new TerminalVisualModel.NavItem("access", "准入", true, false),
            new TerminalVisualModel.NavItem("assets", "资产", true, false)));
    }

    private static HomeVisualModel homeModel() {
        return new HomeVisualModel(shellModel(), "制度总览", "当前玩家制度摘要",
            "制度总览 · 贡献 9,223,372,036,854,775,807",
            Arrays.asList(
                new HomeVisualModel.Section("职业与声望", "后勤见习 / 友善 / 贡献 1280",
                    "下一阶段会把职业等级、资格和制度权限接到同一首页亮点卡中。"),
                new HomeVisualModel.Section("公共任务", "钢材、焦煤、盘点",
                    "公共任务、福利和公共服务入口保持清楚可读，不允许越出卡片。"),
                new HomeVisualModel.Section("市场总览", "钢锭、红穗、建材补给",
                    "标准商品、定制商品与汇率市场都从这里进入独立工作流。"),
                new HomeVisualModel.Section("终端迁移状态", "BANK、MARKET、ASSETS",
                    "极长中英文 mixed content must wrap or ellipsize within the assigned visual region.")));
    }

    private static final class PreviewSettingsPort implements TerminalSettingsPort {
        private final TerminalVisualModel shell;
        private String themeId = "glass_mono";
        private TerminalWindowProfile window = TerminalWindowProfile.STANDARD;
        private boolean reduced;
        private boolean help;

        private PreviewSettingsPort(TerminalVisualModel shell) { this.shell = shell; }
        @Override public TerminalSettingsVisualModel current() {
            return new TerminalSettingsVisualModel(shell, Arrays.asList(
                new TerminalSettingsVisualModel.ThemeChoice("glass_mono", "流动玻璃"),
                new TerminalSettingsVisualModel.ThemeChoice("hacker_green", "黑绿终端"),
                new TerminalSettingsVisualModel.ThemeChoice("high_contrast", "高对比")),
                themeId, window, reduced, help);
        }
        @Override public void selectTheme(String id) { themeId = id; }
        @Override public void selectWindow(TerminalWindowProfile profile) { window = profile; }
        @Override public void toggleMotion() { reduced = !reduced; }
        @Override public void reset() { themeId = "glass_mono"; window = TerminalWindowProfile.STANDARD; reduced = false; }
        @Override public void openHelp() { help = true; }
        @Override public void closeHelp() { help = false; }
    }
}
