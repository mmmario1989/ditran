package org.mz.ditran.core.transaction.impl;

import org.mz.ditran.common.DitranConstants;
import org.mz.ditran.common.Handler;
import org.mz.ditran.common.entity.DitranInfo;
import org.mz.ditran.common.entity.NodeInfo;
import org.mz.ditran.common.entity.ZkPath;
import org.mz.ditran.core.BlockingHandler;
import org.mz.ditran.core.blocking.Condition;
import org.mz.ditran.core.transaction.DitransactionManagerAdapter;
import org.mz.ditran.core.zk.DitranZKClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;

/**
 * @Author: mario
 * @Email: mmmario@foxmail.com
 * @Date: 2018-12-14 12:46 PM
 * @Description:
 */
public class PassiveDitransactionManager extends DitransactionManagerAdapter {

    private ZkPath activePath;

    private long timeout;


    public PassiveDitransactionManager(PlatformTransactionManager transactionManager, DitranZKClient zkClient, ZkPath activePath, long timeout) {
        super(transactionManager, zkClient);
        this.activePath = activePath;
        this.timeout = timeout;
    }

    @Override
    public void begin(NodeInfo nodeInfo, Propagation propagation) throws Exception {
        ZkPath zkPath = new ZkPath(activePath.getFullPath());
        zkPath.setNode(DitranConstants.PASSIVE_NODE);
        TransactionStatus transactionStatus = beginLocal(propagation);
        ditranInfo = DitranInfo.builder()
                .transactionStatus(transactionStatus)
                .zkPath(zkPath)
                .nodeInfo(nodeInfo)
                .build();
    }

    @Override
    public NodeInfo listen() throws Exception {
        // passive端需要阻塞，直到active端写zk成功。如果超时直抛出异常，进行回滚.
        return new BlockingHandler<String, NodeInfo>(activePath.getFullPath()).blocking(new Handler<String, NodeInfo>() {
            @Override
            public NodeInfo handle(String key) throws Throwable {
                return zkClient.getNodeInfo(key);
            }
        }, new Condition<NodeInfo>() {
            @Override
            public boolean onCondition(NodeInfo info) {
                return DitranConstants.ZK_NODE_SUCCESS_VALUE.equals(info.getStatus());
            }
        }, timeout);
    }


    @Override
    public void commit() throws Exception {
        transactionManager.commit(ditranInfo.getTransactionStatus());
    }
}
