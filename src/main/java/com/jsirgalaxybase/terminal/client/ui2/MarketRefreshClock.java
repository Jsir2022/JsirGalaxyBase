package com.jsirgalaxybase.terminal.client.ui2;

/** Five-second, single-flight market refresh clock with visible freshness state. */
final class MarketRefreshClock {
    private int age, pending; private boolean snapshot, timedOut;
    boolean tick(boolean eligible){if(snapshot)age++;if(!eligible){pending=0;return false;}if(pending>0){if(++pending>=240){pending=0;timedOut=true;}return false;}if(age>0&&age%100==0){pending=1;return true;}return false;}
    void received(){age=0;pending=0;snapshot=true;timedOut=false;}
    void requested(){pending=1;}
    String label(){if(!snapshot)return "行情等待中";if(pending>100)return "行情响应延迟";if(pending>0)return "行情刷新中";if(timedOut||age>=240)return "行情已过期 "+age/20+"s";return age<=20?"行情实时":"行情 "+age/20+"s 前";}
}
