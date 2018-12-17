package org.mz.ditran.core.conf;

import org.mz.ditran.core.DitranAspect;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @Author: mario
 * @Email: mmmario@foxmail.com
 * @Date: 2018-12-13 6:23 PM
 * @Description:
 */
public class DitranActiveContainer extends DitranContainer {

    private DitranAspect ditranAspect;


    public DitranActiveContainer(DitranZKConfig zkConfig, PlatformTransactionManager transactionManager,DitranAspect ditranAspect) {
        super(zkConfig, transactionManager);
        this.ditranAspect = ditranAspect;
    }

    @Override
    protected void check() {
        super.check();
        if(ditranAspect==null){
            throw new IllegalArgumentException("ditranAspect can not be null");
        }
    }


}
