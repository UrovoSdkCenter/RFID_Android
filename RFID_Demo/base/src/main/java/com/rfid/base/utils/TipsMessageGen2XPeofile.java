package com.rfid.base.utils;

public class TipsMessageGen2XPeofile {

    public static String getZHDes(){
        String content = "启用 Gen2X 功能操作指南："+"\n" +
                "    Gen2X 是 Impinj 的高性能读取模式，针对 M700、M800 系列标签进行了优化。根据您的需求，可选择两种读取策略："+"\n" +
                "1> 专用模式:4123、4124、4141、4146、4148、4185  仅读取 M700、M800 系列标签；"+"\n" +
                "2> 兼容模式:5123、5124、5141、5146、5148、5185  读取 M700、M800 系列标签 + 普通标签；"+"\n" +
                "\n" +
                "启用步骤:\n" +
                "1> 点击Tab页的【Config】选项卡；\n" +
                "2.1> 在参数列表中找到并点击 【Other Parameters】；\n" +
                "2.2> 找到 【Inventory Mode】 参数；\n" +
                "2.3> 从下拉菜单中选择 【Custom Mode】；\n" +
                "2.4> 选择后，点击右侧的 【Set】  按钮，看到“设置成功”提示后，配置即生效；\n" +
                "3.1> 然后找到 【Inventory Parameters】；\n" +
                "3.2> 点击 【Reader Mode”  下拉选择框；\n" +
                "3.3> 从列表中选择上述 12 个 Gen2X Profile 中的一个；\n" +
                "3.4> 选择后，点击右侧的 【Set】  按钮，看到“设置成功”提示后，配置即生效。\n" +
                "\n"+
                "注意事项:\n" +
                "1> 启用 Gen2X 后，如需要切换回普通模式，只需在【Reader Mode】中选择非 Gen2X 的 Profile；\n" +
                "2> 不同 Profile 在特定环境下的性能可能有差异，建议根据实际测试选择最优配置。";

        return content;
    }
    public static String getENDes(){
        String content = "Operation Guide for Enabling Gen2X Functionality:"+"\n" +
                "    Gen2X is Impinj's high-performance reading mode optimized for M700 and M800 series tags. Depending on your needs, you can choose from two reading strategies:"+"\n" +
                "1> Dedicated Mode:4123, 4124, 4141, 4146, 4148, 4185  Read only M700 and M800 series tags;"+"\n" +
                "2> Compatible Mode:5123, 5124, 5141, 5146, 5148, 5185  Read M700, M800 series tags plus ordinary tags;"+"\n" +
                "\n" +
                "Enabling Steps:\n" +
                "1> Click the 【Config】 tab on the Tab page;\n" +
                "2.1> In the parameter list, find and click 【Other Parameters】;\n" +
                "2.2> Find the 【Inventory Mode】 parameter;\n" +
                "2.3> Select 【Custom Mode】 from the dropdown menu;\n" +
                "2.4> After selection, click the 【Set】 button on the right. Once you see the “Set successful” prompt, the configuration takes effect;\n" +
                "3.1> Then find 【Inventory Parameters】;\n" +
                "3.2> Click the 【Reader Mode】 dropdown selection box;\n" +
                "3.3> Select one of the above 12 Gen2X Profiles from the list;\n" +
                "3.4> After selection, click the 【Set】 button on the right. Once you see the “Set successful” prompt, the configuration takes effect.\n" +
                "\n"+
                "Notes:\n" +
                "1> After enabling Gen2X, if you need to switch back to normal mode, simply select a non-Gen2X Profile in 【Reader Mode】;\n" +
                "2> Different Profiles may have varying performance in specific environments. It is recommended to choose the optimal configuration based on actual testing.";

        return content;
    }
}
