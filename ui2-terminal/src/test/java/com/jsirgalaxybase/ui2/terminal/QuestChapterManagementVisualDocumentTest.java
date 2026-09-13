package com.jsirgalaxybase.ui2.terminal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.Test;

import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiRuntime;
import com.jsirgalaxybase.ui2.core.UiNode;
import com.jsirgalaxybase.ui2.geometry.UiRect;
import com.jsirgalaxybase.ui2.input.UiEvent;
import com.jsirgalaxybase.ui2.input.UiKeyCode;
import com.jsirgalaxybase.ui2.debug.UiDebugSnapshot;
import com.jsirgalaxybase.ui2.motion.FixedUiClock;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.theme.HackerGreenTheme;

public class QuestChapterManagementVisualDocumentTest {
    @Test
    public void chapterListAndSpatialDetailStayInsideAllSupportedViewports() {
        for (QuestChapterManagementVisualModel model : Arrays.asList(list(), detail())) {
            for (int[] size : new int[][] {{350, 193}, {427, 240}, {620, 340}}) {
                UiRuntime runtime = new UiRuntime(new QuestChapterManagementVisualDocument(model,
                    TerminalActionPort.NONE, QuestChapterManagementActionPort.NONE, TerminalWindowProfile.STANDARD,
                    new Runnable() { public void run() {} }), new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F,
                        new FixedUiClock(0L, true)));
                runtime.setViewport(size[0], size[1]);
                DrawList draw = runtime.frame();
                String snapshot = UiDebugSnapshot.capture(runtime.getRoot(), draw, size[0], size[1]);
                assertTrue(snapshot, snapshot.contains("SUMMARY|issues=0"));
                assertTrue(snapshot, snapshot.contains(model.getDetail() == null ? "chapter-admin" : "chapter-layout-canvas"));
                runtime.close();
            }
        }
    }

    @Test public void draftBasicEditorSubmitsAllServerOwnedSemanticFields() {
        final Saved saved = new Saved();
        QuestChapterManagementVisualDocument document = new QuestChapterManagementVisualDocument(detail(),
            TerminalActionPort.NONE, saved, TerminalWindowProfile.STANDARD, new Runnable() { public void run() {} });
        UiRuntime runtime = new UiRuntime(document, new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F,
            new FixedUiClock(0L, true)));
        runtime.setViewport(427, 240); runtime.frame(); click(runtime, "chapter-detail-edit"); runtime.frame();
        click(runtime, "chapter-editor-save");
        assertEquals("chapter-a", saved.id); assertEquals(1, saved.version); assertEquals("hash", saved.hash);
        assertEquals("工业起步", saved.name); assertEquals("通过公共任务建立第一条生产链。", saved.description);
        assertEquals("minecraft:iron_ingot", saved.icon); assertEquals("galaxy:textures/chapter.png", saved.background);
        assertEquals(256, saved.backgroundSize); assertEquals("NORMAL", saved.visibility); runtime.close();
    }

    @Test public void draftCanvasDragCapturesPointerAndSubmitsSemanticMove() {
        Saved saved = new Saved(); QuestChapterManagementVisualDocument document = new QuestChapterManagementVisualDocument(
            detail(), TerminalActionPort.NONE, saved, TerminalWindowProfile.STANDARD, new Runnable() { public void run() {} });
        UiRuntime runtime = new UiRuntime(document, new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F,
            new FixedUiClock(0L, true))); runtime.setViewport(620, 340); runtime.frame();
        UiRect canvas = find(runtime.getRoot(), "chapter-layout-canvas").getBounds();
        int x = canvas.getX() + 7, y = canvas.getY() + 7 + (int)Math.round(24D * Math.min((canvas.getWidth()-8D)/160D,(canvas.getHeight()-8D)/152D));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,x,y,0)));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_MOVE,x+18,y+11,0)));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_UP,x+18,y+11,0)));
        assertEquals("q1",saved.movedQuest);assertTrue(saved.movedX>-32);assertTrue(saved.movedY>0);runtime.close();
    }

    @Test public void candidateSearchSelectsAnyMatchingQuestInsteadOfOnlyFirst() {
        Saved saved = new Saved(); QuestChapterManagementVisualDocument document = new QuestChapterManagementVisualDocument(
            detail(), TerminalActionPort.NONE, saved, TerminalWindowProfile.STANDARD, new Runnable() { public void run() {} });
        UiRuntime runtime = new UiRuntime(document, new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F,
            new FixedUiClock(0L, true))); runtime.setViewport(620,340);runtime.frame();click(runtime,"chapter-candidate-query");
        for(char value:"太空".toCharArray())assertTrue(runtime.dispatch(UiEvent.text(value)));runtime.frame();
        click(runtime,"chapter-candidate-q8");assertEquals("q8",saved.placedQuest);runtime.close();
    }

    @Test public void rightDragResizesThroughSemanticAction() {
        Saved saved = new Saved(); QuestChapterManagementVisualDocument document = new QuestChapterManagementVisualDocument(
            detail(), TerminalActionPort.NONE, saved, TerminalWindowProfile.STANDARD, new Runnable() { public void run() {} });
        UiRuntime runtime = new UiRuntime(document, new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F,
            new FixedUiClock(0L, true)));runtime.setViewport(620,340);runtime.frame();UiRect canvas=find(runtime.getRoot(),"chapter-layout-canvas").getBounds();
        int x=canvas.getX()+7,y=canvas.getY()+7+(int)Math.round(24D*Math.min((canvas.getWidth()-8D)/160D,(canvas.getHeight()-8D)/152D));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,x,y,1)));assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_MOVE,x+16,y+12,1)));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_UP,x+16,y+12,1)));assertEquals("q1",saved.resizedQuest);assertTrue(saved.resizedWidth>24);assertTrue(saved.resizedHeight>24);runtime.close();
    }

    @Test public void canvasDrawsGridAndSupportsCursorAnchoredZoomAndMiddlePan() {
        QuestChapterManagementVisualDocument document = new QuestChapterManagementVisualDocument(detail(),
            TerminalActionPort.NONE, QuestChapterManagementActionPort.NONE, TerminalWindowProfile.STANDARD,
            new Runnable() { public void run() {} });
        UiRuntime runtime = new UiRuntime(document, new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F,
            new FixedUiClock(0L, true))); runtime.setViewport(620, 340); DrawList before = runtime.frame();
        int grids = 0; for (DrawCommand command : before.ordered()) if (command.getKind() == DrawCommand.Kind.LINE) grids++;
        assertTrue("chapter canvas should expose an adaptive grid", grids >= 4);
        UiRect canvas = find(runtime.getRoot(), "chapter-layout-canvas").getBounds();
        int x = canvas.getX() + canvas.getWidth() / 2, y = canvas.getY() + canvas.getHeight() / 2;
        assertTrue(runtime.dispatch(UiEvent.scroll(x, y, 1))); runtime.frame();
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, x, y, 2)));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_MOVE, x + 13, y + 9, 2)));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_UP, x + 13, y + 9, 2)));
        String snapshot = UiDebugSnapshot.capture(runtime.getRoot(), runtime.frame(), 620, 340);
        assertTrue(snapshot, snapshot.contains("SUMMARY|issues=0")); runtime.close();
    }

    @Test public void canvasDrawsOnlyServerProjectedInternalDependencyConnectors() {
        QuestChapterManagementVisualModel.Chapter row = new QuestChapterManagementVisualModel.Chapter(
            "chapter-a", 1, "工业起步", "DRAFT", "hash", 2);
        QuestChapterManagementVisualModel.Detail detail = new QuestChapterManagementVisualModel.Detail(row, "", "", "", 256,
            "NORMAL", Arrays.asList(new QuestChapterManagementVisualModel.Entry("q1", "前置", 0, 0, 24, 24),
                new QuestChapterManagementVisualModel.Entry("q2", "后续", 64, 32, 24, 24)), Collections.<QuestChapterManagementVisualModel.QuestCandidate>emptyList(),
            Arrays.asList(new QuestChapterManagementVisualModel.Dependency("q1", "q2", "INTERNAL"),
                new QuestChapterManagementVisualModel.Dependency("outside", "q2", "EXTERNAL")));
        QuestChapterManagementVisualDocument document = new QuestChapterManagementVisualDocument(new QuestChapterManagementVisualModel(shell(), "", "DRAFT", "", 0, 20, 1,
            Collections.singletonList(row), detail), TerminalActionPort.NONE, QuestChapterManagementActionPort.NONE, TerminalWindowProfile.STANDARD, new Runnable() { public void run() {} });
        UiRuntime runtime = new UiRuntime(document, new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0L, true)));
        runtime.setViewport(620, 340); DrawList draw = runtime.frame(); int lines = 0;
        for (DrawCommand command : draw.ordered()) if (command.getKind() == DrawCommand.Kind.LINE) lines++;
        assertTrue("one internal edge contributes three orthogonal segments beyond the grid", lines >= 7); runtime.close();
    }

    @Test public void sideInspectorAlwaysReservesDependencyDiagnosticSpace() {
        UiRuntime runtime = new UiRuntime(new QuestChapterManagementVisualDocument(detail(), TerminalActionPort.NONE,
            QuestChapterManagementActionPort.NONE, TerminalWindowProfile.STANDARD, new Runnable() { public void run() {} }),
            new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0L, true)));
        runtime.setViewport(350, 193); runtime.frame();
        assertTrue(find(runtime.getRoot(), "chapter-detail-dependencies").getBounds().getHeight() > 0); runtime.close();
    }

    @Test public void listReorderButtonsEmitRelativeServerIntent() {
        QuestChapterManagementVisualModel.Chapter first = new QuestChapterManagementVisualModel.Chapter("first", 1, "第一章", "DRAFT", "h1", 0);
        QuestChapterManagementVisualModel.Chapter second = new QuestChapterManagementVisualModel.Chapter("second", 1, "第二章", "DRAFT", "h2", 0);
        Saved saved = new Saved(); UiRuntime runtime = new UiRuntime(new QuestChapterManagementVisualDocument(
            new QuestChapterManagementVisualModel(shell(), "", "DRAFT", "", 0, 20, 2, Arrays.asList(first, second), null),
            TerminalActionPort.NONE, saved, TerminalWindowProfile.STANDARD, new Runnable() { public void run() {} }),
            new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F, new FixedUiClock(0L, true)));
        runtime.setViewport(620, 340); runtime.frame(); click(runtime, "chapter-admin-down-0");
        assertEquals("second", saved.movedChapter); assertEquals("first", saved.beforeChapter); runtime.close();
    }

    @Test public void arrowKeyMovesSelectedDraftQuestByOneLogicalUnit() {
        Saved saved = new Saved(); QuestChapterManagementVisualDocument document = new QuestChapterManagementVisualDocument(
            detail(), TerminalActionPort.NONE, saved, TerminalWindowProfile.STANDARD, new Runnable() { public void run() {} });
        UiRuntime runtime = new UiRuntime(document, new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F,
            new FixedUiClock(0L, true))); runtime.setViewport(620, 340); runtime.frame();
        UiRect canvas = find(runtime.getRoot(), "chapter-layout-canvas").getBounds();
        int x = canvas.getX() + 7, y = canvas.getY() + 7
            + (int)Math.round(24D * Math.min((canvas.getWidth() - 8D) / 160D, (canvas.getHeight() - 8D) / 152D));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, x, y, 0)));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_UP, x, y, 0)));
        assertTrue(runtime.dispatch(UiEvent.key(UiKeyCode.RIGHT)));
        assertEquals("q1", saved.movedQuest); assertEquals(-31, saved.movedX); assertEquals(0, saved.movedY);
        runtime.close();
    }

    @Test public void selectedDraftEntryExposesBoundedCopyAction() {
        Saved saved = new Saved(); QuestChapterManagementVisualDocument document = new QuestChapterManagementVisualDocument(
            detail(), TerminalActionPort.NONE, saved, TerminalWindowProfile.STANDARD, new Runnable() { public void run() {} });
        UiRuntime runtime = new UiRuntime(document, new UiContext(HackerGreenTheme.create(), Locale.CHINA, 1F,
            new FixedUiClock(0L, true))); runtime.setViewport(620, 340); runtime.frame();
        UiRect canvas = find(runtime.getRoot(), "chapter-layout-canvas").getBounds();
        int x = canvas.getX() + 7, y = canvas.getY() + 7
            + (int)Math.round(24D * Math.min((canvas.getWidth()-8D)/160D,(canvas.getHeight()-8D)/152D));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, x, y, 0)));
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_UP, x, y, 0))); runtime.frame();
        click(runtime, "chapter-detail-copy");
        assertEquals("chapter-a", saved.cloneChapter); assertEquals(Collections.singletonList("q1"), saved.cloneSources);
        assertTrue(saved.cloneX > -32); assertTrue(saved.cloneY > 0); runtime.close();
    }

    @Test public void multiSelectionExposesServerDerivedAlignmentIntent() {
        Saved saved=new Saved();UiRuntime runtime=new UiRuntime(new QuestChapterManagementVisualDocument(detail(),
            TerminalActionPort.NONE,saved,TerminalWindowProfile.STANDARD,new Runnable(){public void run(){}}),
            new UiContext(HackerGreenTheme.create(),Locale.CHINA,1F,new FixedUiClock(0L,true)));
        runtime.setViewport(620,340);runtime.frame();click(runtime,"chapter-detail-select-mode");runtime.frame();
        UiRect canvas=find(runtime.getRoot(),"chapter-layout-canvas").getBounds();double scale=Math.min((canvas.getWidth()-8D)/160D,(canvas.getHeight()-8D)/152D);
        selectCanvas(runtime,canvas,scale,-20,12);runtime.frame();selectCanvas(runtime,canvas,scale,48,36);runtime.frame();
        click(runtime,"chapter-align-center-x");assertEquals("CENTER_X",saved.alignment);assertEquals(Arrays.asList("q1","q2"),saved.alignedIds);runtime.close();
    }

    @Test public void multiSelectionRemovesAllEntriesWithOneAtomicEditIntent() {
        Saved saved=new Saved();UiRuntime runtime=new UiRuntime(new QuestChapterManagementVisualDocument(detail(),
            TerminalActionPort.NONE,saved,TerminalWindowProfile.STANDARD,new Runnable(){public void run(){}}),
            new UiContext(HackerGreenTheme.create(),Locale.CHINA,1F,new FixedUiClock(0L,true)));
        runtime.setViewport(620,340);runtime.frame();click(runtime,"chapter-detail-select-mode");runtime.frame();
        UiRect canvas=find(runtime.getRoot(),"chapter-layout-canvas").getBounds();double scale=Math.min((canvas.getWidth()-8D)/160D,(canvas.getHeight()-8D)/152D);
        selectCanvas(runtime,canvas,scale,-20,12);runtime.frame();selectCanvas(runtime,canvas,scale,48,36);runtime.frame();
        click(runtime,"chapter-remove-selected");assertEquals(Arrays.asList("q1","q2"),saved.removedIds);runtime.close();
    }

    @Test public void multiAlignmentPanelStaysBoundedInAllSupportedViewports() {
        for(int[] size:new int[][]{{350,193},{427,240},{620,340}}){QuestChapterManagementVisualDocument document=
            new QuestChapterManagementVisualDocument(detail(),TerminalActionPort.NONE,QuestChapterManagementActionPort.NONE,
                TerminalWindowProfile.STANDARD,new Runnable(){public void run(){}});document.selectEntriesForPreview(Arrays.asList("q1","q2"));
            UiRuntime runtime=new UiRuntime(document,new UiContext(HackerGreenTheme.create(),Locale.CHINA,1F,new FixedUiClock(0L,true)));
            runtime.setViewport(size[0],size[1]);DrawList draw=runtime.frame();String snapshot=UiDebugSnapshot.capture(runtime.getRoot(),draw,size[0],size[1]);
            assertTrue(snapshot,snapshot.indexOf("OUT_OF_BOUNDS")<0);assertTrue(find(runtime.getRoot(),"chapter-align-center-x").getBounds().getHeight()>0);runtime.close();}
    }

    private static void selectCanvas(UiRuntime runtime,UiRect canvas,double scale,int logicalX,int logicalY){int x=canvas.getX()+4+(int)Math.round((logicalX+32)*scale),y=canvas.getY()+4+(int)Math.round((logicalY+24)*scale);assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN,x,y,0)));}

    private static void click(UiRuntime runtime, String key) { UiRect value = find(runtime.getRoot(), key).getBounds();
        assertTrue(runtime.dispatch(UiEvent.pointer(UiEvent.Type.POINTER_DOWN, value.getX()+Math.max(1,value.getWidth()/2), value.getY()+Math.max(1,value.getHeight()/2), 0))); }
    private static UiNode find(UiNode node,String key){if(node.getElement().getKey().getValue().equals(key))return node;for(UiNode child:node.getChildren()){UiNode found=findOrNull(child,key);if(found!=null)return found;}throw new AssertionError("missing "+key);}
    private static UiNode findOrNull(UiNode node,String key){if(node.getElement().getKey().getValue().equals(key))return node;for(UiNode child:node.getChildren()){UiNode found=findOrNull(child,key);if(found!=null)return found;}return null;}

    private static final class Saved implements QuestChapterManagementActionPort {
        private String id,hash,name,description,icon,background,visibility,movedQuest,placedQuest,resizedQuest,cloneChapter,movedChapter,beforeChapter,alignment;private int version,backgroundSize,movedX,movedY,resizedWidth,resizedHeight,cloneX,cloneY;private java.util.List<String> cloneSources,alignedIds,removedIds;
        public void back(){}public void filter(String value){}public void page(int value){}public void select(String id,int version){}
        public void closeDetail(){}public void create(String name){}public void publish(String id,int version,String hash){}
        public void retire(String id,int version){}public void place(String id,int version,String hash,String questId,int x,int y,int width,int height){placedQuest=questId;}
        public void move(String id,int version,String hash,String questId,int x,int y){movedQuest=questId;movedX=x;movedY=y;}public void resize(String id,int version,String hash,String questId,int width,int height){resizedQuest=questId;resizedWidth=width;resizedHeight=height;}
        public void remove(String id,int version,String hash,String questId){}
        public void removeEntries(String id,int version,String hash,java.util.List<String> questIds){removedIds=new java.util.ArrayList<String>(questIds);}
        public void cloneEntries(String id,int version,String hash,java.util.List<String> questIds,int anchorX,int anchorY){cloneChapter=id;cloneSources=new java.util.ArrayList<String>(questIds);cloneX=anchorX;cloneY=anchorY;}
        public void align(String id,int version,String hash,java.util.List<String> questIds,String alignment){this.alignment=alignment;this.alignedIds=new java.util.ArrayList<String>(questIds);}
        public void moveBefore(String chapterId,String beforeChapterId){movedChapter=chapterId;beforeChapter=beforeChapterId;}
        public void saveBasics(String id,int version,String hash,String name,String description,String icon,String background,int backgroundSize,String visibility){this.id=id;this.version=version;this.hash=hash;this.name=name;this.description=description;this.icon=icon;this.background=background;this.backgroundSize=backgroundSize;this.visibility=visibility;}
    }

    private static QuestChapterManagementVisualModel list() {
        QuestChapterManagementVisualModel.Chapter row = new QuestChapterManagementVisualModel.Chapter(
            "chapter-a", 1, "工业起步", "DRAFT", "hash", 2);
        return new QuestChapterManagementVisualModel(shell(), "", "DRAFT", "", 0, 20, 1,
            Collections.singletonList(row), null);
    }

    private static QuestChapterManagementVisualModel detail() {
        QuestChapterManagementVisualModel.Chapter row = new QuestChapterManagementVisualModel.Chapter(
            "chapter-a", 1, "工业起步", "DRAFT", "hash", 3);
        QuestChapterManagementVisualModel.Detail detail = new QuestChapterManagementVisualModel.Detail(row,
            "通过公共任务建立第一条生产链。", "minecraft:iron_ingot", "galaxy:textures/chapter.png", 256,
            "NORMAL", Arrays.asList(new QuestChapterManagementVisualModel.Entry("q1", "采集铁矿", -32, 0, 24, 24),
                new QuestChapterManagementVisualModel.Entry("q2", "冶炼铁锭", 32, 24, 32, 24),
                new QuestChapterManagementVisualModel.Entry("q3", "交付材料", 96, -24, 24, 32)),
            Arrays.asList(new QuestChapterManagementVisualModel.QuestCandidate("q4", "建设焦炉"),
                new QuestChapterManagementVisualModel.QuestCandidate("q5", "仓储网络"),
                new QuestChapterManagementVisualModel.QuestCandidate("q6", "公共电网"),
                new QuestChapterManagementVisualModel.QuestCandidate("q7", "火箭燃料"),
                new QuestChapterManagementVisualModel.QuestCandidate("q8", "太空阶段准备")));
        return new QuestChapterManagementVisualModel(shell(), "", "DRAFT", "", 0, 20, 1,
            Collections.singletonList(row), detail);
    }

    private static TerminalVisualModel shell() {
        return new TerminalVisualModel("银河终端", "career", Collections.singletonList(
            new TerminalVisualModel.NavItem("career", "职业", true, true)));
    }
}
