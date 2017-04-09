package com.parkjeongseop.cheongwonpay;

/**
 * Created by 정섭 on 2016-08-19.
 */
import android.graphics.drawable.Drawable;

public class ListViewItem {
    private String titleStr ;
    private String descStr ;
    private String itemCode;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public void setTitle(String title) {
        titleStr = title ;
    }
    public void setDesc(String desc) {
        descStr = desc ;
    }

    public String getTitle() {
        return this.titleStr ;
    }
    public String getDesc() {
        return this.descStr ;
    }
}
