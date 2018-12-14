package org.mz.ditran.dubbo.passive;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mz.ditran.common.DitranConstants;
import org.mz.ditran.common.Handler;
import org.mz.ditran.common.entity.NodeInfo;
import org.mz.ditran.common.entity.ResultHolder;
import org.mz.ditran.common.entity.ZkPath;
import org.mz.ditran.core.conf.DitranContainer;
import org.mz.ditran.core.conf.DitranPassiveContainer;
import org.mz.ditran.core.transaction.DitransactionManager;
import org.mz.ditran.core.transaction.DitransactionWrapper;
import org.mz.ditran.core.transaction.impl.PassiveDitransactionManager;
import org.mz.ditran.dubbo.DitranDubboFilter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

/**
 * 事务被动接受端
 * <p>
 * DUBBO服务提供者
 *
 * @Author: jsonz
 * @Date: 2018-12-13 16:51
 */
@Activate(group = Constants.PROVIDER)
@Slf4j
public class DitranPassiveDubboFilter extends DitranDubboFilter {


    @Override
    protected boolean isDitran() {
        return StringUtils.isNotBlank(RpcContext.getContext().getAttachment(DitranConstants.ACTIVE_PATH_KEY));
    }

    @Override
    public Result doInvoke(final Invoker<?> invoker, final Invocation invocation) throws UnknownHostException {
        final String activePathStr = RpcContext.getContext().getAttachment(DitranConstants.ACTIVE_PATH_KEY);
        final ZkPath activePath = new ZkPath(activePathStr);
        final DitranContainer container = DitranContainer.getConfig(DitranPassiveContainer.class);
        PlatformTransactionManager platformTransactionManager = container.getTransactionManager();
        final DitransactionManager manager = new PassiveDitransactionManager(platformTransactionManager, container.getZkClient(), activePath);
        final ResultHolder<Result> holder = new ResultHolder<>();

        // 构建Passive节点信息.
        final NodeInfo nodeInfo = NodeInfo.builder()
                .className(invoker.getInterface().getSimpleName())
                .host(InetAddress.getLocalHost().getHostAddress())
                .methodName(invocation.getMethodName())
                .paramTypes(getParamTypes(invocation))
                .status(DitranConstants.ZK_NODE_START_VALUE).build();


        Executors.newFixedThreadPool(1).execute(new Runnable() {
            @Override
            public void run() {
                try {
                    DitransactionWrapper.wrap(new Handler<Invocation, Result>() {
                        @Override
                        public Result handle(Invocation invocation) throws Throwable {
                            Result result = invoker.invoke(invocation);
                            holder.setT(result);
                            holder.setEmpty(false);
                            return result;
                        }
                    }).with(manager).start(nodeInfo, Propagation.REQUIRED, invocation);
                } catch (Throwable e) {
                    // 抛出异常，使外界能感知到
                    log.error(e.getMessage(), e);
                    holder.setEmpty(false);
                    holder.setE(e);
                }
            }
        });
        while (holder.isEmpty()) ;
        if (holder.getE() != null) {
            throw new RpcException(holder.getE());
        }
        return holder.getT();
    }


    /**
     *
     *
     *
     * @param invocation
     * @return
     */
    private String[] getParamTypes(Invocation invocation) {
        Object[] args = invocation.getArguments();
        String[] params = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            params[i] = args[i].getClass().getSimpleName();
        }

        return params;
    }

}
