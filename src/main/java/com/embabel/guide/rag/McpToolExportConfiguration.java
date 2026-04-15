package com.embabel.guide.rag;

import com.embabel.agent.filter.PropertyFilter;
import com.embabel.agent.mcpserver.McpToolExport;
import com.embabel.agent.rag.neo.drivine.DrivineStore;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.guide.GuideProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Export MCP tools
 */
@Configuration
class McpToolExportConfiguration {

    private static final ExecutorService MCP_TOOL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Bean
    McpToolExport documentationRagTools(
            DrivineStore drivineStore,
            GuideProperties properties
    ) {
        var toolishRag = new ToolishRag(
                "docs",
                "Embabel docs",
                drivineStore
        );
        var activeVersion = properties.getContent().getActiveVersion();
        if (activeVersion != null) {
            var versionFilter = new PropertyFilter.In(
                    "version",
                    List.of(activeVersion, VersionChunkTransformer.SUPPLEMENTARY)
            );
            toolishRag = toolishRag.withMetadataFilter(versionFilter);
        }
        return McpToolExport.fromLlmReference(
        return withTimeouts(McpToolExport.fromLlmReference(
                toolishRag,
                properties.toolNamingStrategy()
        ), properties.getMcpToolTimeout());
    }

    @Bean
    McpToolExport referenceTools(
            DataManager dataManager,
            GuideProperties properties) {
        return withTimeouts(McpToolExport.fromLlmReferences(
                dataManager.referencesForAllUsers(),
                properties.toolNamingStrategy()
        ), properties.getMcpToolTimeout());
    }

    private static McpToolExport withTimeouts(McpToolExport delegate, Duration timeout) {
        return new WrappedMcpToolExport(
                delegate,
                delegate.getToolCallbacks().stream()
                        .map(toolCallback -> (ToolCallback) new TimeoutToolCallback(toolCallback, timeout))
                        .toList()
        );
    }

    private record WrappedMcpToolExport(McpToolExport delegate, List<ToolCallback> toolCallbacks) implements McpToolExport {

        @Override
        public List<ToolCallback> getToolCallbacks() {
            return toolCallbacks;
        }

        @Override
        public String infoString(Boolean verbose, int indent) {
            return delegate.infoString(verbose, indent);
        }
    }

    private static final class TimeoutToolCallback implements ToolCallback {

        private static final Logger logger = LoggerFactory.getLogger(TimeoutToolCallback.class);

        private final ToolCallback delegate;
        private final Duration timeout;

        private TimeoutToolCallback(ToolCallback delegate, Duration timeout) {
            this.delegate = delegate;
            this.timeout = timeout;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String toolInput) {
            return execute(toolInput, null);
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return execute(toolInput, toolContext);
        }

        private String execute(String toolInput, ToolContext toolContext) {
            var toolName = getToolDefinition().name();
            Future<String> future = MCP_TOOL_EXECUTOR.submit(() -> toolContext == null
                    ? delegate.call(toolInput)
                    : delegate.call(toolInput, toolContext));
            try {
                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                logger.warn("MCP tool {} timed out after {} for input {}", toolName, timeout, abbreviate(toolInput));
                return failureJson(toolName, "timeout", "Tool execution exceeded timeout of " + timeout + ". Narrow the query and retry.");
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                logger.warn("MCP tool {} interrupted for input {}", toolName, abbreviate(toolInput));
                return failureJson(toolName, "interrupted", "Tool execution was interrupted.");
            } catch (ExecutionException e) {
                var cause = e.getCause() != null ? e.getCause() : e;
                logger.error("MCP tool {} failed for input {}", toolName, abbreviate(toolInput), cause);
                return failureJson(toolName, "error", cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName());
            }
        }

        private static String abbreviate(String value) {
            if (value == null) {
                return "<null>";
            }
            return value.length() <= 300 ? value : value.substring(0, 300) + "...";
        }

        private static String failureJson(String toolName, String status, String message) {
            return "{\"tool\":\"" + escape(toolName)
                    + "\",\"status\":\"" + escape(status)
                    + "\",\"message\":\"" + escape(message)
                    + "\"}";
        }

        private static String escape(String value) {
            return value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }
}
