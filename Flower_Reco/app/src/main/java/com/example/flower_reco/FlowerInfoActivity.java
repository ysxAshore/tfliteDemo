package com.example.flower_reco;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FlowerInfoActivity extends AppCompatActivity {

    public static final String TAG = "Flower_Recognize.FlowerInfoActivity"; //Log打印用的LAG
    private List<String> infoList; //花朵详细信息info List

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flower_info);
        initViewAndVariable();
    }

    /**
     * @function 初始化控件和变量
     */
    @SuppressLint("SetTextI18n")
    private void initViewAndVariable(){
        //展示花朵图片的imageView控件
        ImageView picImage = findViewById(R.id.flower_info);
        //展示花朵详细信息的textView控件
        TextView picTextInfo = findViewById(R.id.text_info);

        loadInfoList();

        Intent intent = getIntent();
        String picPath = intent.getStringExtra("picPath");//intent传递的图片路径
        int index = intent.getIntExtra("index", 0);//intent传递的对应List索引
        Log.i(TAG,"The parameters are "+ picPath + "," + index);

        Bitmap bitmap = BitmapFactory.decodeFile(picPath);
        picImage.setImageBitmap(bitmap);
        picTextInfo.setText(infoList.get(index));

    }

    /**
     * @function 加载花朵信息List
     */
    private void loadInfoList(){
        infoList = new ArrayList<>();
        infoList.add("荷花是一种水生植物，以其优雅的花形和淡雅的香气而著名。它的花瓣呈圆盘状，通常为粉红色或白色，被誉为“水中芙蓉”。荷花的茎非常高大，叶子呈现出独特的翠绿色。荷花的生长需要充足的水分和阳光，因此在中国南方广泛分布。荷花在夏季盛开，其美丽和优雅的姿态吸引了众多游客前来观赏。荷花不仅具有观赏价值，还有许多实用价值，例如荷花可以入药、食用，莲藕也是人们喜爱的食材之一。荷花的寓意是高洁、清廉和优雅，它也象征着友谊和爱情的纯洁与坚定");
        infoList.add("菊花是中国传统的名花之一，以其傲霜斗寒的品质而著称。它的花瓣细长，层次分明，有红、黄、白等多种颜色。菊花的花期在秋季，此时其他花卉已经凋谢，而菊花却依然傲然开放，给人以坚强和独立的印象。菊花不仅可以用于观赏，还有许多实用价值，例如可以入药、食用、制作菊花茶等。菊花的寓意是坚韧不拔、高洁清雅的品质，也是中国的国花之一。");
        infoList.add("玫瑰被誉为“爱情之花”，其芬芳、娇艳、浪漫，代表了热烈的爱情与美好的祝福。不同颜色的玫瑰也有不同的寓意，例如红玫瑰代表热烈的爱情，粉玫瑰代表温馨的爱，白玫瑰则代表纯洁的爱。玫瑰花语通常与爱情、美丽、祝福等有关，是人们表达爱意和敬意的常用花卉之一。");
        infoList.add("牡丹花是中国传统的名花之一，被誉为“花中之王”。其花瓣丰满、华丽富贵，有红、黄、白、粉等多种颜色，国色天香，是繁荣昌盛的象征。牡丹花的花期在春季，盛开时花瓣层层叠叠，十分美丽。牡丹花在中国被视为富贵、吉祥和幸福的花卉，也寓意着美丽和高贵的品质。");
        infoList.add("蒲公英是一种非常轻盈的植物，其种子被风吹散，寓意着自由与梦想。蒲公英的花朵小巧可爱，通常是黄色或白色，给人一种清新自然的感觉。蒲公英在野外生长非常普遍，其种子飘散的样子非常美丽。蒲公英不仅可以用于观赏，还有许多实用价值，例如可以入药、食用等。");
        infoList.add("牵牛花是一种缠绕植物，寓意着坚韧不拔和勇往直前的精神。它的花朵形状呈漏斗状，通常为蓝色或紫色，非常美丽。牵牛花的花期在夏季，其花朵在早晨开放，晚上闭合。牵牛花的生命力非常顽强，可以在各种环境中生长。");
        infoList.add("水仙花是一种非常清雅的花卉，其花朵形状呈高脚杯状，有白色、黄色和粉色等颜色。水仙花是中国传统的名花之一，被誉为“凌波仙子”，是吉祥与美好的化身。水仙花的花期在春季和冬季，其花香浓郁，花瓣细腻柔嫩。水仙花通常被视为高雅、清新的象征。");
        infoList.add("向日葵是一种阳光明媚的花卉，其花朵形状呈圆盘状，通常为金黄色或橙色。向日葵的花语是追求光明和幸福，也代表着希望和友谊。向日葵的生长需要充足的阳光和温暖的气候条件。");
        infoList.add("樱花是日本的国花，也是世界著名的花卉之一。其花朵小巧玲珑，通常为粉色或白色，非常美丽。樱花的盛开时间很短，只有几天的时间，但是它给人们带来了春天的气息和美好的希望。樱花的美丽和优雅的姿态吸引了众多游客前来观赏。");
        infoList.add("郁金香是一种华贵庄重的花卉，其花朵形状呈杯状或碗状，有红、黄、白、粉等多种颜色。郁金香的寓意是永恒的爱情与幸福，也是荷兰的国花之一。郁金香的花期在春季和初夏之间");
    }
}