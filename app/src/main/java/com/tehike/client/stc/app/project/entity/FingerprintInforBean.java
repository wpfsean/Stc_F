package com.tehike.client.stc.app.project.entity;

import java.io.Serializable;

/**
 * 描述：$desc$
 * ===============================
 *
 * @author $user$ wpfsean@126.com
 * @version V1.0
 * @Create at:$date$ $time$
 */

public class FingerprintInforBean implements Serializable {

    private String authority;
    private String duty;
    private String guid;
    private String name;
    private String number;
    private String photo;
    private String political;
    private String rank;


    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getDuty() {
        return duty;
    }

    public void setDuty(String duty) {
        this.duty = duty;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPolitical() {
        return political;
    }

    public void setPolitical(String political) {
        this.political = political;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public FingerprintInforBean(String authority, String duty, String guid, String name, String number, String photo, String political, String rank) {
        this.authority = authority;
        this.duty = duty;
        this.guid = guid;
        this.name = name;
        this.number = number;
        this.photo = photo;
        this.political = political;
        this.rank = rank;
    }

    @Override
    public String toString() {
        return "FingerprintInforBean{" +
                "authority='" + authority + '\'' +
                ", duty='" + duty + '\'' +
                ", guid='" + guid + '\'' +
                ", name='" + name + '\'' +
                ", number='" + number + '\'' +
                ", photo='" + photo + '\'' +
                ", political='" + political + '\'' +
                ", rank='" + rank + '\'' +
                '}';
    }

    public FingerprintInforBean() {
    }
}
