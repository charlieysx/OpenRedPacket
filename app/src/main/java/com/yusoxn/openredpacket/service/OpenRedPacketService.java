package com.yusoxn.openredpacket.service;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 抢红包的辅助服务
 * <p>
 * Created by Yusxon on 17/2/5.
 */

public class OpenRedPacketService extends AccessibilityService {

    private static final String TAG = OpenRedPacketService.class.getSimpleName();

    private static final String RED_PACKET_MESSAGE = "[微信红包]";
    private static final String RED_PACKET_GET = "领取红包";
    private static final String RED_PACKET_WATCH = "查看红包";
    private static final String RED_PACKET_NONE = "手慢了，红包派完了";
    private static final String RED_PACKET = "微信红包";
    private static final String RED_PACKET_HAD_SAVE = "已存入零钱";

    /**
     * 微信相关界面类名
     * LAUNCHERUI：主界面
     * LUCKYMONEYRECEIVEUI：红包界面
     * LUCKYMONEYDETAILUI：红包详情界面
     */
    private static final String LAUNCHERUI = "com.tencent.mm.ui.LauncherUI";
    private static final String LUCKYMONEYRECEIVEUI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI";
    private static final String LUCKYMONEYDETAILUI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI";

    /**
     * 组件
     * BUTTON：按钮
     * LINEARLAYOUT：这里用来检测红包，防止文字干扰
     * TEXTVIEW
     * IMAGEVIEW
     * FRAMELAYOUT
     * LISTVIEW
     */
    private static final String BUTTON = "android.widget.Button";
    private static final String LINEARLAYOUT = "android.widget.LinearLayout";
    private static final String TEXTVIEW = "android.widget.TextView";
    private static final String IMAGEVIEW = "android.widget.ImageView";
    private static final String FRAMELAYOUT = "android.widget.FrameLayout";
    private static final String LISTVIEW = "android.widget.ListView";

    private Map<String, Boolean> hadOpen = new HashMap<>();

    /**
     * 标记是否是自动打开
     * 防止人为打开被自动关闭
     */
    private boolean autoOpenLuckyMoneyReceiveUI = false;
    private boolean autoOpenLuckyMoneyDetailUI = false;

    private SharedPreferences redPacketPre;

    private String lastKey = "";

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        redPacketPre = getSharedPreferences("REDPACKET", MODE_PRIVATE);
        //系统会在成功连接上你的服务的时候调用这个方法
        Toast.makeText(this, "服务开启", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //通过这个函数可以接收系统发送来的AccessibilityEvent，接收来的AccessibilityEvent是经过过滤的，过滤是在配置工作时设置的
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                notificationStateChanged(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                windowContentChanged(event);
                break;
        }
    }

    @Override
    public void onInterrupt() {
        //这个在系统想要中断AccessibilityService返给的响应时会调用。在整个生命周期里会被调用多次
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //在系统将要关闭这个AccessibilityService会被调用。在这个方法中进行一些释放资源的工作
        return super.onUnbind(intent);
    }

    /**
     * 通知栏状态改变
     *
     * @param event
     */
    private void notificationStateChanged(AccessibilityEvent event) {
        List<CharSequence> texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
                //检测该条消息是否是红包信息
                String message = text.toString();
                if (message.contains(RED_PACKET_MESSAGE)) {
                    Parcelable parcelable = event.getParcelableData();
                    if (parcelable != null && parcelable instanceof Notification) {
                        Notification notification = (Notification) parcelable;
                        try {
                            notification.contentIntent.send();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 窗口内容改变
     *
     * @param event
     */
    private void windowContentChanged(AccessibilityEvent event) {
        AccessibilityNodeInfo node = event.getSource();
        if (node != null) {
            System.out.println(node.getClassName() + "--" + node.getText());
            String className = event.getClassName().toString();
            if (className.equals(LUCKYMONEYRECEIVEUI)) {
                openRedPacket(node);
            } else if (className.equals(LUCKYMONEYDETAILUI)) {
                getMoney(node);
                if (autoOpenLuckyMoneyDetailUI) {
                    close();
                }
            } else {
                //检查聊天列表
                checkChatList(node);
                //检查聊天界面
                checkChatUI(node);
            }
        }
    }

    /**
     * 关闭页面
     */
    private void close() {
        performGlobalAction(GLOBAL_ACTION_BACK);
        autoOpenLuckyMoneyReceiveUI = false;
        autoOpenLuckyMoneyDetailUI = false;
    }

    /**
     * 根据文字查找节点
     *
     * @param nodeInfo
     * @param text
     * @return
     */
    private List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        if (text != null) {
            List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByText(text);
            result.addAll(nodes);
        }

        return result;
    }

    /**
     * 检查聊天列表
     *
     * @param node
     */
    private void checkChatList(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> nodes = findAccessibilityNodeInfosByText(node, RED_PACKET_MESSAGE);
        if (!nodes.isEmpty()) {
            AccessibilityNodeInfo info = nodes.get(0);
            info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    /**
     * 检查聊天界面
     *
     * @param node
     */
    private void checkChatUI(AccessibilityNodeInfo node) {
        boolean is = false;
        while (node != null) {
            if (node.getClassName().equals(LISTVIEW) || node.getClassName().equals(FRAMELAYOUT)) {
                is = true;
                break;
            }
            node = node.getParent();
        }
        if (!is) {
            return;
        }
        List<AccessibilityNodeInfo> nodes = findAccessibilityNodeInfosByText(node, RED_PACKET);
        int position = nodes.size() - 1;
        AccessibilityNodeInfo info;
        while (position >= 0) {
            info = nodes.get(position--);
            if (info != null && info.getParent() != null) {
                if (info.getParent().getClassName().equals(LINEARLAYOUT)) {
                    String key = getKey(info);
                    if(redPacketPre == null) {
                        redPacketPre = getSharedPreferences("REDPACKET", MODE_PRIVATE);
                    }
                    if (redPacketPre.getBoolean("ISFIRST", true)) {
                        if (hadOpen.containsKey(key) && hadOpen.get(key)) {
                            System.out.println("领取过该红包，不再领取--" + key);
                        } else {
                            hadOpen.put(key, true);
                            autoOpenLuckyMoneyReceiveUI = true;
                            autoOpenLuckyMoneyDetailUI = true;
                            System.out.println("领取--" + key);
                            lastKey = key;
                            info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                    } else if (!lastKey.equals(key)) {
                        hadOpen.put(key, true);
                        autoOpenLuckyMoneyReceiveUI = true;
                        autoOpenLuckyMoneyDetailUI = true;
                        System.out.println("领取--" + key);
                        lastKey = key;
                        info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    } else {
                        break;
                    }
                } else {
                    System.out.println("文字干扰");
                }
            }
        }
    }

    /**
     * 打开红包
     *
     * @param node
     */
    private void openRedPacket(AccessibilityNodeInfo node) {
        int childPosition = 0;
        while (childPosition < node.getChildCount()) {
            AccessibilityNodeInfo info = node.getChild(childPosition++);
            if (info != null) {
                if (info.getClassName().equals(BUTTON)) {
                    autoOpenLuckyMoneyDetailUI = true;
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return;
                } else if (info.getClassName().equals(TEXTVIEW)) {
                    String text = info.getText().toString();
                    if (text.equals(RED_PACKET_NONE)) {
                        //匹配"手慢了"，说明红包被领了，关掉该界面
                        if (autoOpenLuckyMoneyReceiveUI) {
                            close();
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取得到的钱数
     *
     * @param node
     */
    private void getMoney(AccessibilityNodeInfo node) {
        if (node != null) {
            List<AccessibilityNodeInfo> nodes = findAccessibilityNodeInfosByText(node, RED_PACKET_HAD_SAVE);
            if (nodes != null && nodes.size() > 0) {
                AccessibilityNodeInfo info = nodes.get(0).getParent();
                if (info != null && info.getChildCount() > 3) {
                    String text = "( ◕‿- )   打开红包领取了" + info.getChild(2).getText() + "元";
                    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 获取该红包的key，避免重复打开
     *
     * @param node
     * @return
     */
    private String getKey(AccessibilityNodeInfo node) {
        String result = "-";
        AccessibilityNodeInfo redPackNode = node.getParent();
        if (redPackNode != null) {
            for (int i = 0; i < redPackNode.getChildCount(); i++) {
                AccessibilityNodeInfo thisNode = redPackNode.getChild(i);
                if (TEXTVIEW.equals(thisNode.getClassName())) {
                    result += thisNode.getText() + "-";
                    break;
                }
            }
            AccessibilityNodeInfo userNode = redPackNode.getParent();
            if (userNode != null) {
                for (int i = 0; i < userNode.getChildCount(); i++) {
                    AccessibilityNodeInfo thisNode = userNode.getChild(i);
                    if (IMAGEVIEW.equals(thisNode.getClassName())) {
                        result = "-" + thisNode.getContentDescription() + result;
                    }
                    if (TEXTVIEW.equals(thisNode.getClassName())) {
                        result += thisNode.getText() + "-";
                    }
                }
            }
        }

        return result;
    }
}
