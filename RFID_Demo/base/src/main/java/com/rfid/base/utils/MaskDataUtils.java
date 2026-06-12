package com.rfid.base.utils;

import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.MatchData;
import com.ubx.usdk.bean.enums.InventorySceneMode;
import com.ubx.usdk.rfid.util.RfidErrorConstants;

import java.util.ArrayList;
import java.util.List;

public class MaskDataUtils {

    public static int addMaskDiffSceneMode(int membank,int startBits,int lenBits,String filterDataHex){
        if (RFIDSDKManager.getInstance().getRfidManager()!=null) {
            InventorySceneMode sceneMode = RFIDSDKManager.getInstance().getRfidManager().getInventorySceneMode();
            int ret = RFIDSDKManager.getInstance().getRfidManager().clearMask();
            if (sceneMode != InventorySceneMode.REPEAT_READ){
                ret = RFIDSDKManager.getInstance().getRfidManager().addMaskByBits(membank, startBits, lenBits, filterDataHex);
            }else {
                List<MatchData> matchDataList = new ArrayList<>();
                MatchData matchData = new MatchData();
                matchData.setMemBank(membank);
                matchData.setMaskStart(startBits);
                matchData.setMaskLen(lenBits);
                matchData.setMaskData(filterDataHex);
                matchDataList.add(matchData);
                ret = RFIDSDKManager.getInstance().getRfidManager().setInventoryMatchData(0, matchDataList);
            }
            return ret;
        }
        return RfidErrorConstants.SDK_UNINITIALIZED;
    }

    public static int setInventoryMaskDataFindTag(int membank,int startBits,int lenBits,String filterDataHex){
        if (RFIDSDKManager.getInstance().getRfidManager()!=null) {
                List<MatchData> matchDataList = new ArrayList<>();
                MatchData matchData = new MatchData();
                matchData.setMemBank(membank);
                matchData.setMaskStart(startBits);
                matchData.setMaskLen(lenBits);
                matchData.setMaskData(filterDataHex);
                matchDataList.add(matchData);
            return RFIDSDKManager.getInstance().getRfidManager().setInventoryMatchData(0, matchDataList);
        }
        return RfidErrorConstants.SDK_UNINITIALIZED;
    }

}
