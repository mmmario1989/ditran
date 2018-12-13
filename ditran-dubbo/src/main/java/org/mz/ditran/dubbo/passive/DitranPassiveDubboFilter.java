package org.mz.ditran.dubbo.passive;

import com.alibaba.dubbo.rpc.RpcContext;
import org.apache.commons.lang3.StringUtils;
import org.mz.ditran.dubbo.DitranDubboFilter;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;

/**
 *
 * 事务被动接受端
 *
 * DUBBO服务提供者
 *
 * @Author: jsonz
 * @Date: 2018-12-13 16:51
 */
@Activate(group = Constants.PROVIDER)
public class DitranPassiveDubboFilter extends DitranDubboFilter {


    @Override
    protected boolean isDitran() {
        return StringUtils.isNotBlank(RpcContext.getContext().getAttachment(org.mz.ditran.common.Constants.DUBBO_ATTACHMENTS_KEY));
    }

    @Override
    public Result doInvoke(Invoker<?> invoker, Invocation invocation) {
        return null;
    }

}
