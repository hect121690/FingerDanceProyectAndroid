package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.Product;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.listeners.SessionTransferURLListener;
import mega.privacy.android.app.lollipop.managerSections.UpgradeAccountFragmentLollipop;
import nz.mega.sdk.MegaPricing;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ChooseAccountFragmentLollipop extends UpgradeAccountFragmentLollipop implements View.OnClickListener {

    private Toolbar tB;

    public ArrayList<Product> accounts;

    private TextView storageSectionFree;
    private TextView bandwidthSectionFree;
    private TextView achievementsSectionFree;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        myAccountInfo = app.getMyAccountInfo();

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        final DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
        if (dbH.getCredentials() == null){
//            megaApi.localLogout();
//            AccountController aC = new AccountController(context);
//            aC.logout(context, megaApi, megaChatApi, false);
            //Show Login Fragment
            ((LoginActivityLollipop)context).showFragment(LOGIN_FRAGMENT);
        }

        accounts = new ArrayList<>();

        View v = inflater.inflate(R.layout.fragment_choose_account, container, false);

        tB =  v.findViewById(R.id.toolbar_choose_account);
        ((LoginActivityLollipop) context).showAB(tB);

        scrollView = v.findViewById(R.id.scroll_view_choose_account);
        new ListenScrollChangesHelper().addViewToListen(scrollView, (v1, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollView.canScrollVertically(-1)) {
                tB.setElevation(px2dp(4, outMetrics));
            } else {
                tB.setElevation(0);
            }
        });

        //FREE ACCOUNT
        RelativeLayout freeLayout = v.findViewById(R.id.choose_account_free_layout);
        freeLayout.setOnClickListener(this);
        TextView titleFree = v.findViewById(R.id.choose_account_free_title_text);
        titleFree.setText(getString(R.string.free_account).toUpperCase());
        storageSectionFree = v.findViewById(R.id.storage_free);
        bandwidthSectionFree = v.findViewById(R.id.bandwidth_free);
        achievementsSectionFree = v.findViewById(R.id.achievements_free);
        //END -- PRO LITE ACCOUNT

        //PRO LITE ACCOUNT
        proLiteLayout = v.findViewById(R.id.choose_account_prolite_layout);
        proLiteLayout.setOnClickListener(this);
        TextView titleProLite = v.findViewById(R.id.choose_account_prolite_title_text);
        titleProLite.setText(getString(R.string.prolite_account).toUpperCase());
        monthSectionLite = v.findViewById(R.id.month_lite);
        storageSectionLite = v.findViewById(R.id.storage_lite);
        bandwidthSectionLite = v.findViewById(R.id.bandwidth_lite);
        //END -- PRO LITE ACCOUNT

        //PRO I ACCOUNT
        pro1Layout = v.findViewById(R.id.choose_account_pro_i_layout);
        pro1Layout.setOnClickListener(this);
        TextView titlePro1 = v.findViewById(R.id.choose_account_pro_i_title_text);
        titlePro1.setText(getString(R.string.pro1_account).toUpperCase());
        monthSectionPro1 = v.findViewById(R.id.month_pro_i);
        storageSectionPro1 = v.findViewById(R.id.storage_pro_i);
        bandwidthSectionPro1 = v.findViewById(R.id.bandwidth_pro_i);
        //END -- PRO I ACCOUNT

        //PRO II ACCOUNT
        pro2Layout = v.findViewById(R.id.choose_account_pro_ii_layout);
        pro2Layout.setOnClickListener(this);
        TextView titlePro2 = v.findViewById(R.id.choose_account_pro_ii_title_text);
        titlePro2.setText(getString(R.string.pro2_account).toUpperCase());
        monthSectionPro2 = v.findViewById(R.id.month_pro_ii);
        storageSectionPro2 = v.findViewById(R.id.storage_pro_ii);
        bandwidthSectionPro2 = v.findViewById(R.id.bandwidth_pro_ii);
        //END -- PRO II ACCOUNT

        //PRO III ACCOUNT
        pro3Layout = v.findViewById(R.id.choose_account_pro_iii_layout);
        pro3Layout.setOnClickListener(this);
        TextView titlePro3 = v.findViewById(R.id.choose_account_pro_iii_title_text);
        titlePro3.setText(getString(R.string.pro3_account).toUpperCase());
        monthSectionPro3 = v.findViewById(R.id.month_pro_iii);
        storageSectionPro3 = v.findViewById(R.id.storage_pro_iii);
        bandwidthSectionPro3 = v.findViewById(R.id.bandwidth_pro_iii);
        //END -- PRO III ACCOUNT

        //BUSINESS
        businessLayout = v.findViewById(R.id.choose_account_business_layout);
        businessLayout.setOnClickListener(this);
        monthSectionBusiness = v.findViewById(R.id.month_business);
        storageSectionBusiness = v.findViewById(R.id.storage_business);
        bandwidthSectionBusiness = v.findViewById(R.id.bandwidth_business);
        //END -- BUSINESS

        setPricingInfo();
        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.choose_account_free_layout:
                onFreeClick();
                break;
            case R.id.choose_account_prolite_layout:
                onUpgradeLiteClick();
                break;
            case R.id.choose_account_pro_i_layout:
                onUpgrade1Click();
                break;
            case R.id.choose_account_pro_ii_layout:
                onUpgrade2Click();
                break;
            case R.id.choose_account_pro_iii_layout:
                onUpgrade3Click();
                break;
            case R.id.choose_account_business_layout:
                megaApi.getSessionTransferURL(REGISTER_BUSINESS_ACCOUNT, new SessionTransferURLListener(context));
                break;
        }
    }

    void onFreeClick(){
        logDebug("onFreeClick");

        Intent intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("firstLogin", true);
        intent.putExtra("upgradeAccount", false);
        intent.putExtra("newAccount", true);
        intent.putExtra(ManagerActivityLollipop.NEW_CREATION_ACCOUNT, true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    private void onUpgrade1Click() {
//		((ManagerActivity)context).showpF(1, accounts);
        logDebug("onUpgrade1Click");

        Intent intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", PRO_I);
        intent.putExtra("newAccount", true);
        intent.putExtra(ManagerActivityLollipop.NEW_CREATION_ACCOUNT, true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    private void onUpgrade2Click() {
//		((ManagerActivity)context).showpF(2, accounts);
        logDebug("onUpgrade2Click");

        Intent intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", PRO_II);
        intent.putExtra("newAccount", true);
        intent.putExtra(ManagerActivityLollipop.NEW_CREATION_ACCOUNT, true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    private void onUpgrade3Click() {
//		((ManagerActivity)context).showpF(3, accounts);
        logDebug("onUpgrade3Click");

        Intent intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", PRO_III);
        intent.putExtra("newAccount", true);
        intent.putExtra(ManagerActivityLollipop.NEW_CREATION_ACCOUNT, true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    private void onUpgradeLiteClick(){
//		((ManagerActivity)context).showpF(4, accounts);
        logDebug("onUpgradeLiteClick");

        Intent intent = new Intent(context,ManagerActivityLollipop.class);
        intent.putExtra("upgradeAccount", true);
        intent.putExtra("accountType", PRO_LITE);
        intent.putExtra("newAccount", true);
        intent.putExtra(ManagerActivityLollipop.NEW_CREATION_ACCOUNT, true);
        startActivity(intent);
        ((LoginActivityLollipop)context).finish();
    }

    void setPricingInfo() {

        MegaPricing p = myAccountInfo.getPricing();
        if (p == null) {
            logWarning("Return - getPricing NULL");
            return;
        }

        //Currently the API side doesn't return this value, so we have to hardcode.
        String textToShowFreeStorage = "[A] 50 GB [/A]" + getString(R.string.label_storage_upgrade_account) + " ";
        try {
            textToShowFreeStorage = textToShowFreeStorage.replace("[A]", "<font color=\'#000000\'>");
            textToShowFreeStorage = textToShowFreeStorage.replace("[/A]", "</font>");
        } catch (Exception e) {
            logWarning("Exception formatting string", e);
        }
        storageSectionFree.setText(getSpannedHtmlText(textToShowFreeStorage + "<sup><small><font color=\'#ff333a\'>1</font></small></sup>"));

        String textToShowFreeBandwidth = "[A] " + getString(R.string.limited_bandwith).toUpperCase() + "[/A] " + getString(R.string.label_transfer_quota_upgrade_account);
        try {
            textToShowFreeBandwidth = textToShowFreeBandwidth.replace("[A]", "<font color=\'#000000\'>");
            textToShowFreeBandwidth = textToShowFreeBandwidth.replace("[/A]", "</font>");
        } catch (Exception e) {
            logWarning("Exception formatting string", e);
        }
        bandwidthSectionFree.setText(getSpannedHtmlText(textToShowFreeBandwidth));

        achievementsSectionFree.setText(getSpannedHtmlText("<sup><small><font color=\'#ff333a\'>1</font></small></sup> " + getString(R.string.footnote_achievements)));

        setProPricingInfo();
    }
}
