package com.tehike.client.stc.app.project.plandutyentity;

import java.io.Serializable;

/**
 * 描述：$desc$
 * ===============================
 *
 * @author $user$ wpfsean@126.com
 * @version V1.0
 * @Create at:$date$ $time$
 */

public class PersionBean implements Serializable {

    String persionid;
    String persionname;

    public PersionBean(String persionid, String persionname) {
        this.persionid = persionid;
        this.persionname = persionname;
    }

    public String getPersionid() {
        return persionid;
    }

    public void setPersionid(String persionid) {
        this.persionid = persionid;
    }

    public String getPersionname() {
        return persionname;
    }

    public void setPersionname(String persionname) {
        this.persionname = persionname;
    }
}
