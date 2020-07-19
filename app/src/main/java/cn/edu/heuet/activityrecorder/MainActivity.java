package cn.edu.heuet.activityrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks {
    private final String TAG = "MainActivity";
    private final int PERMISSION_STORAGE_CODE = 10001;
    private final String dirName = "ActivityRecorder";// 目录名
    private final String timeFileName = "ActivityTime.txt";// 活动时间文件名
    private final String typeFileName = "ActivityType.txt";// 活动类型文件名
    private Spinner spinnerActivity;
    private Button btnAddActivity;
    private Button btnStart;
    private TextView lblStatus;
    private static ArrayList<String> arrActivity = null;
    private FileUtils fileUtils;
    private String curActivity = null;// 当前活动

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerActivity = (Spinner) findViewById(R.id.spinnerActivity);
        btnAddActivity = (Button) findViewById(R.id.btnAddActivity);
        btnStart = (Button) findViewById(R.id.btnStart);
        lblStatus = (TextView) findViewById(R.id.lblStatus);
        lblStatus.setText("");

        btnAddActivity.setOnClickListener(new AddActiviyButtonClick());
        btnStart.setOnClickListener(new StartActiviyButtonClick());
        //请求权限
        storageTask();
    }

    private void initFile() {
        try {
            fileUtils = new FileUtils(dirName, timeFileName, typeFileName);
            fillSpinner();

            // 初使化
            curActivity = fileUtils.getCurrentActivityName();
            if (curActivity != null && !curActivity.equals("")) {
                int index = arrActivity.indexOf(curActivity);
                if (index != -1) {
                    spinnerActivity.setSelection(index);// 设置默认值
                    btnStart.setText(R.string.end);
                    lblStatus.setText(MessageFormat.format("{0}{1}", curActivity, getString(R.string.doing)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean hasStoragePermission() {
        return EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void fillSpinner() {
        arrActivity = fileUtils.getAllActivityType();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arrActivity);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_checked);
        spinnerActivity.setAdapter(adapter);

    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            Toast.makeText(
                    this,
                    hasStoragePermission() + "",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    @AfterPermissionGranted(PERMISSION_STORAGE_CODE)
    public void storageTask() {
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
            initFile();
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this,
                    getString(R.string.permission_storage),
                    PERMISSION_STORAGE_CODE, perms);
        }
    }

    @Override
    public void onRationaleAccepted(int requestCode) {
        Log.d(TAG, "onRationaleAccepted:" + requestCode);
    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Log.d(TAG, "onRationaleDenied:" + requestCode);
    }

    class StartActiviyButtonClick implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            if (arrActivity == null || arrActivity.size() == 0) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("没有活动类型，请先添加!")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("确定", null).show();
                return;
            }
            if (curActivity == null || curActivity.equals("")) {// 当前没有活动在执行，则开始
                curActivity = spinnerActivity.getSelectedItem().toString();
                SaveData("start");
                btnStart.setText(R.string.end);
                lblStatus.setText(curActivity + "中...");

            } else {// 当前有活动在执行，则停止
                SaveData("stop");
                btnStart.setText(R.string.start);
                lblStatus.setText("");
                curActivity = null;
            }

        }

        // 保存数据
        private void SaveData(String status) {
            String time = new SimpleDateFormat("yyyyMMddHHmmss",
                    Locale.getDefault()).format(new Date());
            String msg = curActivity + "," + status + "," + time;
            fileUtils.appendLine(msg);
        }
    }

    class AddActiviyButtonClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            LayoutInflater factory = LayoutInflater.from(MainActivity.this);// 提示框
            final View view = factory.inflate(R.layout.activity_add, null);// 这里必须是final的???
            final EditText edit = (EditText) view
                    .findViewById(R.id.txtaddactivityname);// 获得输入框对象

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.inputactivityname)
                    // 提示框标题
                    .setView(view)
                    .setPositiveButton(
                            R.string.ok,// 提示框的两个按钮
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    // 事件
                                    String newActivityName = edit.getText()
                                            .toString();
                                    newActivityName = newActivityName.trim();
                                    if (newActivityName.equals("")) {
                                        return;// 输入为空时不保存
                                    }
                                    boolean res = fileUtils
                                            .addActivityType(newActivityName);
                                    if (res) {
                                        fillSpinner();
                                    }
                                    int index = arrActivity
                                            .indexOf(newActivityName);
                                    if (index != -1) {
                                        spinnerActivity.setSelection(index);// 设置默认选择添加项
                                    }
                                }
                            }).setNegativeButton(R.string.cancel, null)
                    .create().show();
        }
    }
}