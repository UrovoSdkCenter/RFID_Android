
##   2026-04-08
1. Added 16‑KB page alignment required by Google for the SO library.

2. New interface: Set whether the returned RSII range from the inventory check is 0-100
// true: Displays the signal value ranging from 0 to 100; false: Displays the default value (not within the range of 0 to 100)
RFIDSDKManager.getInstance().getRfidManager().setRssiConvertEnabled(true);

3. The method setRssiInDbm has been removed. Now, the dBm value is automatically returned in the readTag.rssidBm field of the onInventoryTag(ReadTag readTag) callback, which represents the dBm signal value. 

4. The method for switching the signal value return range has been modified:
"setRssiConvertEnabled" has been replaced with: 2.5.15 "setRssiUnitType"
"getRssiConvertEnabled" has been replaced with:        "getRssiUnitType"