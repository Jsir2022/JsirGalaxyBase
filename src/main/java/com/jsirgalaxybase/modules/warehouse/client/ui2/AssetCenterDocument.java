package com.jsirgalaxybase.modules.warehouse.client.ui2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityRow;
import com.jsirgalaxybase.modules.warehouse.domain.AssetActivityType;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentAction;
import com.jsirgalaxybase.modules.warehouse.domain.TerminalCellContentEntry;
import com.jsirgalaxybase.modules.warehouse.infrastructure.minecraft.TerminalAssetCenterTab;
import com.jsirgalaxybase.terminal.client.ui2.TerminalAppShell;
import com.jsirgalaxybase.terminal.client.ui2.TerminalVisualModelAdapter;
import com.jsirgalaxybase.terminal.client.viewmodel.TerminalHomeScreenModel;
import com.jsirgalaxybase.ui2.core.UiContext;
import com.jsirgalaxybase.ui2.core.UiElement;
import com.jsirgalaxybase.ui2.geometry.UiSize;
import com.jsirgalaxybase.ui2.layout.LayoutSpec;
import com.jsirgalaxybase.ui2.render.DrawCommand;
import com.jsirgalaxybase.ui2.render.DrawList;
import com.jsirgalaxybase.ui2.state.UiStore;
import com.jsirgalaxybase.ui2.terminal.AssetCenterActionPort;
import com.jsirgalaxybase.ui2.terminal.AssetCenterVisualDocument;
import com.jsirgalaxybase.ui2.terminal.AssetCenterVisualModel;
import com.jsirgalaxybase.ui2.terminal.TerminalActionPort;
import com.jsirgalaxybase.ui2.terminal.VisualItem;

/** Live Minecraft adapter around the shared platform-neutral asset-center document. */
public final class AssetCenterDocument extends AssetCenterVisualDocument {
    public interface CursorAccess { ItemStack current(); }
    private static final int PAGE_SIZE = 20;
    private final UiStore<AssetCenterUiState, AssetCenterAction> store;
    private final CursorAccess cursor;
    private final TerminalHomeScreenModel shellModel;

    public AssetCenterDocument(final UiStore<AssetCenterUiState, AssetCenterAction> store,
        TerminalHomeScreenModel shellModel, final TerminalAppShell.Actions shellActions,
        final CursorAccess cursor) {
        super(model(store.getState(), shellModel, cursor), shell(shellActions),
            actions(store, cursor), TerminalVisualModelAdapter.windowProfile());
        this.store = store;
        this.cursor = cursor;
        this.shellModel = shellModel;
    }

    @Override public UiElement build(UiContext context) {
        updateVisualModel(model(store.getState(), shellModel, cursor));
        updateWindowProfile(TerminalVisualModelAdapter.windowProfile());
        return super.build(context);
    }

    @Override public LayoutSpec layout(UiElement root, UiSize viewport, UiContext context) {
        updateVisualModel(model(store.getState(), shellModel, cursor));
        return super.layout(root, viewport, context);
    }

    public Map<String, ItemStack> externalItems() {
        Map<String, ItemStack> result = new LinkedHashMap<String, ItemStack>();
        List<TerminalCellContentEntry> entries = store.getState().getCell().getEntries();
        for (int i = 0; i < entries.size(); i++) result.put("asset:cell:" + i, entries.get(i).getStack());
        List<AssetActivityRow> rows = store.getState().getActivity().getRows();
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).getItemStack() != null)
            result.put("asset:activity:" + i, rows.get(i).getItemStack());
        return Collections.unmodifiableMap(result);
    }

    public String tooltipFor(DrawList list, int x, int y) {
        if (list == null) return "";
        Map<String, com.jsirgalaxybase.ui2.geometry.UiRect> regions =
            new LinkedHashMap<String, com.jsirgalaxybase.ui2.geometry.UiRect>();
        for (DrawCommand command : list.ordered()) if (command.getKind() == DrawCommand.Kind.EXTERNAL_REGION)
            regions.put(command.getExternalId(), command.getBounds());
        List<TerminalCellContentEntry> entries = store.getState().getCell().getEntries();
        for (int i = 0; i < entries.size(); i++) {
            com.jsirgalaxybase.ui2.geometry.UiRect bounds = regions.get("asset:cell:" + i);
            if (bounds != null && bounds.contains(x, y)) return entries.get(i).getStack().getDisplayName()
                + "|Cell 数量：" + compact(entries.get(i).getQuantity()) + "|左键取一组，右键取一个";
        }
        return "";
    }

    private static AssetCenterVisualModel model(AssetCenterUiState state,
        TerminalHomeScreenModel shellModel, CursorAccess cursor) {
        TerminalHomeScreenModel shell = shellModel == null ? TerminalHomeScreenModel.placeholder() : shellModel;
        List<VisualItem> items = new ArrayList<VisualItem>();
        List<TerminalCellContentEntry> entries = state.getCell().getEntries();
        for (int i = 0; i < entries.size(); i++) items.add(visual("cell-" + i, entries.get(i).getStack(), entries.get(i).getQuantity()));
        List<AssetCenterVisualModel.Activity> activities = new ArrayList<AssetCenterVisualModel.Activity>();
        for (AssetActivityRow row : state.getActivity().getRows()) {
            ItemStack stack = row.getItemStack();
            String detail = stack == null ? row.getReference() : stack.getDisplayName();
            if (row.getQuantity() > 0) detail += " ×" + compact(row.getQuantity());
            if (row.getAmount() > 0) detail += " · " + compact(row.getAmount()) + " GT";
            activities.add(new AssetCenterVisualModel.Activity(row.getStableKey(), activityLabel(row), detail,
                new SimpleDateFormat("MM-dd HH:mm", Locale.ROOT).format(new Date(row.getCreatedAt().toEpochMilli())),
                stack == null ? null : visual(row.getStableKey(), stack, row.getQuantity())));
        }
        return new AssetCenterVisualModel(TerminalVisualModelAdapter.shell(shell),
            state.getTab() == TerminalAssetCenterTab.ACTIVITY ? AssetCenterVisualModel.Tab.ACTIVITY : AssetCenterVisualModel.Tab.STORAGE,
            state.getCell().isCellPresent(), state.getCell().getUsedBytes(), state.getCell().getTotalBytes(),
            state.getCell().getStoredTypes(), state.getCell().getTotalTypes(), state.getCell().getPageIndex(),
            state.getCell().getTotalPages(), cursor != null && cursor.current() != null, state.getCell().getFeedback(),
            items, activities, state.isHelpOpen());
    }

    private static VisualItem visual(String id, ItemStack stack, long quantity) {
        Object key = Item.itemRegistry.getNameForObject(stack.getItem());
        return new VisualItem(id, key == null ? "missing:unknown" : String.valueOf(key),
            stack.getItemDamage(), stack.getDisplayName(), quantity);
    }

    private static TerminalActionPort shell(final TerminalAppShell.Actions actions) {
        return new TerminalActionPort(){public void navigate(String id){actions.navigate(id);}public void refresh(){actions.refresh();}public void help(){actions.help();}public void back(){actions.back();}public void close(){actions.close();}};
    }

    private static AssetCenterActionPort actions(final UiStore<AssetCenterUiState,AssetCenterAction> store,
        final CursorAccess cursor) {
        return new AssetCenterActionPort(){
            public void selectTab(AssetCenterVisualModel.Tab tab){store.dispatch(AssetCenterAction.tab(tab==AssetCenterVisualModel.Tab.ACTIVITY?TerminalAssetCenterTab.ACTIVITY:TerminalAssetCenterTab.STORAGE,PAGE_SIZE));}
            public void sortVault(){store.dispatch(AssetCenterAction.simple(AssetCenterAction.Type.SORT_VAULT));}
            public void depositCursor(){store.dispatch(AssetCenterAction.cell(TerminalCellContentAction.INJECT_CURSOR,cursor.current()));}
            public void extract(String id,boolean single){int index=parseIndex(id);List<TerminalCellContentEntry> entries=store.getState().getCell().getEntries();if(index>=0&&index<entries.size())store.dispatch(AssetCenterAction.cell(single?TerminalCellContentAction.EXTRACT_ONE:TerminalCellContentAction.EXTRACT_STACK,entries.get(index).getStack()));}
            public void page(int page){store.dispatch(AssetCenterAction.page(page,PAGE_SIZE));}
            public void closeHelp(){store.dispatch(AssetCenterAction.simple(AssetCenterAction.Type.CLOSE_HELP));}
        };
    }
    private static int parseIndex(String id){try{return Integer.parseInt(id.substring(id.lastIndexOf('-')+1));}catch(RuntimeException ignored){return -1;}}
    private static String activityLabel(AssetActivityRow row){AssetActivityType type=row.getType();if(type==AssetActivityType.STANDARD_BUY_DEPOSITED)return "标准市场买入入库";if(type==AssetActivityType.STANDARD_SELL_SETTLED)return "标准市场卖出结算";if(type==AssetActivityType.CUSTOM_BUY_DELIVERED)return "定制市场买入交付";if(type==AssetActivityType.CUSTOM_SELL_SETTLED)return "定制市场卖出结算";if(type==AssetActivityType.ASSET_RETURNED)return "撤单资产返还";if(type==AssetActivityType.DELIVERY_FAILED)return "交付失败";if(type==AssetActivityType.RECOVERY_COMPLETED)return "恢复完成";return "需要恢复";}
}
