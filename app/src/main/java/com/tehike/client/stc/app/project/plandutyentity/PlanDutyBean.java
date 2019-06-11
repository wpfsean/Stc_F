package com.tehike.client.stc.app.project.plandutyentity;

import com.tehike.client.stc.app.project.ui.fragments.ManagmentServiceFragment;

import java.io.Serializable;
import java.util.List;

/**
 * 描述：$desc$
 * ===============================
 *
 * @author $user$ wpfsean@126.com
 * @version V1.0
 * @Create at:$date$ $time$
 */

public class PlanDutyBean implements Serializable {

    int day;
    String frmtime;
    String guardpost;
    int id;
    List<PersionBean> mlist;

    public List<PersionBean> getMlist() {
        return mlist;
    }

    public void setMlist(List<PersionBean> mlist) {
        this.mlist = mlist;
    }

    String persionid;
    int sccnt;
    String schdate;
    int schrank;
    String totime;

    public PlanDutyBean() {
    }


    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getFrmtime() {
        return frmtime;
    }

    public void setFrmtime(String frmtime) {
        this.frmtime = frmtime;
    }

    public String getGuardpost() {
        return guardpost;
    }

    public void setGuardpost(String guardpost) {
        this.guardpost = guardpost;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPersionid() {
        return persionid;
    }

    public void setPersionid(String persionid) {
        this.persionid = persionid;
    }

    public int getSccnt() {
        return sccnt;
    }

    public void setSccnt(int sccnt) {
        this.sccnt = sccnt;
    }

    public String getSchdate() {
        return schdate;
    }

    public void setSchdate(String schdate) {
        this.schdate = schdate;
    }

    public int getSchrank() {
        return schrank;
    }

    public void setSchrank(int schrank) {
        this.schrank = schrank;
    }

    public String getTotime() {
        return totime;
    }

    public void setTotime(String totime) {
        this.totime = totime;
    }

    @Override
    public String toString() {
        return "PlanDutyBean{" +
                "day=" + day +
                ", frmtime='" + frmtime + '\'' +
                ", guardpost='" + guardpost + '\'' +
                ", id=" + id +
                ", persionid='" + persionid + '\'' +
                ", sccnt=" + sccnt +
                ", schdate='" + schdate + '\'' +
                ", schrank=" + schrank +
                ", totime='" + totime + '\'' +
                '}';
    }
}
