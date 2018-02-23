package mega.privacy.android.app.lollipop.qrcode;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.QRCodeSaveBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

/**
 * Created by mega on 12/01/18.
 */

public class QRCodeActivity extends PinActivityLollipop implements MegaRequestListenerInterface{

    private static int REQUEST_DOWNLOAD_FOLDER = 1000;

    private Toolbar tB;
    private ActionBar aB;

    private MenuItem shareMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem settingsMenuItem;
    private MenuItem resetQRMenuItem;

    private TabLayout tabLayoutQRCode;
    private ViewPager viewPagerQRCode;

    private ScanCodeFragment scanCodeFragment;
    private MyCodeFragment myCodeFragment;

    private QRCodePageAdapter qrCodePageAdapter;

    private DrawerLayout drawerLayout;

    private int qrCodeFragment;

    MegaApiAndroid megaApi;

    private boolean contacts = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        contacts = getIntent().getBooleanExtra("contacts", false);

        setContentView(R.layout.activity_qr_code);

        tB = (Toolbar) findViewById(R.id.toolbar);
        if(tB==null){
            log("Tb is Null");
            return;
        }

        if (megaApi == null) {
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        tB.setTitle(getString(R.string.section_qr_code));

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        qrCodePageAdapter =new QRCodePageAdapter(getSupportFragmentManager(),this);

        tabLayoutQRCode =  (TabLayout) findViewById(R.id.sliding_tabs_qr_code);
        viewPagerQRCode = (ViewPager) findViewById(R.id.qr_code_tabs_pager);
        viewPagerQRCode.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                supportInvalidateOptionsMenu();

                if (position == 0) {
                    qrCodeFragment = 0;
                    myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
                }
                else {
                    qrCodeFragment = 1;
                    ScanCodeFragment.scannerView.startCamera();
                    scanCodeFragment = (ScanCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 1);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        viewPagerQRCode.setAdapter(qrCodePageAdapter);
        tabLayoutQRCode.setupWithViewPager(viewPagerQRCode);
        if (contacts){
            viewPagerQRCode.setCurrentItem(1);
        }
        else {
            viewPagerQRCode.setCurrentItem(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_qr_code, menu);

        shareMenuItem = menu.findItem(R.id.qr_code_share);
        saveMenuItem = menu.findItem(R.id.qr_code_save);
        settingsMenuItem = menu.findItem(R.id.qr_code_settings);
        resetQRMenuItem = menu.findItem(R.id.qr_code_reset);

        Drawable share = getResources().getDrawable(R.drawable.ic_social_share_white);
        share.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        shareMenuItem.setIcon(share);

        switch (qrCodeFragment) {
            case 0: {
                shareMenuItem.setVisible(true);
                saveMenuItem.setVisible(true);
                settingsMenuItem.setVisible(true);
                resetQRMenuItem.setVisible(true);
                break;
            }
            case 1: {
                shareMenuItem.setVisible(false);
                saveMenuItem.setVisible(false);
                settingsMenuItem.setVisible(false);
                resetQRMenuItem.setVisible(false);
                break;
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.qr_code_share: {
                shareQR ();
                break;
            }
            case R.id.qr_code_save: {
                QRCodeSaveBottomSheetDialogFragment qrCodeSaveBottomSheetDialogFragment = new QRCodeSaveBottomSheetDialogFragment();
                qrCodeSaveBottomSheetDialogFragment.show(getSupportFragmentManager(), qrCodeSaveBottomSheetDialogFragment.getTag());

                break;
            }
            case R.id.qr_code_settings: {
                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("drawerItem", ManagerActivityLollipop.DrawerItem.SETTINGS);
                intent.putExtras(bundle);
                startActivity(intent);
                this.finish();
                break;
            }
            case R.id.qr_code_reset: {
                resetQR();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == Activity.RESULT_OK && intent != null) {
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if (parentPath != null){
                File qrFile = null;
                if (megaApi == null) {
                    megaApi = ((MegaApplication) getApplication()).getMegaApi();
                }
                String myEmail = megaApi.getMyEmail();
                if (this.getExternalCacheDir() != null){
                    qrFile = new File(this.getExternalCacheDir().getAbsolutePath(), myEmail + "QRcode.jpg");
                }
                else{
                    qrFile = new File(this.getCacheDir().getAbsolutePath(), myEmail + "QRcode.jpg");
                }
                if (qrFile != null && qrFile.exists()){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                        if (!hasStoragePermission) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_WRITE_STORAGE);
                        }
                    }

                    double availableFreeSpace = Double.MAX_VALUE;
                    try{
                        StatFs stat = new StatFs(parentPath);
                        availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
                    }
                    catch(Exception ex){}

                    if(availableFreeSpace < qrFile.length()) {
                        showSnackbar(getString(R.string.error_not_enough_free_space));
                        return;
                    }
                    File newQrFile = new File(parentPath, myEmail + "QRcode.jpg");
                    if (newQrFile != null && !newQrFile.exists()){
                        try {
                            newQrFile.createNewFile();
                            FileChannel src = new FileInputStream(qrFile).getChannel();
                            FileChannel dst = new FileOutputStream(newQrFile).getChannel();
                            dst.transferFrom(src, 0, src.size());       // copy the first file to second.....
                            src.close();
                            dst.close();
                            showSnackbar(getString(R.string.success_download_qr, parentPath));
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        showSnackbar(getString(R.string.error_download_qr));
                    }
                }
                else {
                    showSnackbar(getString(R.string.error_download_qr));
                }
           }
        }
    }

    public void shareQR () {
        log("shareQR");

        if (myCodeFragment == null) {
            log("MyCodeFragment is NULL");
            myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
        }
        File qrCodeFile = myCodeFragment.queryIfQRExists();
        if (qrCodeFile != null && qrCodeFile.exists()){
            Intent share = new Intent(android.content.Intent.ACTION_SEND);
            share.setType("image/*");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                log("Use provider to share");
                Uri uri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider",qrCodeFile);
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            else{
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + qrCodeFile));
            }
            startActivity(Intent.createChooser(share, getString(R.string.context_share)));
        }
        else {
            showSnackbar(getString(R.string.error_share_qr));
        }
    }

    public void resetQR () {
        log("resetQR");

        if (myCodeFragment == null) {
            log("MyCodeFragment is NULL");
            myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
        }
        myCodeFragment.resetQRCode();
    }

    public void resetSuccessfully (boolean success) {
        log("resetSuccessfully");
        if (success){
            showSnackbar(getString(R.string.qrcode_reset_successfully));
        }
        else {
            showSnackbar(getString(R.string.qrcode_reset_not_successfully));
        }
    }

    public void showSnackbar(String s){
        log("showSnackbar");
        Snackbar snackbar = Snackbar.make(drawerLayout, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    public static void log(String message) {
        Util.log("QRCodeActivity", message);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        if (e.getErrorCode() == MegaError.API_OK){
            log("OK INVITE CONTACT: "+request.getEmail());
            if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD){
//                showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                if (scanCodeFragment == null) {
                    log("MyCodeFragment is NULL");
                    scanCodeFragment = (ScanCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 1);
                }
                scanCodeFragment.showAlertDialog(R.string.invite_sent, R.string.invite_sent_text, true);
            }
        }
        else if (e.getErrorCode() == MegaError.API_EEXIST){
            if (scanCodeFragment == null) {
                log("MyCodeFragment is NULL");
                scanCodeFragment = (ScanCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 1);
            }
            scanCodeFragment.showAlertDialog(R.string.invite_not_sent, R.string.invite_not_sent_text_already_contact, true);
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
