package com.rfid.base;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;

import com.rfid.base.databinding.ActivityFixedBinding;
import com.ubx.usdk.RFIDSDKManager;
import com.ubx.usdk.bean.AntennaSet;
import com.ubx.usdk.bean.AntennaStatus;
import com.ubx.usdk.bean.ResultAntenna;
import com.ubx.usdk.rfid.GpioController;
import com.ubx.usdk.rfid.RfidManager;

/**
 * 固定式：天线多路复用、GPIO 等读写器能力演示。
 */
public class FixedActivity extends BaseActivity implements TabLifecycleListener {

    private ActivityFixedBinding binding;

    private static final int[] ANTENNA_LABEL_RES = {
            R.string.fixed_antenna_1, R.string.fixed_antenna_2, R.string.fixed_antenna_3, R.string.fixed_antenna_4,
            R.string.fixed_antenna_5, R.string.fixed_antenna_6, R.string.fixed_antenna_7, R.string.fixed_antenna_8,
    };

    private final int[] outLevels = new int[4];
    private boolean beepOn;
    private boolean blueLedOn;
    private boolean orangeLedOn;

    private final GpioController gpio = GpioController.getInstance();
    private final GpioController.GpioInputListener gpioInputListener = new GpioController.GpioInputListener() {
        @Override
        public void onInput1Changed(int state) {
            postInputLabel(0, state);
        }

        @Override
        public void onInput2Changed(int state) {
            postInputLabel(1, state);
        }

        @Override
        public void onInput3Changed(int state) {
            postInputLabel(2, state);
        }

        @Override
        public void onInput4Changed(int state) {
            postInputLabel(3, state);
        }
    };

    private void postInputLabel(int index, int state) {
        if (binding == null) {
            return;
        }
        TextView tv = inputViewForIndex(index);
        if (tv == null) {
            return;
        }
        tv.post(() -> tv.setText(formatInputLine(index + 1, state)));
    }

    private TextView inputViewForIndex(int index) {
        switch (index) {
            case 0:
                return binding.tvFixedGpioIn1;
            case 1:
                return binding.tvFixedGpioIn2;
            case 2:
                return binding.tvFixedGpioIn3;
            case 3:
                return binding.tvFixedGpioIn4;
            default:
                return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFixedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initGpioUi();
        wireAntennaActions();
    }

    private void wireAntennaActions() {
        binding.btnFixedAntennaDetect.setOnClickListener(v -> onAntennaDetect());
        binding.btnFixedSetInventory.setOnClickListener(v -> onSetInventoryAntennas());
        binding.btnFixedGetInventory.setOnClickListener(v -> onGetInventoryAntennas());
        binding.btnFixedSetAccess.setOnClickListener(v -> onSetAccessAntennas());
        binding.btnFixedGetAccess.setOnClickListener(v -> onGetAccessAntennas());
    }

    private RfidManager rfidOrNull() {
        try {
            return RFIDSDKManager.getInstance().getRfidManager();
        } catch (Throwable t) {
            return null;
        }
    }

    private void onAntennaDetect() {
        RfidManager rf = rfidOrNull();
        if (rf == null) {
            toast(R.string.fixed_tf_rfid_unavailable);
            return;
        }
        AntennaStatus status = rf.checkAntennaStatus();
        if (status == null) {
            toast(R.string.get_failed);
            return;
        }
        applyAntennaStatus(status);
    }

    private CheckBox[] antennaCheckBoxes() {
        return new CheckBox[]{
                binding.cbFixedAnt1, binding.cbFixedAnt2, binding.cbFixedAnt3, binding.cbFixedAnt4,
                binding.cbFixedAnt5, binding.cbFixedAnt6, binding.cbFixedAnt7, binding.cbFixedAnt8,
        };
    }

    private void applyAntennaStatus(AntennaStatus status) {
        boolean[] present = {
                status.ant1, status.ant2, status.ant3, status.ant4,
                status.ant5, status.ant6, status.ant7, status.ant8,
        };
        CheckBox[] boxes = antennaCheckBoxes();
        for (int i = 0; i < 8; i++) {
            CheckBox cb = boxes[i];
            if (present[i]) {
                cb.setEnabled(true);
                cb.setChecked(true);
            } else {
                cb.setChecked(false);
                cb.setEnabled(false);
            }
        }
    }

    private AntennaSet buildAntennaSetFromSelection() {
        AntennaSet set = new AntennaSet();
        CheckBox[] boxes = antennaCheckBoxes();
        boolean[] v = new boolean[8];
        for (int i = 0; i < 8; i++) {
            v[i] = boxes[i].isEnabled() && boxes[i].isChecked();
        }
        set.ant1 = v[0];
        set.ant2 = v[1];
        set.ant3 = v[2];
        set.ant4 = v[3];
        set.ant5 = v[4];
        set.ant6 = v[5];
        set.ant7 = v[6];
        set.ant8 = v[7];
        return set;
    }

    private boolean hasAtLeastOneSelectedAntenna() {
        CheckBox[] boxes = antennaCheckBoxes();
        for (CheckBox cb : boxes) {
            if (cb.isEnabled() && cb.isChecked()) {
                return true;
            }
        }
        return false;
    }

    private void onSetInventoryAntennas() {
        if (!hasAtLeastOneSelectedAntenna()) {
            toast(R.string.fixed_toast_select_at_least_one_antenna);
            return;
        }
        RfidManager rf = rfidOrNull();
        if (rf == null) {
            toast(R.string.fixed_tf_rfid_unavailable);
            return;
        }
        int ret = rf.setInventoryAntennaMultiplexing(buildAntennaSetFromSelection());
        if (ret == 0) {
            toast(R.string.set_success);
        } else {
            toast(R.string.set_failed);
        }
    }

    private void onGetInventoryAntennas() {
        RfidManager rf = rfidOrNull();
        if (rf == null) {
            toast(R.string.fixed_tf_rfid_unavailable);
            return;
        }
        ResultAntenna result = rf.getInventoryAntennaMultiplexing();
        binding.tvFixedInventoryResult.setText(formatResultAntennaText(result));
    }

    private void onSetAccessAntennas() {
        if (!hasAtLeastOneSelectedAntenna()) {
            toast(R.string.fixed_toast_select_at_least_one_antenna);
            return;
        }
        RfidManager rf = rfidOrNull();
        if (rf == null) {
            toast(R.string.fixed_tf_rfid_unavailable);
            return;
        }
        int ret = rf.setAccessAntennaMultiplexing(buildAntennaSetFromSelection());
        if (ret == 0) {
            toast(R.string.set_success);
        } else {
            toast(R.string.set_failed);
        }
    }

    private void onGetAccessAntennas() {
        RfidManager rf = rfidOrNull();
        if (rf == null) {
            toast(R.string.fixed_tf_rfid_unavailable);
            return;
        }
        ResultAntenna result = rf.getAccessAntennaMultiplexing();
        binding.tvFixedAccessResult.setText(formatResultAntennaText(result));
    }

    private String formatResultAntennaText(ResultAntenna result) {
        if (result == null) {
            return getString(R.string.get_failed);
        }
        StringBuilder sb = new StringBuilder();
        if (result.code != 0) {
            sb.append(getString(R.string.fixed_result_code_fmt, result.code)).append('\n');
        }
        if (result.antennaSet == null) {
            sb.append(getString(R.string.get_failed));
            return sb.toString();
        }
        sb.append(formatAntennaSetLine(result.antennaSet));
        return sb.toString();
    }

    private String formatAntennaSetLine(AntennaSet set) {
        String on = getString(R.string.fixed_antenna_label_on);
        String off = getString(R.string.fixed_antenna_label_off);
        boolean[] values = {set.ant1, set.ant2, set.ant3, set.ant4, set.ant5, set.ant6, set.ant7, set.ant8};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) {
                sb.append("  ");
            }
            sb.append(getString(ANTENNA_LABEL_RES[i])).append(' ').append(values[i] ? on : off);
        }
        return sb.toString();
    }

    private String formatInputLine(int inputIndex, int state) {
        String s;
        if (state == 1) {
            s = getString(R.string.fixed_gpio_state_high);
        } else if (state == 0) {
            s = getString(R.string.fixed_gpio_state_low);
        } else {
            s = getString(R.string.fixed_gpio_state_unknown);
        }
        return getString(R.string.fixed_gpio_in_fmt, inputIndex, s);
    }

    private void initGpioUi() {
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            gpioOutButton(idx).setOnClickListener(v -> toggleGpioOut(idx));
            updateOutButtonLabel(idx);
        }
        binding.btnFixedGpioBeep.setOnClickListener(v -> {
            beepOn = !beepOn;
            gpio.setBeep(beepOn ? 1 : 0);
            binding.btnFixedGpioBeep.setText(beepOn ? R.string.fixed_gpio_beep_off : R.string.fixed_gpio_beep_on);
        });
        binding.btnFixedGpioBlueLed.setOnClickListener(v -> {
            blueLedOn = !blueLedOn;
            gpio.setBlueLed(blueLedOn ? 1 : 0);
            binding.btnFixedGpioBlueLed.setText(blueLedOn ? R.string.fixed_gpio_blue_led_off : R.string.fixed_gpio_blue_led_on);
        });
        binding.btnFixedGpioOrangeLed.setOnClickListener(v -> {
            orangeLedOn = !orangeLedOn;
            gpio.setOrangeLed(orangeLedOn ? 1 : 0);
            binding.btnFixedGpioOrangeLed.setText(orangeLedOn ? R.string.fixed_gpio_orange_led_off : R.string.fixed_gpio_orange_led_on);
        });
        binding.tvFixedGpioIn1.setText(formatInputLine(1, -1));
        binding.tvFixedGpioIn2.setText(formatInputLine(2, -1));
        binding.tvFixedGpioIn3.setText(formatInputLine(3, -1));
        binding.tvFixedGpioIn4.setText(formatInputLine(4, -1));

        binding.btnFixedGpioStartMonitor.setOnClickListener(v -> {
            gpio.setGpioInputListener(gpioInputListener);
            gpio.startMonitoring();
            Toast.makeText(this, R.string.fixed_gpio_start_monitor, Toast.LENGTH_SHORT).show();
        });
        binding.btnFixedGpioStopMonitor.setOnClickListener(v -> {
            gpio.stopMonitoring();
            gpio.setGpioInputListener(null);
            Toast.makeText(this, R.string.fixed_gpio_stop_monitor, Toast.LENGTH_SHORT).show();
        });
    }

    private AppCompatButton gpioOutButton(int index) {
        switch (index) {
            case 0:
                return binding.btnFixedGpioOut1;
            case 1:
                return binding.btnFixedGpioOut2;
            case 2:
                return binding.btnFixedGpioOut3;
            case 3:
            default:
                return binding.btnFixedGpioOut4;
        }
    }

    private void toggleGpioOut(int index) {
        outLevels[index] = outLevels[index] == 0 ? 1 : 0;
        int v = outLevels[index];
        switch (index) {
            case 0:
                gpio.setOut1(v);
                break;
            case 1:
                gpio.setOut2(v);
                break;
            case 2:
                gpio.setOut3(v);
                break;
            case 3:
                gpio.setOut4(v);
                break;
            default:
                break;
        }
        updateOutButtonLabel(index);
    }

    private void updateOutButtonLabel(int index) {
        gpioOutButton(index).setText(getString(R.string.fixed_gpio_out_fmt, index + 1, outLevels[index]));
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        gpio.stopMonitoring();
        gpio.setGpioInputListener(null);
        binding = null;
        super.onDestroy();
    }

    @Override
    public void onTabSelected() {
    }

    @Override
    public void onTabUnselected() {
    }
}
