# EN

# 2025-12-30  （URFIDLibrary-v2.5.1230.aar）  

### 1. setInventorySceneMode - New Inventory Scene Modes
Added two new reading modes:
•	Cycle Count Mode: Deduplicated inventory counting mode
•	Max Range Mode: Maximum read distance mode


### 2. Data Callback Parameter Type Change
The onInventoryTag callback method signature has been updated:
```java 
Previous:
onInventoryTag(String EPC, String Data, String rssi);
Updated:
onInventoryTag(String EPC, String Data, int rssi);
```
Note: The rssi parameter type changed from String to int.


### 3. RfidProfile Enumeration - Miller Configuration Return Type Change
The Miller configuration getter in RfidProfile enumeration now returns int type instead of String.


### 4. New Method: getSupportProfileList()
Added a new method to retrieve the list of supported Profile combinations:
getSupportProfileList()


### 5. New Method: getSupportFrequencyBandList()
Added a new method to retrieve the list of supported Frequency Region combinations:
getSupportFrequencyBandList()


### 6. New Method: getSupportWorkRegionList()
Added a new method to retrieve the list of supported Country Code combinations:
getSupportWorkRegionList()


### 7. Enhanced Module Type Identification: getModuleType()
Enhanced the method for retrieving the current module type. This method can be used to differentiate supported Profiles:
```java 
int getModuleType()
Supported Module Types:
•	MODULE_U1
•	MODULE_U2
•	MODULE_U3
•	MODULE_U4
•	MODULE_U5
•	MODULE_OTHER
```

### 8. New Method: getReaderDeviceType()
Added a new method to retrieve the reader device type:
getReaderDeviceType()
ReaderDeviceType Enumeration Values:
```java 
	Value	Constant	                Description
	-1	ReaderDeviceType.UNKNOWN	Unknown device type
	0	ReaderDeviceType.INTEGRATED	Integrated device
	1	ReaderDeviceType.PERIPHERAL_UART	Serial port device
	2	ReaderDeviceType.BLE_DEVICE	Bluetooth device
```

### 9. New Method: setInventoryPhaseFlag(boolean flag)
Added a new method to enable or disable the return of phase and frequency information:
setInventoryPhaseFlag(boolean flag)


### 10. Callback Registration Method Update
Recommended: Use addDataCallback() instead of the deprecated registration method.

```java 	
	 Previous:
		registerCallback(IRfidCallback cb) 
	 Updated:
		addDataCallback(DataCallback cb)
```

Callback Method Signature Change:
```java 	
	Previous: onInventoryTag(String EPC, String Data, int rssi);
	Updated: onInventoryTag(ReadTag readTag);
```

ReadTag Class Properties:
```java 
Property	Type	Description
epcId	    String	EPC value
memId	    String	TID or USER data (In QueryMemBank EPC+TID or EPC+USER mode, this parameter contains TID or USER data respectively)
rssi	    int	    Signal strength value (0-100)
phase	    int	    Phase value
FreqKhz	    int	    Frequency point (in KHz)
```

### 11. Release Method Deprecation
The rfidManager.release() method has been removed.
Use the following method instead:
RFIDSDKManager.getInstance().release();


### 12.Code optimization
The core functional modules have been optimized to enhance code maintainability and execution efficiency.






# 2026-04-08  （URFIDLibrary-v2.6.0408.aar）   

### 1. The SO library is 16KB aligned. 

### 2. The method setRssiInDbm has been removed.
 Now, the dBm value is automatically returned in the readTag.rssidBm field of the onInventoryTag(ReadTag readTag) callback, which represents the dBm signal value. 

### 3. The method for switching the signal value return range has been modified:
```java 
"setRssiConvertEnabled" has been replaced with: 2.5.15 "setRssiUnitType"
"getRssiConvertEnabled" has been replaced with:        "getRssiUnitType"
```


# 2026-06-09  （URFIDLibrary-v2.6.0609.aar）

### 1. Added RFID firmware update interface. 


### 2. Optimized and fixed the issue of inconsistent 16KB alignment display results on different Android Studio versions.














# CN 



# 2025-12-30  （URFIDLibrary-v2.5.1230.aar）  

### 1. setInventorySceneMode - 新增盘点场景模式
新增两种读取模式：
•	Cycle Count（去重盘点模式）：自动去重的盘点计数模式
•	Max Range（最远读距模式）：最大读取距离模式

### 2. 数据回调参数类型变更
onInventoryTag 回调方法签名已更新：
变更前：
onInventoryTag(String EPC, String Data, String rssi);
变更后：
onInventoryTag(String EPC, String Data, int rssi);
说明：rssi 参数类型从 String 变更为 int。

### 3. RfidProfile 枚举 - Miller 配置返回类型变更
RfidProfile 枚举中获取 Miller 配置的方法，返回类型从 String 变更为 int。

### 4. 新增方法：getSupportProfileList()
新增获取支持的 Profile 组合列表的方法：
getSupportProfileList()

### 5. 新增方法：getSupportFrequencyBandList()
新增获取支持的频率区域（FrequencyRegion）组合列表的方法：
getSupportFrequencyBandList()

### 6. 新增方法：getSupportWorkRegionList()
新增获取支持的国家代码（CountryCode）组合列表的方法：
getSupportWorkRegionList()

### 7. 完善模块类型获取方法：getModuleType()
完善获取当前模块类型的方法，可用于区分支持的 Profile：
```java 
int getModuleType()
支持的模块类型：
•	MODULE_U1
•	MODULE_U2
•	MODULE_U3
•	MODULE_U4
•	MODULE_U5
•	MODULE_OTHER
```

### 8. 新增方法：getReaderDeviceType()
新增获取设备类型的方法：
```java 
getReaderDeviceType() ReaderDeviceType 枚举值：
值	常量	说明
-1	ReaderDeviceType.UNKNOWN	未知设备类型
0	ReaderDeviceType.INTEGRATED	一体机设备
1	ReaderDeviceType.PERIPHERAL_UART	串口设备
2	ReaderDeviceType.BLE_DEVICE	蓝牙设备
```

### 9. 新增方法：setInventoryPhaseFlag(boolean flag)
新增设置是否返回相位、频点信息的方法：
```java 
setInventoryPhaseFlag(boolean flag)
参数说明：
•	flag = true：返回相位和频点信息
•	flag = false：不返回相位和频点信息
```

### 10. 回调注册方法变更
建议使用 addDataCallback() 替代原有的回调注册方法。
```java 
变更前：
registerCallback(IRfidCallback cb)
变更后：
addDataCallback(DataCallback cb)
```

```java 
回调方法签名变更：
变更前：
onInventoryTag(String EPC, String Data, int rssi);
变更后：
onInventoryTag(ReadTag readTag);
```

ReadTag 类属性说明：
```java 
	属性	类型	说明
	epcId	String	EPC 值
	memId	String	TID 或 USER 数据（在 QueryMemBank 的 EPC+TID 或 EPC+USER 模式下，该参数分别为 TID 和 USER 数据）
	rssi	int	   信号强度值（范围：0-100）
	phase	int	   相位值
	FreqKhz	int	   频点（单位：KHz）
```

### 11. release 方法变更
移除了 rfidManager.release() 方法。
请使用以下方法替代：
RFIDSDKManager.getInstance().release();

### 12. 代码优化
针对核心功能模块进行了优化，提升代码可维护性与执行效率。



# 2026-04-08  （URFIDLibrary-v2.6.0408.aar）   

### 1.SO库做了16KB对齐；

### 2.删除了 setRssiInDbm 方法
dBm值现在在盘点回调 onInventoryTag(ReadTag readTag) 中的 readTag.rssidBm 字段默认返回 dBm 信号值。

### 3.用于切换信号值返回范围的方法，做了变更：
```java 
setRssiConvertEnabled 替换成：2.5.15 setRssiUnitType  
getRssiConvertEnabled 替换成：getRssiUnitType
```


# 2026-06-09  （URFIDLibrary-v2.6.0609.aar）  
 
### 1.新增RFID固件更新接口；

### 2- 优化并修复了不同 Android Studio 版本之间显示的 16KB 对齐结果不一致的问题。