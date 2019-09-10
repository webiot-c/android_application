package com.webiot_c.cprss_notifi_recv;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class HelpActivity extends AppCompatActivity {

    public static final String HELP_TITLE = "com.webiot_c.cprss_notifi_recv.HelpActivity.help_title";
    public static final String GIF_RESOURCE = "com.webiot_c.cprss_notifi_recv.HelpActivity.gif_resource";
    public static final String HELP_CONTEXT = "com.webiot_c.cprss_notifi_recv.HelpActivity.help_context";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        setContentView(R.layout.activity_help);

        String help_title   = getIntent().getStringExtra(HELP_TITLE);
        int gif_resource    = getIntent().getIntExtra(GIF_RESOURCE, 0);
        String help_context = getIntent().getStringExtra(HELP_CONTEXT);

        ImageView imageView = (ImageView) findViewById(R.id.desc_gif);

        Glide.with(this).load(gif_resource)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .into(imageView);


        ((TextView) findViewById(R.id.descui_title)).setText(help_title);
        ((TextView) findViewById(R.id.descui_context)).setText(Html.fromHtml(help_context));

    }
}
