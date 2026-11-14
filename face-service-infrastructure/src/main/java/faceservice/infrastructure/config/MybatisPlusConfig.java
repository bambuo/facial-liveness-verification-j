package faceservice.infrastructure.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import faceservice.domain.security.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：租户隔离由 TenantLineInnerInterceptor 从 JWT 上下文统一注入，
 * 从插件层杜绝遗漏 WHERE tenant_id；face_tenants 主键即租户本身，跳过。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new StringValue(TenantContext.require().tenantId());
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return "face_tenants".equalsIgnoreCase(tableName);
            }
        }));
        return interceptor;
    }
}
