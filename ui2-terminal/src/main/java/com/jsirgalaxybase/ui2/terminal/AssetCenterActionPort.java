package com.jsirgalaxybase.ui2.terminal;

/** Asset operations remain outside the visual module and are implemented by the live adapter. */
public interface AssetCenterActionPort {
    void selectTab(AssetCenterVisualModel.Tab tab); void sortVault(); void depositCursor();
    void extract(String visualItemId, boolean single); void page(int page); void closeHelp();
    AssetCenterActionPort NONE = new AssetCenterActionPort(){public void selectTab(AssetCenterVisualModel.Tab tab){}public void sortVault(){}public void depositCursor(){}public void extract(String id,boolean single){}public void page(int page){}public void closeHelp(){}};
}
