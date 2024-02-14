package com.example.flower_reco;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ImageView image_flower;//加载所要识别的图片的ImageView
    private Button upload_local;//上传本地图片的按钮
    private Button upload_takePhoto;//上传拍照图片的按钮
    private Button start_reco;//开始识别的按钮
    private Interpreter tflite;//tensorflow lite解释器
    public static final String TAG = "Flower_Recognize.MainActivity";//Log打印信息的标识
    private boolean load_result;//标志模型文件tflite是否加载成功
    public static final String MODEL_PATH = "graph.tflite";//.tflite文件名称——在assets目录下
    public static final String LABEL_PATH = "graph.txt";//label文件名称——在assets目录下
    private List<String> labelList;//10个花朵的label
    private final int[] dims = {1, 224, 224, 3};//输入图片tensor的维度
    private ActivityResultLauncher<Intent> selectPicLauncher;//负责启动ACTION_PICK的ResultLauncher
    private ActivityResultLauncher<Intent> takePhotoLauncher;//负责拍照的ResultLauncher
    private ActivityResultLauncher<Intent> activityLauncher;//负责跳转活动的ResultLauncher
    private static final int PERMISSION_CODE = 100;//请求权限的requestCode
    private String picPath = null;//所选的图片或者所拍摄保存的图片的Path
    private final PermissionUtil.OnPermissionsListener permissionsListener = new PermissionUtil.OnPermissionsListener() {//请求权限监听器
        /**
         * @function 所请求的权限已拥有时的回调函数
         */
        @Override
        public void onPermissionsOwned() {
            Log.i(TAG,"This permission has owned");
        }

        /**
         * @function 所请求的权限已被禁止时的回调函数
         * @param permissions 所请求的所有权限
         * @param grantResults 请求的结果 1允许，0禁止
         * @param pmList 未被允许的权限
         */
        @Override
        public void onPermissionsForbidden(String[] permissions, int[] grantResults, ArrayList<String> pmList) {
            Log.i(TAG,"These permissions have forbidden"+pmList.toString());
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("警告")
                    .setMessage("请前往设置中手动打开"+ pmList +"权限！")
                    .setPositiveButton("确定", (dialog1, which) -> {
                        //确定后不需要做什么，用户自己前往设置打开权限
                    })
                    .create();
            dialog.show();
        }

        /**
         * @function 所请求的权限被拒绝时的回调函数
         * @param permissions 所请求的所有权限
         * @param grantResults 请求的结果 1允许，0禁止
         * @param pmList 被拒绝的权限
         */
        @Override
        public void onPermissionsDenied(String[] permissions, int[] grantResults, ArrayList<String> pmList) {
            Log.i(TAG,"These permissions have denied"+pmList.toString());
            //重新请求权限
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示")
                    .setMessage("【"+ pmList +"】权限为应用必要权限，请授权")
                    .setPositiveButton("确定", (dialog12, which) -> {
                        String[] sList=pmList.toArray(new String[0]);
                        //重新申请权限,通过权限名的方式申请多组权限
                        PermissionUtil.requestByPermissionName(MainActivity.this,sList, PERMISSION_CODE,permissionsListener);
                    })
                    .create();
            dialog.show();
        }

        /**
         * @function 所请求的权限请求成功时的回调函数
         */
        @Override
        public void onPermissionsSucceed() {
            Log.i(TAG,"These permission‘s requests succeed");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViewAndVariable();
        clickView();
    }

    /**
     * @function 初始化控件以及变量
     */
    private void initViewAndVariable(){
        //API24以上系统分享支持file:///开头 忽略路径泄露风险 另一种方法是使用内容提供器解决
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        //初始化控件
        image_flower = findViewById(R.id.flower_upload);
        upload_local = findViewById(R.id.upload_local);
        upload_takePhoto = findViewById(R.id.upload_takePhoto);
        start_reco = findViewById(R.id.start_reco);

        initResultLauncher();//初始化ActivityResultLauncher类变量

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//请求权限
            requestPermissions();
        }

        loadModel();//加载tflite模型
        loadLabel();//加载标签
    }

    /**
     * @function 初始化ActivityResultLauncher类变量
     */
    private void initResultLauncher(){
        //ACTION_PICK Intent的返回回调处理
        selectPicLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    int resultCode = result.getResultCode();
                    if (resultCode == RESULT_OK){ //not calling setResult method,the resultCode is RESULT_OK
                        assert data != null;
                        handleImageOnKitKat(data);//获取所选择图片的路径并加载到ImageView控件中
                    }
                }
        );

        //ACTION_IMAGE_CAPTURE Intent的回调处理
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    if (resultCode == RESULT_OK){
                        Log.i(TAG,"saved pic's path:"+ picPath);//返回成功即说明图片已被保存到路径picPath中
                        displayImage(picPath);//直接加载即可
                    }
                }
        );

        //跳转Activity的回调处理
        activityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                }
        );
    }

    /**
     * @function 请求权限函数
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestPermissions(){
        String[] pgArray=new String[]{
                Manifest.permission_group.CAMERA,
                Manifest.permission_group.STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
        };//App所需要请求的权限数组
        Log.i(TAG,"进行[相机+存储]权限申请...");
        PermissionUtil.requestByGroupName(this,pgArray,PERMISSION_CODE,permissionsListener);//请求权限

        //目前Android声明上述权限后，也不能访问图库等存储，需要授予访问所有文件权限的权限——上市开发App不建议这种做法
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                Environment.isExternalStorageManager()) {
                Toast.makeText(this, "已获得访问所有文件权限", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("本程序需要您同意允许访问所有文件权限")
                    .setPositiveButton("确定", (dialog1, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        activityLauncher.launch(intent); //视为一次不需要处理返回结果的Activity跳转
                    }).create();
            dialog.show();
        }
    }

    /**
     * @function 重写权限请求回调处理函数
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);//继承之前的函数
        if (requestCode == PERMISSION_CODE) {//如果请求码是对应的，那么就请求对应的权限请求工具类的权限请求回调处理函数
            PermissionUtil.onRequestPermissionsResult(MainActivity.this, permissions, grantResults, permissionsListener, false);
        }
    }

    /**
     * @function 点击控件处理函数
     */
    private void clickView(){
        //点击上传本地图片
        upload_local.setOnClickListener(v -> openSysAlbum());

        //点击拍照
        upload_takePhoto.setOnClickListener(v -> openSysCamera());

        //点击开始识别
        start_reco.setOnClickListener(v -> {
            if (load_result && picPath !=null){
                Log.i(TAG,"开始识别图片...");
                Bitmap bitmap = ((BitmapDrawable)image_flower.getDrawable()).getBitmap();
                float [][][][] floatArr = getScaledMatrix(bitmap, dims);
                float [][]res = new float[1][10];
                long start = System.currentTimeMillis();
                tflite.run(floatArr,res);
                long end = System.currentTimeMillis();
                float max = 0;
                int maxIndex = 0;
                for (int i = 0; i < res[0].length; i++) {
                    if (res[0][i]>max){
                        max = res[0][i];
                        maxIndex = i;
                    }
                }
                Log.i(TAG,"分类最大值："+ max +"最大值索引："+ maxIndex +"用时(ms)："+ (end - start));
                Toast.makeText(MainActivity.this,"识别结果:"+labelList.get(maxIndex)+"识别时间:"+(end-start)+"ms",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this,FlowerInfoActivity.class);
                intent.putExtra("index",maxIndex);
                intent.putExtra("picPath",picPath);
                activityLauncher.launch(intent);
            } else {
                Toast.makeText(MainActivity.this,"未选择所要识别的图片或者模型加载失败",Toast.LENGTH_SHORT).show();
                Log.i(TAG,"未选择所要识别的图片或者模型加载失败");
            }
        });
    }

    /**
     * @function 加载tflite模型
     */
    private void loadModel() {
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            tflite = new Interpreter(loadModelFile(),options);
            Log.d(TAG, MainActivity.MODEL_PATH + " model load success");
            load_result = true;
        } catch (IOException e) {
            Log.d(TAG, MainActivity.MODEL_PATH + " model load fail");
            load_result = false;
            e.printStackTrace();
        }
    }

    /**
     * @function 读取.tflite文件
     * @return 字节序列
     * @throws IOException 会处罚文件IO异常
     */
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getApplicationContext().getAssets().openFd(MainActivity.MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * @function 加载花朵分类标签
     */
    private void loadLabel(){
        try {
            labelList = loadLabelList();
            Log.d(TAG, LABEL_PATH + " labels load success");
        } catch (IOException e) {
            labelList = new ArrayList<>();
            Log.d(TAG, LABEL_PATH + " labels load failed");
            e.printStackTrace();
        }
    }

    /**
     * @function 读取'LABEL_PATH‘路径的txt文件
     * @return String型List
     * @throws IOException 打开文件IO异常
     */
    private List<String> loadLabelList() throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    /**
     * 获取图片的四维数组
     * @param bitmap bitmap对象
     * @param dims 参数数组
     * @return 图片四维数组
     */
    public float[][][][] getScaledMatrix(Bitmap bitmap, int[] dims) {
        //新建一个1*224*224*3的四维数组
        float[][][][] inFloat = new float[dims[0]][dims[1]][dims[2]][dims[3]];
        //新建一个一维数组，长度是图片像素点的数量
        int[] pixels = new int[dims[1] * dims[2]];
        //把原图缩放成我们需要的图片大小
        Bitmap bm = Bitmap.createScaledBitmap(bitmap, dims[1], dims[2], false);
        //把图片的每个像素点的值放到我们前面新建的一维数组中
        bm.getPixels(pixels, 0, bm.getWidth(), 0, 0, dims[1], dims[2]);
        int pixel = 0;
        //for循环，把每个像素点的值转换成RBG的值，存放到我们的目标数组中
        for (int i = 0; i < dims[1]; ++i) {
            for (int j = 0; j < dims[2]; ++j) {
                final int val = pixels[pixel++];
                double red = ((val >> 16) & 0xFF)/255.0; //归一化
                double green = ((val >> 8) & 0xFF)/255.0;
                double blue = (val & 0xFF)/255.0;
                float[] arr = {(float) red, (float) green, (float) blue};
                inFloat[0][i][j] = arr;
            }
        }
        if (bm.isRecycled()) {
            bm.recycle();
        }
        return inFloat;
    }

    /**
     * @function 定义Intent跳转到特定图库的Uri下挑选，然后将挑选结果返回给Activity
     */
    private void openSysAlbum() {
        Intent albumIntent = new Intent(Intent.ACTION_PICK);//使用ACTION_PICK选择图片,ACTION_GET_CONTENT/ACTION_OPEN_DOCUMENT选择文件
        albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");//选择image类型
        selectPicLauncher.launch(albumIntent);
    }

    /**
     * @function 定义Intent跳转到相机进行拍照，然后将结果返回给Activity
     */
    private void openSysCamera(){

        File dir = new File(Environment.getExternalStorageDirectory(),"pictures"); //定义File目录，表示pictures目录
        boolean sign = true;
        if(!dir.exists()){//若目录不存在，则创建
            sign = dir.mkdirs();
        }
        if (sign) {
            File currentImageFile = new File(dir,System.currentTimeMillis() + ".jpg");//定义图片名称
            Uri imageUri = Uri.fromFile(currentImageFile);//获取文件的URI
            picPath = imageUri.getPath();//得到uri对应的path

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//设置intent跳转
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
            takePhotoLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(MainActivity.this,"保存图片目录创建失败",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @function ACTION_PICK 回调intent处理
     * @param data Intent类型变量
     */
    private void handleImageOnKitKat(Intent data) {
        Uri uri = data.getData();
        assert uri != null;
        if (DocumentsContract.isDocumentUri(this, uri)) {
            // 如果是document类型的Uri，则通过document id处理
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                // 解析出数字格式的id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                picPath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content: //downloads/public_downloads"), Long.parseLong(docId));
                picPath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // 如果是content类型的Uri，则使用普通方式处理
            picPath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            // 如果是file类型的Uri，直接获取图片路径即可
            picPath = uri.getPath();
        }
        // 根据图片路径显示图片
        displayImage(picPath);
        Log.i(TAG,"select pic's path:"+ picPath);
    }

    /**
     * @function 获取图片的路径
     * @param uri Uri类型变量
     * @param selection String类型变量
     * @return 图片path
     */
    @SuppressLint("Range")
    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if(cursor != null){
            if(cursor.moveToFirst()){
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    /**
     * @function 根据图片path展示图片
     * @param imagePath String型变量 图片的路径
     */
    private void displayImage(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        image_flower.setImageBitmap(bitmap);
    }
}