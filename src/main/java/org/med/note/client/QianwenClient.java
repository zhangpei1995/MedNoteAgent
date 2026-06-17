package org.med.note.client;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.protocol.Protocol;
import org.med.note.config.DotenvConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 阿里云百炼-千问通用客户端
 * 支持动态系统提示、用户提问、模型切换、统一异常捕获
 */
@Component
public class QianwenClient {

    // ==================== 全局配置项（统一维护，方便修改） ====================
    /**
     * 接口地址
     */
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1";
    /**
     * 默认模型
     */
    private static final String DEFAULT_MODEL = "qwen-max";
    // 初始化客户端实例（单例复用，避免重复创建连接）
    private static final Generation GENERATION_CLIENT;

    static {
        GENERATION_CLIENT = new Generation(Protocol.HTTP.getValue(), API_URL);
    }

    private final String apiKey;
    private final String model;

    public QianwenClient(
            @Value("${mednote.llm.dashscope.model:qwen-max}") String model
    ) {
        this.apiKey = DotenvConfig.getQwenApiKey();
        this.model = model;
    }


}
