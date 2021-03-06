package com.fast.access.kam.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bowyer.app.fabtoolbar.FabToolbar;
import com.fast.access.kam.AppController;
import com.fast.access.kam.R;
import com.fast.access.kam.activities.base.BaseActivity;
import com.fast.access.kam.global.adapter.AppDetailsAdapter;
import com.fast.access.kam.global.helper.AppHelper;
import com.fast.access.kam.global.helper.FileUtil;
import com.fast.access.kam.global.model.AppDetailModel;
import com.fast.access.kam.global.model.AppsModel;
import com.fast.access.kam.global.model.EventsModel;
import com.fast.access.kam.widget.ParallaxRecyclerView;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by Kosh on 8/18/2015. copyrights are reserved
 */
public class AppDetailsActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.appbar)
    AppBarLayout appbar;
    @Bind(R.id.main_content)
    CoordinatorLayout mainContent;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.fabtoolbar)
    FabToolbar fabtoolbar;
    @Bind(R.id.appDetails)
    ParallaxRecyclerView appDetails;
    @Bind(R.id.delete)
    ImageButton delete;
    @Bind(R.id.extract)
    ImageButton extract;
    @Bind(R.id.share)
    ImageButton share;
    private AppsModel appsModel;
    private final int APP_RESULT = 1001;
    private LinearLayoutManager manager;

    @OnClick(R.id.fab)
    public void onExpand() {
        fabtoolbar.expandFab();
    }

    @OnClick(R.id.extract)
    public void onExtract() {
        FileUtil fileUtil = new FileUtil();
        File file = fileUtil.generateFile(appsModel.getAppName());
        try {
            File path = getApkFile();
            if (path.exists()) {
                FileUtils.copyFile(path, file);
                goToFolder("KAM/" + appsModel.getAppName() + ".apk");
            } else {
                showMessage("Could not find application file");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showMessage(e.getMessage() != null ? e.getMessage() : "Error Extracting App");
        }
        fabtoolbar.slideOutFab();
    }

    @OnClick(R.id.share)
    public void onShare() {
        share();
        fabtoolbar.slideOutFab();
    }

    @OnClick(R.id.delete)
    public void onDelete() {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(Uri.parse("package:" + appsModel.getPackageName()));
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        startActivityForResult(intent, APP_RESULT);
        fabtoolbar.slideOutFab();
    }

    @Override
    protected int layout() {
        return R.layout.application_details;
    }

    @Override
    protected boolean canBack() {
        return true;
    }

    @Override
    protected boolean hasMenu() {
        return false;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppController.getController().getBus().register(this);
        if (getIntent() == null || getIntent().getExtras() == null) {
            finish();
            return;
        }
        appsModel = getIntent().getExtras().getParcelable("app");
        if (appsModel != null) {
            manager = new LinearLayoutManager(this);
            appDetails.setItemAnimator(new DefaultItemAnimator());
            appDetails.setLayoutManager(manager);
            List<AppDetailModel> appDetailModelList = new ArrayList<>();
            AppDetailModel header = new AppDetailModel();
            header.setType(AppDetailModel.HEADER);
            header.setAppsModel(appsModel);
            appDetailModelList.add(header);
            List<String> permissions = AppHelper.getAppPermissions(this, appsModel.getPackageName());
            if (permissions != null && !permissions.isEmpty()) {
                for (String permission : permissions) {
                    if (permission != null) {
                        AppDetailModel per = new AppDetailModel();
                        per.setType(AppDetailModel.ITEMS);
                        per.setAppPermission(permission);
                        appDetailModelList.add(per);
                    }
                }
            }
            int backgroundColor = getResources().getColor(R.color.windows_background);
            if (AppHelper.isDarkTheme(this)) {
                backgroundColor = getResources().getColor(R.color.black);
            }
            AppDetailsAdapter appDetailsAdapter = new AppDetailsAdapter(appDetailModelList, backgroundColor);
            appDetails.setAdapter(appDetailsAdapter);

        } else {
            finish();
        }
        fabtoolbar.setFab(fab);
        fabtoolbar.setColor(AppHelper.getAccentColor(this));
        fab.setBackgroundTintList(ColorStateList.valueOf(AppHelper.getAccentColor(this)));
        fab.setRippleColor(AppHelper.getPrimaryColor(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == APP_RESULT) {
                onBackPressed();
            }
        }
    }

    /**
     * would love to use snakbar, but then the fabtoolbar layout make the fab overlay the snackbar.
     *
     * @param msg
     */
    private void showMessage(String msg) {
        Toast.makeText(AppDetailsActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    private File getApkFile() throws PackageManager.NameNotFoundException {
        if (appsModel.getFilePath() == null) {//making sure setting the path if doesn't exists
            ApplicationInfo packageInfo = getPackageManager().getApplicationInfo(appsModel.getPackageName(), 0);
            appsModel.setFilePath(packageInfo.sourceDir);
        }
        return new File(appsModel.getFilePath());
    }

    private void goToFolder(String location) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Hooray!, The APK extracted successfully at " + location)
                .setPositiveButton("Open Folder", new DialogInterface.OnClickListener() {
                    @SuppressLint("SdCardPath")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri kam = Uri.fromFile(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/KAM"));
                            intent.setData(kam);
                            startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showMessage("No Application found to open the folder.");
                        }
                    }
                }).setNegativeButton("Close", null)
                .show();
    }

    private void share() {
        try {
            File path = getApkFile();
            if (path.exists()) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(path));
                intent.setType("application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(intent, "Share " + appsModel.getAppName()));
            } else {
                showMessage("Could not find application file");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (AppController.getController().getBus().isRegistered(this)) {
            AppController.getController().getBus().unregister(this);
        }
        super.onDestroy();
    }

    public void onEvent(final EventsModel eventsModel) {
        if (eventsModel != null) {
            if (eventsModel.getEventType() != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (eventsModel.getEventType()) {
                            case DELETE:
                                if (appsModel != null) {
                                    if (eventsModel.getPackageName().equalsIgnoreCase(appsModel.getPackageName())) {
                                        finish();
                                        Toast.makeText(AppDetailsActivity.this, "Application uninstalled", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    finish();
                                }
                                break;

                        }
                    }
                });
            }
        }
    }


}
