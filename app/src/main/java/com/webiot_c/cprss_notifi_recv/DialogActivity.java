package com.webiot_c.cprss_notifi_recv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class DialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String title = getIntent().getStringExtra("title");
        String message = getIntent().getStringExtra("mes");
        setContentView(R.layout.activity_dialog);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        DialogActivity.this.finishAndRemoveTask();
                    }
                })
                .show();
    }
}
