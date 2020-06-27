# MyNFC
android的NFC开发入门
# NFC简述  
近距离无线通信 (NFC) 是一组近距离无线技术，通常只有在距离不超过 4 厘米时才能启动连接。借助 NFC，您可以在 NFC 标签与 Android 设备之间或者两台 Android 设备之间共享小型负载。标签的复杂度可能各有不同。简单标签仅提供读取和写入语义，有时可使用一次性可编程区域将卡片设置为只读。较复杂的标签可提供数学运算，还可使用加密硬件来验证对扇区的访问权限。最为复杂的标签可包含操作环境，允许与针对标签执行的代码进行复杂的互动。存储在标签中的数据也可以采用多种格式编写，但许多 Android 框架 API 都基于名为 NDEF（NFC 数据交换格式）的 NFC Forum 标准。  
支持 NFC 的 Android 设备同时支持以下三种主要操作模式：  
* 读取器/写入器模式：支持 NFC 设备读取和/或写入被动 NFC 标签和贴纸。  
* 点对点模式：支持 NFC 设备与其他 NFC 对等设备交换数据；Android Beam 使用的就是此操作模式。  
* 卡模拟模式：支持 NFC 设备本身充当 NFC 卡。然后，可以通过外部 NFC 读取器（例如 NFC 销售终端）访问模拟 NFC 卡。  
# NFC在项目中的应用
## 一、标签调度系统
Android 设备通常会在屏幕解锁后查找 NFC 标签，除非设备的“设置”菜单中停用了 NFC 功能。在 Android 设备发现 NFC 标签后，期望的行为就是让最合适的 Activity 来处理该 Intent，而不是询问用户应使用哪个应用。由于设备需要在非常近的范围内扫描 NFC 标签，因此，让用户手动选择 Activity 可能会迫使他们将设备从标签处移开并导致连接中断。为帮助我们解决这一问题，Android 提供了一个特殊的标签调度系统，用于分析扫描到的 NFC 标签、解析它们并尝试找到对扫描到的数据感兴趣的应用。这个标签调度系统通过以下操作来实现这些目的：  
* 解析 NFC 标签并确定 MIME 类型或 URI（后者用于标识标签中的数据负载）。  
* 将 MIME 类型或 URI 与负载一起封装到 Intent 中。  
* 根据 Intent 启动 Activity。  
## 二、如何将 NFC 标签分发到应用  

当标签调度系统创建完用于封装 NFC 标签及其标识信息的 Intent 后，它会将该 Intent 发送给感兴趣的应用，由这些应用对其进行过滤。如果有多个应用可处理该 Intent，系统会显示 Activity 选择器，供用户选择要使用的 Activity。标签调度系统定义了三种 Intent，按优先级从高到低列出如下：  
* 1、ACTION_NDEF_DISCOVERED：如果扫描到包含 NDEF 负载的标签，并且可识别其类型，则使用此 Intent 启动 Activity。这是优先级最高的 Intent，标签调度系统会尽可能尝试使用此 Intent 启动 Activity，在行不通时才会尝试使用其他 Intent。  

* 2、ACTION_TECH_DISCOVERED：如果没有登记要处理 ACTION_NDEF_DISCOVERED Intent 的 Activity，则标签调度系统会尝试使用此 Intent 来启动应用。此外，如果扫描到的标签包含无法映射到 MIME 类型或 URI 的 NDEF 数据，或者该标签不包含 NDEF 数据，但它使用了已知的标签技术，那么也会直接启动此 Intent（无需先启动 ACTION_NDEF_DISCOVERED）。  

* 3、ACTION_TAG_DISCOVERED：如果没有处理 ACTION_NDEF_DISCOVERED 或者 ACTION_TECH_DISCOVERED Intent 的 Activity，则使用此 Intent 启动 Activity。
### 标签调度系统的基本工作方式如下：  
在解析 NFC 标签（ACTION_NDEF_DISCOVERED 或 ACTION_TECH_DISCOVERED）时，尝试使用由标签调度系统创建的 Intent 启动 Activity。如果不存在过滤该 Intent 的 Activity，则尝试使用下一优先级的 Intent（ACTION_TECH_DISCOVERED 或 ACTION_TAG_DISCOVERED）启动 Activity，直到应用过滤该 Intent 或者直到标签调度系统试完所有可能的 Intent。如果没有应用过滤任何 Intent，则不执行任何操作。  
![](https://developer.android.com/images/nfc_tag_dispatch.png)  
#### Notice:尽可能使用 NDEF 消息和 ACTION_NDEF_DISCOVERED Intent，因为它是三种 Intent 中最具体的一种。与其他两种 Intent 相比，此 Intent 可使您在更恰当的时间启动应用，从而为用户带来更好的体验。  
## 三、使用前台调度系统  
借助前台调度系统，Activity 可以拦截 Intent 并声明自己可优先于其他 Activity 处理同一 Intent。使用此系统涉及为 Android 系统构造一些数据结构，以便将合适的 Intent 发送到您的应用。要启用前台调度系统，请执行以下操作：
### 在 Activity 的 onCreate() 方法中添加以下代码：
* 1、创建一个 PendingIntent 对象，这样 Android 系统会使用扫描到的标签的详情对其进行填充。  
```java
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);  
```
* 2、声明 Intent 过滤器，以处理您要拦截的 Intent。前台调度系统会对照设备扫描标签时所获得的 Intent 来检查所指定的 Intent 过滤器。如果匹配，那么应用会处理该 Intent。如果不匹配，那么前台调度系统会回退到 Intent 调度系统。指定 Intent 过滤器和技术过滤器的 null 数组，以指明要过滤所有回退到 TAG_DISCOVERED Intent 的标签。以下代码段会处理 NDEF_DISCOVERED 的所有 MIME 类型。您应只处理需要的内容。  
```java
IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
       try {
            ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
                                           You should specify only the ones that you need. */
        }
        catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
       intentFiltersArray = new IntentFilter[] {ndef, };
```  
* 3、设置应用要处理的一组标签技术。调用 Object.class.getName() 方法以获取要支持的技术的类。  
```java
techListsArray = new String[][] { new String[] { NfcF.class.getName() } };
```  
### 替换以下 Activity 生命周期回调，并添加相应逻辑，以分别在 Activity 失去 (onPause()) 焦点和重新获得 (onResume()) 焦点时启用和停用前台调度。enableForegroundDispatch() 必须从主线程调用，并且只能在 Activity 在前台运行时调用（在 onResume() 中调用可确保这一点）。您还需要实现 onNewIntent 回调以处理扫描到的 NFC 标签中的数据。  
```java
public void onPause() {
        super.onPause();
        adapter.disableForegroundDispatch(this);
    }

    public void onResume() {
        super.onResume();
        adapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }

    public void onNewIntent(Intent intent) {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        //do something with tagFromIntent
    }
```
## 四、在项目中的应用
### 在 AndroidManifest.xml 文件中声明以下内容，然后才能访问设备的 NFC 硬件并正确处理 NFC Intent：
```java
<uses-permission android:name="android.permission.NFC" /> 
<uses-feature android:name="android.hardware.nfc" android:required="true" /> //使用<uses-feature />，当将其require属性设置为true时，表示当设备具有NFC这一功能时，才允许用户安装该应用。如果您的应用使用 NFC 功能，但该功能对您的应用来说并不重要，您可以省略 uses-feature 元素，并在运行时通过检查 getDefaultAdapter() 是否为 null 来了解 NFC 的可用性。
```  
### 在 AndroidManifest.xml 文件中为需要处理NFC的Activity配置intent过滤器：
#### 过滤ACTION_NDEF_DISCOVERED  
要过滤 ACTION_NDEF_DISCOVERED Intent，请声明 Intent 过滤器以及要过滤的数据类型。以下示例展示了如何过滤 MIME 类型为 text/plain 的 ACTION_NDEF_DISCOVERED Intent：  
```java 
    <intent-filter>
        <action android:name="android.nfc.action.NDEF_DISCOVERED"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:mimeType="text/plain" />
    </intent-filter>
```
####  过滤ACTION_TECH_DISCOVERED
如果您的 Activity 过滤 ACTION_TECH_DISCOVERED Intent，您必须创建一个 XML 资源文件，用它在 tech-list 集内指定您的 Activity 所支持的技术。如果 tech-list 集是标签所支持的技术（可通过调用 getTechList() 来获取）的子集，则您的 Activity 会被视为一个匹配项。例如，如果扫描到的标签支持 MifareClassic、NdefFormatable 和 NfcA，为了使它们与您的 Activity 匹配，您的 tech-list 集必须指定所有这三种技术，或者其中的两种或一种技术。  
以下示例定义了所有技术。您可以移除自己不需要的技术。将此文件（你可以随便命名）保存到 <project-root>/res/xml 文件夹中。
```java
    <resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
        <tech-list>
            <tech>android.nfc.tech.IsoDep</tech>
            <tech>android.nfc.tech.NfcA</tech>
            <tech>android.nfc.tech.NfcB</tech>
            <tech>android.nfc.tech.NfcF</tech>
            <tech>android.nfc.tech.NfcV</tech>
            <tech>android.nfc.tech.Ndef</tech>
            <tech>android.nfc.tech.NdefFormatable</tech>
            <tech>android.nfc.tech.MifareClassic</tech>
            <tech>android.nfc.tech.MifareUltralight</tech>
        </tech-list>
    </resources>
```  
  在您的 AndroidManifest.xml 文件中，在 <activity> 元素的 <meta-data> 元素中指定您刚刚创建的资源文件，如以下示例所示：
```java
    <activity>
    ...
    <intent-filter>
        <action android:name="android.nfc.action.TECH_DISCOVERED"/>
    </intent-filter>

    <meta-data android:name="android.nfc.action.TECH_DISCOVERED"
        android:resource="@xml/nfc_tech_filter" />
    ...
    </activity>
```  
####  ACTION_TAG_DISCOVERED
要过滤 ACTION_TAG_DISCOVERED，请使用以下 Intent 过滤器：
```java
<intent-filter>
        <action android:name="android.nfc.action.TAG_DISCOVERED"/>
    </intent-filter>  
```  
### 在Activity中如何实现  
```java  
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
        //设置该Activity可以响应哪几种类型的Intent
        intentFilter = new IntentFilter[] {ndef,new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED) };
        //设置Activity可以响应哪几种NFC的标签读写技术
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
        //处理NFC标签的负载信息
        System.out.println("action:"+intent.getAction());
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String [] str=tagFromIntent.getTechList();
        Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
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
```



