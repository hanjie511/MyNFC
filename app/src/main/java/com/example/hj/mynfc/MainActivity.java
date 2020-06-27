package com.example.hj.mynfc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Toast;

import java.io.StringReader;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private NfcAdapter nfcAdapter;
    private IntentFilter intentFilter[];
    private String [][] techListsArray;
    private PendingIntent pendingIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=MainActivity.this;
        initNfcAdapter(context);
        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
                                           You should specify only the ones that you need. */
        }
        catch (Exception e) {
            throw new RuntimeException("fail", e);
        }
        intentFilter = new IntentFilter[] {ndef,new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED) };
        techListsArray = new String[][] { new String[] { NfcF.class.getName() },new String[] { android.nfc.tech.NfcV.class.getName() },
                new String[] { android.nfc.tech.NfcA.class.getName() }};
    }
    private void initNfcAdapter(Context context){
         nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (nfcAdapter == null) {
            Toast.makeText(context, "设备不支持NFC功能!", Toast.LENGTH_SHORT).show();
        } else {
            if (!nfcAdapter.isEnabled()) {
                Toast.makeText(context, "请打开设备的NFC开关", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "NFC功能已打开!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        System.out.println("action:"+intent.getAction());
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String [] str=tagFromIntent.getTechList();
        Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        System.out.println("rawMsgs.length:"+rawMsgs.length);
        for (String s:str){
            System.out.println("str:"+s);
        }
        String[] hexdec = bytes2hex(tagFromIntent.getId());
        String tagId =hexdec[0];
        System.out.println("tagId:"+tagId);
    }
    public static String[] bytes2hex(byte[] bytes)
    {
        String[] hexdec = new String[2];
        StringBuilder sb = new StringBuilder();
        StringBuilder sb10 = new StringBuilder();
        String tmp = null;
        int tmp10 = 0;
        for (byte b : bytes)
        {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            tmp10 = 0xFF&b;
            sb10.append(tmp10);
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1)// 每个字节8为，转为16进制标志，2个16进制位
            {
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        hexdec[0] = sb.toString();
        hexdec[1] = sb10.toString();
        return hexdec;

    }

    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, techListsArray);
    }
}
